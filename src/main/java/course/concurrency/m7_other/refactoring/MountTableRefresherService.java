package course.concurrency.m7_other.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class MountTableRefresherService {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<>();
        routerStore.getCachedRecords().stream()
            .map(Others.RouterState::getAdminAddress)
            .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        routerClientsCache.cleanUp();
        executor.shutdown();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
            Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
            () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
            routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        List<Others.RouterState> cachedRecords = routerStore.getCachedRecords();
        List<MountTableRefresherThread> refreshThreads = new ArrayList<>();

        for (Others.RouterState routerState : cachedRecords) {
            String adminAddress = routerState.getAdminAddress();
            if (adminAddress == null || adminAddress.isEmpty())
                continue;

            if (isLocalAdmin(adminAddress)) {
                refreshThreads.add(getLocalRefresher(adminAddress));
            } else {
                refreshThreads.add(new MountTableRefresherThread(createManager(adminAddress), adminAddress));
            }
        }

        if (!refreshThreads.isEmpty())
            invokeRefresh(refreshThreads);
    }

    private void invokeRefresh(List<MountTableRefresherThread> refreshThreads) {
        try {
            List<Future<Boolean>> results = executor.invokeAll(
                refreshThreads,
                cacheUpdateTimeout,
                TimeUnit.MILLISECONDS
            );

            boolean allReqCompleted = results.stream().noneMatch(Future::isCancelled);
            if (!allReqCompleted)
                log("Not all router admins updated their cache");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("Mount table cache refresher was interrupted.");
        }

        logResult(refreshThreads);
    }

    protected MountTableRefresherThread getLocalRefresher(String adminAddress) {
        return new MountTableRefresherThread(createManager("local"), adminAddress);
    }

    protected Others.MountTableManager createManager(String address) {
        return new Others.MountTableManager(address);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<MountTableRefresherThread> refreshThreads) {
        int successCount = 0;
        int failureCount = 0;

        for (MountTableRefresherThread mountTableRefreshThread : refreshThreads) {
            if (mountTableRefreshThread.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                removeFromCache(mountTableRefreshThread.getAdminAddress());
            }
        }

        log(String.format(
            "Mount table entries cache refresh successCount=%d,failureCount=%d",
            successCount, failureCount
        ));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache<String, Others.RouterClient> cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}