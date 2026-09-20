    package course.concurrency.m3_shared.testing;

    import org.junit.jupiter.api.RepeatedTest;

    import java.util.concurrent.*;

    import static org.junit.jupiter.api.Assertions.assertEquals;

    public class TestExperiments {

        // Don't change this class
        public static class Counter {
            private volatile int counter = 0;

            public void increment() {
                counter++;
            }

            public int get() {
                return counter;
            }
        }

        ExecutorService pool = Executors.newFixedThreadPool(20);

        @RepeatedTest(100)
        public void counterShouldFail() {
            int iterations = 20;

            Counter counter = new Counter();
            CountDownLatch latch = new CountDownLatch(iterations);

            for (int i = 0; i < iterations; i++) {
                pool.execute(() -> {
                    try {
                        latch.await();
                        counter.increment();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });

                latch.countDown();
            }

            pool.shutdown();

            assertEquals(iterations, counter.get());
        }
    }
