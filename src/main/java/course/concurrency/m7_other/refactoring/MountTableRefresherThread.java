package course.concurrency.m7_other.refactoring;

import static course.concurrency.m7_other.refactoring.Others.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class MountTableRefresherThread implements Callable<Boolean> {

    private boolean success;
    private final String adminAddress;
    private final MountTableManager manager;

    public MountTableRefresherThread(MountTableManager manager, String adminAddress) {
        this.manager = manager;
        this.adminAddress = adminAddress;
    }

    @Override
    public Boolean call() {
        return success = manager.refresh();
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "MountTableRefreshThread [success=" + success + ", adminAddress=" + adminAddress + "]";
    }

    public String getAdminAddress() {
        return adminAddress;
    }
}
