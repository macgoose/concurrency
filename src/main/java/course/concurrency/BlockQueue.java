package course.concurrency;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockQueue<T> extends CustomQueue<T> {

    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public BlockQueue(int capacity) {
        super(capacity);
        this.capacity = capacity;
    }

    public int size() {
        lock.lock();

        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    public void add(T value) throws InterruptedException {
        lock.lock();

        try {
            while (queue.size() == capacity)
                notFull.await();

            queue.add(value);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T pool() throws InterruptedException {
        lock.lock();

        try {
            while (queue.isEmpty())
                notEmpty.await();

            notFull.signal();
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

}
