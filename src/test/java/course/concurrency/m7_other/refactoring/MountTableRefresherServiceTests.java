package course.concurrency.m7_other.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;
    private final List<String> addresses = List.of("123", "local6", "789", "local");

    @BeforeEach
    public void setUpStreams() {
        manager = mock(Others.MountTableManager.class);
        routerStore = mock(Others.RouterStore.class);
        routerClientsCache = mock(Others.LoadingCache.class);

        service = spy(new MountTableRefresherService());
        service.setRouterStore(routerStore);
        service.setRouterClientsCache(routerClientsCache);
        service.setCacheUpdateTimeout(1000);

        doReturn(manager)
            .when(service)
            .createManager(anyString());
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).collect(toList());

        when(routerStore.getCachedRecords()).thenReturn(states);
        when(manager.refresh()).thenReturn(true);

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).collect(toList());

        when(routerStore.getCachedRecords()).thenReturn(states);
        when(manager.refresh()).thenReturn(false);

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        List<Others.RouterState> states = addresses.stream()
            .map(Others.RouterState::new)
            .collect(toList());

        when(routerStore.getCachedRecords()).thenReturn(states);
        when(manager.refresh()).thenReturn(true, false, true, false);

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        AtomicInteger counter = new AtomicInteger();
        List<Others.RouterState> states = addresses.stream()
            .map(Others.RouterState::new)
            .collect(toList());

        when(routerStore.getCachedRecords()).thenReturn(states);
        when(manager.refresh()).thenAnswer(invocation -> {
            if (counter.incrementAndGet() == 1)
                throw new RuntimeException("Refresh failed");
            return true;
        });

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=3,failureCount=1");
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        AtomicInteger counter = new AtomicInteger();
        List<Others.RouterState> states = addresses.stream()
            .map(Others.RouterState::new)
            .collect(toList());

        service.setCacheUpdateTimeout(500);

        when(routerStore.getCachedRecords()).thenReturn(states);
        when(manager.refresh()).thenAnswer(invocation -> {
            if (counter.incrementAndGet() == 1) {
                Thread.sleep(2000);
            }
            return true;
        });

        service.refresh();

        verify(service).log("Not all router admins updated their cache");
        verify(service).log("Mount table entries cache refresh successCount=3,failureCount=1");
    }

}
