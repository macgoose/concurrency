package course.concurrency;

import java.util.ArrayDeque;
import java.util.Queue;

public class CustomQueue<T> {

    protected final Queue<T> queue;

    public CustomQueue(int capacity) {
        this.queue = new ArrayDeque<>(capacity);
    }

    public int size() {
        return queue.size();
    }

    public void add(T value) throws InterruptedException {
        queue.add(value);
    }

    public T pool() throws InterruptedException {
        return queue.poll();
    }

}
