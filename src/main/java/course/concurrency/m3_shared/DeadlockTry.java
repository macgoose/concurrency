package course.concurrency.m3_shared;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeadlockTry {
    public static void main(String[] args) {
        Map<Integer, Integer> map = new ConcurrentHashMap<>();

        map.put(1, 2);
        map.put(2, 1);

        Runnable deadlyRun1 = () -> map.compute(
            1, (k1,v1) -> map.compute(v1, (k2, v2) -> v2)
        );
        Runnable deadlyRun2 = () -> map.compute(
            2, (k1,v1) -> map.compute(v1, (k2, v2) -> v2)
        );

        new Thread(deadlyRun1).start();
        new Thread(deadlyRun2).start();

        System.out.println("Done");
    }
}
