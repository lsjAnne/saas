package backend.automation.application;

import backend.automation.domain.repository.AutomationApprovalRepository;
import backend.automation.domain.repository.AutomationExecutionRepository;
import backend.automation.model.AutomationApproval;
import backend.automation.model.AutomationExecution;
import backend.common.exception.BusinessException;
import backend.rule.domain.repository.AutomationRuleRepository;
import backend.rule.model.AutomationRule;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动化执行记录 / 人工确认队列 / 总览统计查询。
 */
@Service
public class AutomationQueryService {

    private final AutomationExecutionRepository executionRepository;
    private final AutomationApprovalRepository approvalRepository;
    private final AutomationRuleRepository ruleRepository;
    private final StoreRepository storeRepository;

    public AutomationQueryService(AutomationExecutionRepository executionRepository,
                                  AutomationApprovalRepository approvalRepository,
                                  AutomationRuleRepository ruleRepository,
                                  StoreRepository storeRepository) {
        this.executionRepository = executionRepository;
        this.approvalRepository = approvalRepository;
        this.ruleRepository = ruleRepository;
        this.storeRepository = storeRepository;
    }

    public AutomationPage<AutomationExecutionView> listExecutions(String tenantId,
                                                                 String storeId,
                                                                 String status,
                                                                 String actionType,
                                                                 String triggerSource,
                                                                 Integer page,
                                                                 Integer size) {
        requireOwnedStore(tenantId, storeId);
        Map<String, AutomationRule> ruleMap = ruleRepository.findByStoreIds(List.of(storeId)).stream()
                .collect(Collectors.toMap(AutomationRule::ruleId, Function.identity(), (left, right) -> left));
        List<AutomationExecutionView> filtered = executionRepository.findByTenantId(tenantId).stream()
                .filter(execution -> execution.storeId().equals(storeId))
                .filter(execution -> status == null || status.isBlank() || status.equals(execution.status()))
                .filter(execution -> actionType == null || actionType.isBlank() || actionType.equals(execution.actionType()))
                .filter(execution -> triggerSource == null || triggerSource.isBlank()
                        || triggerSource.equals(execution.triggerSource()))
                .map(execution -> toExecutionView(execution, ruleMap.get(execution.ruleId())))
                .toList();
        return paginate(filtered, page, size);
    }

    public AutomationExecutionView getExecution(String tenantId, String executionId) {
        AutomationExecution execution = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, execution.storeId());
        AutomationRule rule = ruleRepository.findByRuleId(execution.ruleId()).orElse(null);
        return toExecutionView(execution, rule);
    }

    public AutomationStatsView getStats(String tenantId, String storeId) {
        requireOwnedStore(tenantId, storeId);
        List<AutomationExecution> executions = executionRepository.findByTenantId(tenantId).stream()
                .filter(execution -> execution.storeId().equals(storeId))
                .toList();
        LocalDate today = LocalDate.now();
        long success = countByStatus(executions, "SUCCESS");
        long failed = countByStatus(executions, "FAILED");
        long pendingReview = countByStatus(executions, "PENDING_REVIEW");
        long skipped = countByStatus(executions, "SKIPPED");
        List<AutomationExecution> todayExecutions = executions.stream()
                .filter(execution -> execution.executedAt() != null && execution.executedAt().toLocalDate().equals(today))
                .toList();
        long avgCostMs = executions.isEmpty() ? 0L :
                Math.round(executions.stream().mapToLong(AutomationExecution::costMs).average().orElse(0));
        long pendingApprovalCount = approvalRepository.findByTenantId(tenantId).stream()
                .filter(approval -> approval.storeId().equals(storeId))
                .filter(approval -> "PENDING".equals(approval.status()))
                .count();
        Map<String, long[]> byTypeMap = new LinkedHashMap<>();
        for (AutomationExecution execution : executions) {
            long[] bucket = byTypeMap.computeIfAbsent(execution.actionType(), key -> new long[3]);
            bucket[0]++;
            if ("SUCCESS".equals(execution.status())) bucket[1]++;
            if ("FAILED".equals(execution.status())) bucket[2]++;
        }
        List<AutomationActionTypeStatView> byActionType = byTypeMap.entrySet().stream()
                .map(entry -> new AutomationActionTypeStatView(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();
        return new AutomationStatsView(
                executions.size(),
                (int) success,
                (int) failed,
                (int) pendingReview,
                (int) skipped,
                todayExecutions.size(),
                (int) countByStatus(todayExecutions, "SUCCESS"),
                (int) countByStatus(todayExecutions, "FAILED"),
                (int) pendingApprovalCount,
                avgCostMs,
                byActionType
        );
    }

    public AutomationPage<AutomationApprovalView> listApprovals(String tenantId,
                                                               String status,
                                                               String storeId,
                                                               Integer page,
                                                               Integer size) {
        List<AutomationApprovalView> filtered = approvalRepository.findByTenantId(tenantId).stream()
                .filter(approval -> storeId == null || storeId.isBlank() || storeId.equals(approval.storeId()))
                .filter(approval -> status == null || status.isBlank() || status.equals(approval.status()))
                .filter(approval -> {
                    if (storeId == null || storeId.isBlank()) {
                        return true;
                    }
                    return isOwnedStore(tenantId, approval.storeId());
                })
                .map(AutomationQueryService::toApprovalView)
                .collect(Collectors.toList());
        return paginate(filtered, page, size);
    }

    private long countByStatus(List<AutomationExecution> executions, String status) {
        return executions.stream().filter(execution -> status.equals(execution.status())).count();
    }

    private boolean isOwnedStore(String tenantId, String storeId) {
        return storeRepository.findByStoreId(storeId)
                .map(store -> tenantId.equals(store.tenantId()))
                .orElse(false);
    }

    private void requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
    }

    private static AutomationExecutionView toExecutionView(AutomationExecution execution, AutomationRule rule) {
        return new AutomationExecutionView(
                execution.executionId(),
                execution.storeId(),
                execution.ruleId(),
                rule == null ? null : rule.ruleName(),
                execution.actionType(),
                execution.triggerSource(),
                execution.targetType(),
                execution.targetId(),
                execution.result(),
                execution.status(),
                execution.retryCount(),
                execution.costMs(),
                execution.errorMessage(),
                execution.executedAt()
        );
    }

    private static AutomationApprovalView toApprovalView(AutomationApproval approval) {
        return new AutomationApprovalView(
                approval.taskId(),
                approval.executionId(),
                approval.storeId(),
                approval.targetType(),
                approval.targetId(),
                approval.reason(),
                approval.status(),
                approval.reviewedBy(),
                approval.reviewedAt(),
                approval.createdAt()
        );
    }

    private static <T> AutomationPage<T> paginate(List<T> filtered, Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedSize, filtered.size());
        return new AutomationPage<>(new ArrayList<>(filtered.subList(fromIndex, toIndex)),
                normalizedPage, normalizedSize, filtered.size());
    }

    public record AutomationExecutionView(
            String executionId,
            String storeId,
            String ruleId,
            String ruleName,
            String actionType,
            String triggerSource,
            String targetType,
            String targetId,
            String result,
            String status,
            int retryCount,
            long costMs,
            String errorMessage,
            OffsetDateTime executedAt
    ) {
    }

    public record AutomationApprovalView(
            String taskId,
            String executionId,
            String storeId,
            String targetType,
            String targetId,
            String reason,
            String status,
            String reviewedBy,
            OffsetDateTime reviewedAt,
            OffsetDateTime createdAt
    ) {
    }

    public record AutomationStatsView(
            int total,
            int success,
            int failed,
            int pendingReview,
            int skipped,
            int todayTotal,
            int todaySuccess,
            int todayFailed,
            int pendingApprovalCount,
            long avgCostMs,
            List<AutomationActionTypeStatView> byActionType
    ) {
    }

    public record AutomationActionTypeStatView(
            String actionType,
            long count,
            long success,
            long failed
    ) {
    }

    public record AutomationPage<T>(
            List<T> list,
            int page,
            int size,
            long total
    ) {
    }
}
