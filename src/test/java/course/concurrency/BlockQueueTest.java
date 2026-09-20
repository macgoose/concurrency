package course.concurrency;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class BlockQueueTest {

    ExecutorService executor = Executors.newFixedThreadPool(4);

    <T> CustomQueue<T> createQueue(int capacity) {
        return new BlockQueue<>(capacity);
    }

    @Test
    void shouldBlockEnqueue() throws Exception {
        CustomQueue<Integer> queue = createQueue(1);

        queue.add(1);

        Future<?> future = executor.submit(() -> {
            queue.add(2);
            return null;
        });

        assertThrows(
            TimeoutException.class,
            () -> future.get(100, TimeUnit.MILLISECONDS)
        );

        assertEquals(1, queue.pool());

        future.get(1, TimeUnit.SECONDS);

        assertEquals(2, queue.pool());
        assertEquals(0, queue.size());
    }

    @Test
    void shouldBlockDequeue() throws InterruptedException, ExecutionException, TimeoutException {
        CustomQueue<Integer> queue = createQueue(1);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Integer> future = executor.submit(() -> {
            startLatch.countDown();
            return queue.pool();
        });

        assertFalse(future.isDone());
        queue.add(1);
        assertEquals(1, future.get(1, TimeUnit.SECONDS));
        assertEquals(0, queue.size());
    }

    @Test
    void shouldEnqueueAndDequeue() throws InterruptedException {
        CustomQueue<Integer> queue = createQueue(3);

        for (int i = 0; i < 3; i++) {
            queue.add(i);
        }

        for (int i = 0; i < 3; i++) {
            assertEquals(i, queue.pool());
        }

        assertEquals(0, queue.size());
    }

    @Test
    void shouldHandleConcurrentEnqueueAndDequeue() throws Exception {
        int producers = 2;
        int consumers = 2;
        int elementsPerProducer = 10_000;
        int totalElements = producers * elementsPerProducer;
        int elementsPerConsumer = totalElements / consumers;

        CustomQueue<Integer> queue = createQueue(4);

        CountDownLatch readyLatch = new CountDownLatch(producers + consumers);
        CountDownLatch startLatch = new CountDownLatch(1);

        Set<Integer> consumed = ConcurrentHashMap.newKeySet();

        List<Future<?>> futures = new ArrayList<>();

        for (int producer = 0; producer < producers; producer++) {
            int producerId = producer;

            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                int from = producerId * elementsPerProducer;
                int to = from + elementsPerProducer;

                for (int value = from; value < to; value++) {
                    queue.add(value);
                }

                return null;
            }));
        }

        for (int consumer = 0; consumer < consumers; consumer++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                for (int i = 0; i < elementsPerConsumer; i++) {
                    Integer value = queue.pool();

                    assertTrue(
                        consumed.add(value),
                        "Value was consumed more than once: " + value
                    );
                }

                return null;
            }));
        }

        assertTrue(readyLatch.await(1, TimeUnit.SECONDS));

        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }

        assertEquals(totalElements, consumed.size());
        assertEquals(0, queue.size());

        for (int value = 0; value < totalElements; value++) {
            assertTrue(consumed.contains(value), "Missing value: " + value);
        }
    }
}
