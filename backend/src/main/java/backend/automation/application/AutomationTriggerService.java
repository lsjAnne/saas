package backend.automation.application;

import backend.automation.application.AutomationExecutionService.EvaluationSummary;
import backend.channel.application.ChannelSyncService;
import backend.channel.dto.ChannelSyncResult;
import backend.common.exception.BusinessException;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.ChannelAccount;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单自动闭环触发入口：mock 渠道拉单（ChannelSyncService）→ 订单落库（OrderApplicationService.syncOrder）
 * → 自动风控（auditOrder）→ 规则引擎执行动作。
 */
@Service
public class AutomationTriggerService {

    private final StoreRepository storeRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelSyncService channelSyncService;
    private final AutomationExecutionService executionService;

    public AutomationTriggerService(StoreRepository storeRepository,
                                    ChannelAccountRepository channelAccountRepository,
                                    ChannelSyncService channelSyncService,
                                    AutomationExecutionService executionService) {
        this.storeRepository = storeRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.channelSyncService = channelSyncService;
        this.executionService = executionService;
    }

    public OrderSyncTriggerView triggerOrderSync(String tenantId, String storeId) {
        Store store = requireOwnedStore(tenantId, storeId);
        ChannelAccount account = resolveAuthorizedAccount(tenantId, store);
        ChannelSyncResult syncResult = channelSyncService.syncOrders(tenantId, account.channelAccountId(), storeId);
        EvaluationSummary summary = executionService.evaluateOrders(
                tenantId, store, "MANUAL", account.channelAccountId());
        return new OrderSyncTriggerView(
                "MANUAL",
                syncResult.totalCount(),
                summary.matchedRuleCount(),
                summary.executionCount(),
                summary.createdExecutionIds());
    }

    /** 供调度器调用：以系统身份对单个店铺执行一次定时拉单。 */
    public OrderSyncTriggerView scheduledOrderSync(String tenantId, Store store) {
        ChannelAccount account = resolveAuthorizedAccount(tenantId, store);
        channelSyncService.syncOrders(tenantId, account.channelAccountId(), store.storeId());
        EvaluationSummary summary = executionService.evaluateOrders(
                tenantId, store, "SCHEDULE", account.channelAccountId());
        return new OrderSyncTriggerView(
                "SCHEDULE", summary.matchedRuleCount(), summary.executionCount(),
                summary.executionCount(), summary.createdExecutionIds());
    }

    private ChannelAccount resolveAuthorizedAccount(String tenantId, Store store) {
        List<ChannelAccount> accounts = channelAccountRepository.findByOrganizationIds(List.of(store.organizationId()));
        ChannelAccount preferred = accounts.stream()
                .filter(account -> store.platformType() != null && store.platformType().equals(account.channelType()))
                .findFirst()
                .orElse(null);
        ChannelAccount account = preferred != null ? preferred : accounts.stream().findFirst().orElse(null);
        if (account == null) {
            throw new BusinessException("8603", "no authorized channel account for store: " + store.storeId(), HttpStatus.BAD_REQUEST);
        }
        return account;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return store;
    }

    public record OrderSyncTriggerView(
            String triggerSource,
            int pulledOrderCount,
            int matchedRuleCount,
            int executionCount,
            List<String> createdExecutionIds
    ) {
    }
}
