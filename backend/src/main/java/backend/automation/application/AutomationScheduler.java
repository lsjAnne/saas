package backend.automation.application;

import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import backend.tenant.context.TenantContext;
import backend.tenant.context.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动化定时拉单调度器。默认关闭（app.automation.scheduler.enabled=false），
 * 开启后对“被手动触发过的店铺”做周期性 mock 拉单 + 规则执行，避免测试抖动。
 */
@Component
@ConditionalOnProperty(name = "app.automation.scheduler.enabled", havingValue = "true")
public class AutomationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutomationScheduler.class);

    private final StoreRepository storeRepository;
    private final AutomationTriggerService triggerService;
    private final Map<String, String> registeredStores = new ConcurrentHashMap<>();

    public AutomationScheduler(StoreRepository storeRepository,
                               AutomationTriggerService triggerService) {
        this.storeRepository = storeRepository;
        this.triggerService = triggerService;
    }

    /** 手动触发过的店铺登记为定时拉单候选。 */
    public void registerStore(String tenantId, String storeId) {
        registeredStores.put(tenantId + "|" + storeId, tenantId);
    }

    @Scheduled(fixedDelayString = "${app.automation.scheduler.fixed-delay-ms:1800000}")
    public void scheduledOrderSync() {
        if (registeredStores.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : List.copyOf(registeredStores.entrySet())) {
            String[] parts = entry.getKey().split("\\|", 2);
            String tenantId = parts[0];
            String storeId = parts[1];
            Store store = storeRepository.findByStoreId(storeId).orElse(null);
            if (store == null) {
                continue;
            }
            TenantContext previous = TenantContextHolder.get();
            try {
                TenantContextHolder.set(new TenantContext(tenantId, "automation-scheduler", "system"));
                triggerService.scheduledOrderSync(tenantId, store);
            } catch (Exception exception) {
                log.warn("automation scheduled order sync failed: storeId={}, reason={}", storeId, exception.getMessage());
            } finally {
                if (previous == null) {
                    TenantContextHolder.clear();
                } else {
                    TenantContextHolder.set(previous);
                }
            }
        }
    }
}
