package backend.rule.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.rule.domain.repository.AutomationRuleRepository;
import backend.rule.model.AutomationRule;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RuleService {

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final AutomationRuleRepository automationRuleRepository;

    public RuleService(AuditLogService auditLogService,
                       StoreRepository storeRepository,
                       AutomationRuleRepository automationRuleRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.automationRuleRepository = automationRuleRepository;
    }

    public List<AutomationRule> listRules(String tenantId) {
        return automationRuleRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public RuleGovernanceOverviewView getGovernanceOverview(String tenantId) {
        List<AutomationRule> rules = listRules(tenantId);
        List<RuleCategoryStatView> categoryStats = rules.stream()
                .collect(Collectors.groupingBy(AutomationRule::ruleCategory, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new RuleCategoryStatView(entry.getKey(), entry.getValue().intValue()))
                .toList();
        List<RuleRiskStatView> riskStats = rules.stream()
                .collect(Collectors.groupingBy(AutomationRule::riskCategory, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new RuleRiskStatView(entry.getKey(), entry.getValue().intValue()))
                .toList();
        return new RuleGovernanceOverviewView(
                rules.size(),
                (int) rules.stream().filter(AutomationRule::enabled).count(),
                (int) rules.stream().filter(rule -> !rule.enabled()).count(),
                (int) rules.stream().filter(this::isFallbackRule).count(),
                (int) rules.stream().filter(this::requiresManualReview).count(),
                categoryStats,
                riskStats
        );
    }

    public List<RuleOrchestrationView> listOrchestrations(String tenantId) {
        Map<String, Store> storeMap = storeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Store::storeId, Function.identity(), (left, right) -> left));
        return listRules(tenantId).stream()
                .map(rule -> new RuleOrchestrationView(
                        rule.ruleId(),
                        rule.storeId(),
                        storeMap.containsKey(rule.storeId()) ? storeMap.get(rule.storeId()).shopName() : null,
                        rule.ruleType(),
                        rule.ruleCategory(),
                        rule.riskCategory(),
                        resolveExecutionStage(rule.ruleCategory()),
                        isFallbackRule(rule),
                        requiresManualReview(rule),
                        rule.enabled(),
                        rule.createdAt()
                ))
                .toList();
    }

    public AutomationRule createRule(String tenantId, SaveRuleRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        AutomationRule saved = automationRuleRepository.save(new AutomationRule(
                null,
                request.storeId(),
                request.ruleType(),
                normalizeCategory(request.ruleCategory()),
                normalizeCategory(request.riskCategory()),
                request.ruleName(),
                request.ruleExpression(),
                request.enabled() == null || request.enabled(),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_AUTOMATION_RULE", "automation_rule", saved.ruleId());
        return saved;
    }

    public AutomationRule updateRule(String tenantId, String ruleId, SaveRuleRequest request) {
        AutomationRule current = requireOwnedRule(tenantId, ruleId);
        if (!current.storeId().equals(request.storeId())) {
            throw new BusinessException("1004", "storeId does not match current rule", HttpStatus.BAD_REQUEST);
        }
        requireOwnedStore(tenantId, request.storeId());
        AutomationRule updated = automationRuleRepository.save(new AutomationRule(
                current.ruleId(),
                current.storeId(),
                request.ruleType(),
                normalizeCategory(request.ruleCategory()),
                normalizeCategory(request.riskCategory()),
                request.ruleName(),
                request.ruleExpression(),
                request.enabled() == null ? current.enabled() : request.enabled(),
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_AUTOMATION_RULE", "automation_rule", ruleId);
        return updated;
    }

    public AutomationRule enableRule(String tenantId, String ruleId) {
        AutomationRule current = requireOwnedRule(tenantId, ruleId);
        if (current.enabled()) {
            return current;
        }
        AutomationRule updated = automationRuleRepository.save(new AutomationRule(
                current.ruleId(),
                current.storeId(),
                current.ruleType(),
                current.ruleCategory(),
                current.riskCategory(),
                current.ruleName(),
                current.ruleExpression(),
                true,
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "ENABLE_AUTOMATION_RULE", "automation_rule", ruleId);
        return updated;
    }

    public AutomationRule disableRule(String tenantId, String ruleId) {
        AutomationRule current = requireOwnedRule(tenantId, ruleId);
        if (!current.enabled()) {
            return current;
        }
        AutomationRule updated = automationRuleRepository.save(new AutomationRule(
                current.ruleId(),
                current.storeId(),
                current.ruleType(),
                current.ruleCategory(),
                current.riskCategory(),
                current.ruleName(),
                current.ruleExpression(),
                false,
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "DISABLE_AUTOMATION_RULE", "automation_rule", ruleId);
        return updated;
    }

    public FallbackSimulationView simulateFallback(String tenantId, FallbackSimulationRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        List<AutomationRule> matchedRules = automationRuleRepository.findByStoreIds(List.of(request.storeId())).stream()
                .filter(AutomationRule::enabled)
                .filter(this::isFallbackRule)
                .filter(rule -> matchesFallbackScenario(rule, request))
                .sorted(Comparator.comparing(AutomationRule::createdAt))
                .toList();
        boolean covered = !matchedRules.isEmpty();
        boolean retrySuggested = matchedRules.stream().anyMatch(this::supportsRetry);
        return new FallbackSimulationView(
                request.storeId(),
                request.scenarioCode(),
                request.failureSource(),
                request.requestedAction(),
                covered ? "covered" : "gap",
                covered,
                retrySuggested,
                "manual_review",
                covered ? "pause_and_escalate" : "manual_gap_review",
                matchedRules
        );
    }

    public void clear() {
        automationRuleRepository.deleteAll();
    }

    private AutomationRule requireOwnedRule(String tenantId, String ruleId) {
        AutomationRule rule = automationRuleRepository.findByRuleId(ruleId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        Store store = storeRepository.findByStoreId(rule.storeId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return rule;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant context invalid", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private String normalizeCategory(String value) {
        return value == null || value.isBlank() ? "general" : value;
    }

    private String resolveExecutionStage(String ruleCategory) {
        return switch (normalizeCategory(ruleCategory).toLowerCase(Locale.ROOT)) {
            case "product_selection" -> "pre_check";
            case "live", "qa" -> "runtime";
            case "fulfillment" -> "post_check";
            default -> "general";
        };
    }

    private boolean isFallbackRule(AutomationRule rule) {
        String normalizedText = normalizedRuleText(rule);
        return normalizedText.contains("fallback") || normalizedText.contains("retry");
    }

    private boolean requiresManualReview(AutomationRule rule) {
        String normalizedText = normalizedRuleText(rule);
        return normalizedText.contains("manual") || normalizedText.contains("review");
    }

    private boolean supportsRetry(AutomationRule rule) {
        return normalizedRuleText(rule).contains("retry");
    }

    private boolean matchesFallbackScenario(AutomationRule rule, FallbackSimulationRequest request) {
        String scenarioText = normalizeText(request.scenarioCode()) + " " + normalizeText(request.failureSource());
        return List.of(rule.ruleCategory(), rule.riskCategory(), rule.ruleType()).stream()
                .filter(Objects::nonNull)
                .map(this::normalizeText)
                .anyMatch(candidate -> !candidate.isBlank() && scenarioText.contains(candidate));
    }

    private String normalizedRuleText(AutomationRule rule) {
        return List.of(rule.ruleType(), rule.ruleName(), rule.ruleExpression()).stream()
                .filter(Objects::nonNull)
                .map(this::normalizeText)
                .collect(Collectors.joining(" "));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record SaveRuleRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "ruleType is required")
            String ruleType,
            String ruleCategory,
            String riskCategory,
            @NotBlank(message = "ruleName is required")
            String ruleName,
            @NotBlank(message = "ruleExpression is required")
            String ruleExpression,
            Boolean enabled
    ) {
    }

    public record RuleGovernanceOverviewView(
            int totalRules,
            int enabledRules,
            int disabledRules,
            int fallbackRuleCount,
            int manualReviewRuleCount,
            List<RuleCategoryStatView> categoryStats,
            List<RuleRiskStatView> riskStats
    ) {
    }

    public record RuleCategoryStatView(
            String ruleCategory,
            int ruleCount
    ) {
    }

    public record RuleRiskStatView(
            String riskCategory,
            int ruleCount
    ) {
    }

    public record RuleOrchestrationView(
            String ruleId,
            String storeId,
            String shopName,
            String ruleType,
            String ruleCategory,
            String riskCategory,
            String executionStage,
            boolean fallbackEnabled,
            boolean manualReviewRequired,
            boolean enabled,
            OffsetDateTime createdAt
    ) {
    }

    public record FallbackSimulationRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "scenarioCode is required")
            String scenarioCode,
            @NotBlank(message = "failureSource is required")
            String failureSource,
            @NotBlank(message = "requestedAction is required")
            String requestedAction,
            Integer signalValue
    ) {
    }

    public record FallbackSimulationView(
            String storeId,
            String scenarioCode,
            String failureSource,
            String requestedAction,
            String coverageStatus,
            boolean fallbackTriggered,
            boolean retrySuggested,
            String suggestedAction,
            String terminalAction,
            List<AutomationRule> matchedRules
    ) {
    }
}

