package backend.automation.application;

import backend.automation.application.AutomationTriggerService.OrderSyncTriggerView;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import backend.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomationSchedulerTest {

    private StoreRepository storeRepository;
    private AutomationTriggerService triggerService;
    private AutomationScheduler scheduler;

    private Store store;

    @BeforeEach
    void setUp() {
        storeRepository = mock(StoreRepository.class);
        triggerService = mock(AutomationTriggerService.class);
        scheduler = new AutomationScheduler(storeRepository, triggerService);
        store = new Store("store-1", "tenant-1", "org-1", "owner", "pdd", "shop-1",
                "name", "authorized", BigDecimal.ZERO, BigDecimal.ZERO, null, OffsetDateTime.now());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldDoNothingWhenNoStoreRegistered() {
        scheduler.scheduledOrderSync();
        verify(triggerService, never()).scheduledOrderSync(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSweepRegisteredStore() {
        when(storeRepository.findByStoreId("store-1")).thenReturn(Optional.of(store));
        when(triggerService.scheduledOrderSync("tenant-1", store))
                .thenReturn(new OrderSyncTriggerView("SCHEDULE", 2, 1, 1, java.util.List.of("exec-1")));

        scheduler.registerStore("tenant-1", "store-1");
        scheduler.scheduledOrderSync();

        verify(triggerService, times(1)).scheduledOrderSync("tenant-1", store);
    }

    @Test
    void shouldSkipMissingStoreAndContinue() {
        when(storeRepository.findByStoreId("store-1")).thenReturn(Optional.empty());
        scheduler.registerStore("tenant-1", "store-1");
        scheduler.scheduledOrderSync();
        verify(triggerService, never()).scheduledOrderSync(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSwallowTriggerException() {
        when(storeRepository.findByStoreId("store-1")).thenReturn(Optional.of(store));
        when(triggerService.scheduledOrderSync("tenant-1", store))
                .thenThrow(new IllegalStateException("boom"));
        scheduler.registerStore("tenant-1", "store-1");
        scheduler.scheduledOrderSync();
        verify(triggerService, times(1)).scheduledOrderSync("tenant-1", store);
    }
}
