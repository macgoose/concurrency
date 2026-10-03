package course.concurrency.m6_streams;

import java.util.concurrent.*;

public class ThreadPoolTask {

    public static class LifoBlockingQueue<E> extends LinkedBlockingDeque<E> {
        @Override
        public boolean offer(E e) {
            return offerFirst(e);
        }
    }

    // Task #1
    public ThreadPoolExecutor getLifoExecutor() {
        return new ThreadPoolExecutor(
            1,
            1,
            0L, TimeUnit.SECONDS,
            new LifoBlockingQueue<>()
        );
    }

    // Task #2
    public ThreadPoolExecutor getRejectExecutor() {
        return new ThreadPoolExecutor(
            8,
            8,
            0L,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}
