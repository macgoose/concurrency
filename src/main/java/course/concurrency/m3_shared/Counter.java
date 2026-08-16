package course.concurrency.m3_shared;

public class Counter {

    private static final Object LOCK = new Object();
    private static int currPhase = 1;

    public static void syncPrint(int phase) {
        synchronized (LOCK) {
            while (currPhase != phase) {
                try {
                    LOCK.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            System.out.println(phase);
            currPhase++;
            LOCK.notifyAll();
        }
    }

    public static void first() {
        syncPrint(1);
    }

    public static void second() {
        syncPrint(2);
    }

    public static void third() {
        syncPrint(3);
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> first());
        Thread t2 = new Thread(() -> second());
        Thread t3 = new Thread(() -> third());
        t1.start();
        t2.start();
        t3.start();
    }
}
