package backend.live.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.live.application.LiveService.*;
import backend.live.domain.repository.LivePlanRepository;
import backend.live.domain.repository.LiveProductItemRepository;
import backend.live.domain.repository.LiveScriptRepository;
import backend.live.domain.repository.LiveSessionRepository;
import backend.live.model.LivePlan;
import backend.live.model.LiveProductItem;
import backend.live.model.LiveScript;
import backend.live.model.LiveSession;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import backend.product.domain.repository.CandidateProductRepository;
import backend.product.domain.repository.ProductDraftRepository;
import backend.product.domain.repository.ProductRepository;
import backend.product.model.CandidateProduct;
import backend.product.model.Product;
import backend.product.model.ProductDraft;
import backend.saas.domain.repository.UsageQuotaRepository;
import backend.saas.model.UsageQuota;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.ChannelAccount;
import backend.store.model.Store;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LiveApplicationService {

    private final AuditLogService auditLogService;
    private final OrganizationRepository organizationRepository;
    private final UsageQuotaRepository usageQuotaRepository;
    private final StoreRepository storeRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final LivePlanRepository livePlanRepository;
    private final LiveProductItemRepository liveProductItemRepository;
    private final LiveScriptRepository liveScriptRepository;
    private final LiveSessionRepository liveSessionRepository;
    private final CandidateProductRepository candidateProductRepository;
    private final ProductDraftRepository productDraftRepository;
    private final ProductRepository productRepository;
    private final Environment environment;
    private final Map<String, CommitmentWhitelistConfig> commitmentWhitelistConfigs = new ConcurrentHashMap<>();

    public LiveApplicationService(AuditLogService auditLogService,
                                  OrganizationRepository organizationRepository,
                                  UsageQuotaRepository usageQuotaRepository,
                                  StoreRepository storeRepository,
                                  ChannelAccountRepository channelAccountRepository,
                                  LivePlanRepository livePlanRepository,
                                  LiveProductItemRepository liveProductItemRepository,
                                  LiveScriptRepository liveScriptRepository,
                                  LiveSessionRepository liveSessionRepository,
                                  CandidateProductRepository candidateProductRepository,
                                  ProductDraftRepository productDraftRepository,
                                  ProductRepository productRepository,
                                  Environment environment) {
        this.auditLogService = auditLogService;
        this.organizationRepository = organizationRepository;
        this.usageQuotaRepository = usageQuotaRepository;
        this.storeRepository = storeRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.livePlanRepository = livePlanRepository;
        this.liveProductItemRepository = liveProductItemRepository;
        this.liveScriptRepository = liveScriptRepository;
        this.liveSessionRepository = liveSessionRepository;
        this.candidateProductRepository = candidateProductRepository;
        this.productDraftRepository = productDraftRepository;
        this.productRepository = productRepository;
        this.environment = environment;
    }

    public List<LivePlan> listLivePlans(String tenantId) {
        return livePlanRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public List<LiveSession> listLiveSessions(String tenantId) {
        return liveSessionRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public LiveSessionStatusView getLiveSessionStatus(String tenantId, String liveSessionId) {
        LiveSession liveSession = requireOwnedSession(tenantId, liveSessionId);
        LivePlan livePlan = getLivePlan(tenantId, liveSession.livePlanId());
        RuntimeReadinessSnapshot runtimeSnapshot = buildRuntimeReadinessSnapshot();
        List<LiveSession> tenantSessions = listLiveSessions(tenantId);
        int tenantQuotaLimit = usageQuotaRepository.findByTenantId(tenantId).stream()
                .filter(quota -> "live_concurrency".equals(quota.quotaCode()))
                .findFirst()
                .map(UsageQuota::quotaLimit)
                .orElse(0);
        int runningSessionCount = (int) tenantSessions.stream()
                .filter(session -> "running".equals(session.sessionStatus()))
                .count();
        LiveAccountView liveAccount = livePlan.liveAccountId() == null || livePlan.liveAccountId().isBlank()
                ? null
                : listLiveAccounts(tenantId).stream()
                .filter(account -> livePlan.liveAccountId().equals(account.liveAccountId()))
                .findFirst()
                .orElse(null);
        LiveScript activeScript = liveScriptRepository.findByLivePlanId(livePlan.livePlanId()).stream()
                .filter(LiveScript::active)
                .findFirst()
                .orElse(null);
        LiveBoundProductItemView currentBoundProduct = resolveCurrentBoundProduct(livePlan, activeScript);
        return new LiveSessionStatusView(
                liveSession.liveSessionId(),
                liveSession.livePlanId(),
                livePlan.planStatus(),
                liveSession.sessionStatus(),
                liveSession.storeId(),
                liveSession.liveAccountId(),
                liveAccount == null ? null : liveAccount.accountName(),
                liveSession.roomId(),
                liveSession.actualStartAt(),
                liveSession.actualEndAt(),
                calculateDurationSeconds(liveSession),
                liveAccount != null && liveAccount.occupied(),
                tenantQuotaLimit,
                Math.max(tenantQuotaLimit - runningSessionCount, 0),
                currentBoundProduct == null ? null : currentBoundProduct.productId(),
                currentBoundProduct == null ? null : currentBoundProduct.title(),
                currentBoundProduct == null ? null : currentBoundProduct.sourceType(),
                activeScript == null ? null : activeScript.scriptVersion(),
                activeScript == null ? null : toSnippet(activeScript.scriptContent()),
                liveSession.controlMode(),
                liveSession.currentScene(),
                liveSession.takeoverStatus(),
                liveSession.takeoverOperator(),
                liveSession.promiseAuditStatus(),
                liveSession.promiseAuditRemark(),
                runtimeSnapshot.runtimeReady(),
                runtimeSnapshot.callbackReady(),
                runtimeSnapshot.strictProviderGate(),
                runtimeSnapshot.strictCallbackGate(),
                runtimeSnapshot.providerName(),
                runtimeSnapshot.providerEndpoint(),
                runtimeSnapshot.callbackEndpoint(),
                runtimeSnapshot.readinessMessage(),
                liveSession.errorMessage()
        );
    }

    public LiveConcurrencyOverviewView getConcurrencyOverview(String tenantId) {
        List<LiveSession> runningSessions = listLiveSessions(tenantId).stream()
                .filter(session -> "running".equals(session.sessionStatus()))
                .toList();
        int tenantQuotaLimit = usageQuotaRepository.findByTenantId(tenantId).stream()
                .filter(quota -> "live_concurrency".equals(quota.quotaCode()))
                .findFirst()
                .map(UsageQuota::quotaLimit)
                .orElse(0);
        List<LiveAccountView> liveAccounts = listLiveAccounts(tenantId);
        Map<String, LiveAccountView> accountViewMap = liveAccounts.stream()
                .collect(Collectors.toMap(LiveAccountView::liveAccountId, Function.identity(), (left, right) -> left));
        List<LiveRunningSessionView> runningSessionViews = runningSessions.stream()
                .map(session -> new LiveRunningSessionView(
                        session.liveSessionId(),
                        session.livePlanId(),
                        session.storeId(),
                        session.liveAccountId(),
                        session.sessionStatus(),
                        session.roomId(),
                        session.actualStartAt(),
                        session.errorMessage()
                ))
                .toList();
        List<LiveAccountOccupancyView> occupiedAccounts = runningSessions.stream()
                .map(session -> new LiveAccountOccupancyView(
                        session.liveAccountId(),
                        accountViewMap.containsKey(session.liveAccountId())
                                ? accountViewMap.get(session.liveAccountId()).accountName()
                                : null,
                        session.liveSessionId(),
                        session.livePlanId(),
                        session.storeId(),
                        session.actualStartAt()
                ))
                .toList();
        return new LiveConcurrencyOverviewView(
                tenantId,
                runningSessions.size(),
                tenantQuotaLimit,
                Math.max(tenantQuotaLimit - runningSessions.size(), 0),
                runningSessionViews,
                occupiedAccounts
        );
    }

    public List<LiveConcurrencyQueueView> listConcurrencyQueues(String tenantId) {
        return buildConcurrencyQueueViews(tenantId);
    }

    public List<LiveRiskEventView> listLiveRiskEvents(String tenantId) {
        return buildLiveRiskEvents(tenantId);
    }

    public List<LiveRiskEventView> listLiveSessionRiskEvents(String tenantId, String liveSessionId) {
        requireOwnedSession(tenantId, liveSessionId);
        return buildLiveRiskEvents(tenantId).stream()
                .filter(event -> liveSessionId.equals(event.liveSessionId()))
                .toList();
    }

    public LiveSpecialAnalysisView getLiveSpecialAnalysis(String tenantId) {
        RuntimeReadinessSnapshot runtimeSnapshot = buildRuntimeReadinessSnapshot();
        List<LiveAccountView> liveAccounts = listLiveAccounts(tenantId);
        List<LiveConcurrencyQueueView> queueViews = buildConcurrencyQueueViews(tenantId);
        List<LiveRiskEventView> riskEvents = buildLiveRiskEvents(tenantId);
        List<LiveSession> sessions = listLiveSessions(tenantId);
        return new LiveSpecialAnalysisView(
                liveAccounts.size(),
                (int) liveAccounts.stream().filter(LiveAccountView::occupied).count(),
                queueViews.size(),
                riskEvents.size(),
                (int) sessions.stream().filter(session -> "manual".equals(defaultIfBlank(session.takeoverStatus(), "auto"))).count(),
                (int) sessions.stream().filter(session -> "rejected".equals(defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"))).count(),
                (int) sessions.stream().filter(session -> !"standard".equals(defaultIfBlank(session.controlMode(), "standard"))).count(),
                (int) sessions.stream().filter(session -> "failed".equals(session.sessionStatus())).count(),
                (int) queueViews.stream().filter(view -> List.of("runtime_provider_not_ready", "callback_not_ready").contains(view.blockedType())).count(),
                runtimeSnapshot.runtimeReady(),
                runtimeSnapshot.callbackReady(),
                runtimeSnapshot.providerName()
        );
    }

    public List<LiveRiskRecoveryPlanView> listRiskRecoveryPlans(String tenantId) {
        List<LiveConcurrencyQueueView> queueViews = buildConcurrencyQueueViews(tenantId);
        List<LiveRiskRecoveryPlanView> recoveryPlans = new ArrayList<>();
        for (LiveSession session : listLiveSessions(tenantId)) {
            String riskCode = null;
            String riskLevel = null;
            String recoveryAction = null;
            String blockingReason = null;
            if ("failed".equals(session.sessionStatus())) {
                riskCode = "live_session_failed";
                riskLevel = "critical";
                recoveryAction = "restart_live_plan";
                blockingReason = defaultIfBlank(session.errorMessage(), "live session failed");
            } else if ("rejected".equals(defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"))) {
                riskCode = "live_promise_audit_rejected";
                riskLevel = "high";
                recoveryAction = "manual_review_and_script_fix";
                blockingReason = defaultIfBlank(session.promiseAuditRemark(), "promise audit rejected");
            } else if ("manual".equals(defaultIfBlank(session.takeoverStatus(), "auto"))) {
                riskCode = "manual_takeover_active";
                riskLevel = "high";
                recoveryAction = "operator_handover_followup";
                blockingReason = defaultIfBlank(session.takeoverOperator(), "manual takeover active");
            } else if (!"standard".equals(defaultIfBlank(session.controlMode(), "standard"))) {
                riskCode = "live_strong_control_enabled";
                riskLevel = "medium";
                recoveryAction = "relax_control_after_review";
                blockingReason = "control mode=" + session.controlMode();
            }
            if (riskCode != null) {
                recoveryPlans.add(new LiveRiskRecoveryPlanView(
                        "live_session",
                        session.liveSessionId(),
                        session.liveSessionId(),
                        session.livePlanId(),
                        riskCode,
                        riskLevel,
                        recoveryAction,
                        List.of("high", "critical").contains(riskLevel),
                        blockingReason
                ));
            }
        }
        for (LiveConcurrencyQueueView queueView : queueViews) {
            recoveryPlans.add(new LiveRiskRecoveryPlanView(
                    "queued_plan",
                    queueView.livePlanId(),
                    null,
                    queueView.livePlanId(),
                    queueView.blockedType(),
                    "high",
                    switch (queueView.blockedType()) {
                        case "account_missing" -> "bind_live_account";
                        case "account_auth_invalid" -> "refresh_live_account_auth";
                        case "account_occupied" -> "reassign_or_wait_account";
                        case "runtime_provider_not_ready" -> "complete_runtime_provider_config";
                        case "callback_not_ready" -> "complete_callback_config";
                        default -> "release_tenant_quota";
                    },
                    true,
                    queueView.blockedReason()
            ));
        }
        recoveryPlans.sort((left, right) -> {
            int severityCompare = Integer.compare(riskRank(right.riskLevel()), riskRank(left.riskLevel()));
            return severityCompare != 0 ? severityCompare : left.entityId().compareTo(right.entityId());
        });
        return recoveryPlans;
    }

    public LiveSpecialAnalysisDrilldownView getLiveSpecialAnalysisDrilldown(String tenantId) {
        List<LiveSession> sessions = listLiveSessions(tenantId);
        List<LiveRiskEventView> riskEvents = buildLiveRiskEvents(tenantId);
        List<LiveConcurrencyQueueView> queueViews = buildConcurrencyQueueViews(tenantId);
        List<String> highRiskSessionIds = riskEvents.stream()
                .filter(event -> List.of("high", "critical").contains(event.severity()))
                .map(LiveRiskEventView::liveSessionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> queuedPlanIds = listLivePlans(tenantId).stream()
                .filter(plan -> List.of("ready", "scheduled").contains(plan.planStatus()))
                .map(LivePlan::livePlanId)
                .distinct()
                .toList();
        return new LiveSpecialAnalysisDrilldownView(
                highRiskSessionIds,
                sessions.stream()
                        .filter(session -> "failed".equals(session.sessionStatus()))
                        .map(LiveSession::liveSessionId)
                        .toList(),
                sessions.stream()
                        .filter(session -> "manual".equals(defaultIfBlank(session.takeoverStatus(), "auto")))
                        .map(LiveSession::liveSessionId)
                        .toList(),
                sessions.stream()
                        .filter(session -> "rejected".equals(defaultIfBlank(session.promiseAuditStatus(), "not_reviewed")))
                        .map(LiveSession::liveSessionId)
                        .toList(),
                queuedPlanIds,
                queueViews.stream().map(LiveConcurrencyQueueView::blockedType).distinct().toList(),
                riskEvents.stream().map(LiveRiskEventView::eventCode).distinct().toList()
        );
    }

    public LivePlan createLivePlan(String tenantId, CreateLivePlanRequest request) {
        validateScheduleWindow(request.scheduledStartAt(), request.scheduledEndAt());
        requireOwnedStore(tenantId, request.storeId());
        if (request.liveAccountId() != null && !request.liveAccountId().isBlank()) {
            requireOwnedChannelAccount(tenantId, request.liveAccountId());
        }
        LivePlan livePlan = livePlanRepository.save(new LivePlan(
                null,
                request.storeId(),
                request.liveAccountId(),
                request.planName(),
                "draft",
                request.scheduledStartAt(),
                request.scheduledEndAt(),
                request.anchorProfileName(),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_LIVE_PLAN", "live_plan", livePlan.livePlanId());
        return livePlan;
    }

    public LivePlan updateLivePlan(String tenantId, String livePlanId, UpdateLivePlanRequest request) {
        validateScheduleWindow(request.scheduledStartAt(), request.scheduledEndAt());
        LivePlan current = getLivePlan(tenantId, livePlanId);
        if (!List.of("draft", "ready", "scheduled").contains(current.planStatus())) {
            throw new BusinessException("7110", "live plan status does not allow update", HttpStatus.BAD_REQUEST);
        }
        if (request.liveAccountId() != null && !request.liveAccountId().isBlank()) {
            requireOwnedChannelAccount(tenantId, request.liveAccountId());
        }
        LivePlan updated = livePlanRepository.save(current.withEditableFields(
                request.liveAccountId(),
                request.planName(),
                request.scheduledStartAt(),
                request.scheduledEndAt(),
                request.anchorProfileName()
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_LIVE_PLAN", "live_plan", livePlanId);
        return updated;
    }

    public LivePlan getLivePlan(String tenantId, String livePlanId) {
        LivePlan livePlan = livePlanRepository.findByLivePlanId(livePlanId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, livePlan.storeId());
        return livePlan;
    }

    public LiveCommitmentWhitelistConfigView configureCommitmentWhitelist(String tenantId,
                                                                          String livePlanId,
                                                                          ConfigureCommitmentWhitelistCommand request) {
        getLivePlan(tenantId, livePlanId);
        CommitmentWhitelistConfig config = new CommitmentWhitelistConfig(
                livePlanId,
                normalizePhraseList(request.whitelistPhrases()),
                normalizePhraseList(request.manualConfirmPhrases()),
                normalizePhraseList(request.prohibitedPhrases()),
                OffsetDateTime.now()
        );
        commitmentWhitelistConfigs.put(livePlanId, config);
        auditLogService.recordForTenant(tenantId, "CONFIGURE_LIVE_COMMITMENT_WHITELIST", "live_plan", livePlanId);
        return new LiveCommitmentWhitelistConfigView(
                config.livePlanId(),
                config.whitelistPhrases(),
                config.manualConfirmPhrases(),
                config.prohibitedPhrases(),
                config.updatedAt()
        );
    }

    public List<LiveProductPoolItemView> getLiveProductPool(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        String storeId = livePlan.storeId();
        List<String> storeIds = List.of(storeId);
        Map<String, CandidateProduct> candidateMap = candidateProductRepository.findByStoreIds(storeIds).stream()
                .collect(Collectors.toMap(CandidateProduct::candidateProductId, Function.identity(), (left, right) -> left));
        Map<String, ProductDraft> draftMap = productDraftRepository.findByStoreIds(storeIds).stream()
                .collect(Collectors.toMap(ProductDraft::productDraftId, Function.identity(), (left, right) -> left));
        List<LiveProductPoolItemView> poolItems = new ArrayList<>();
        for (Product product : productRepository.findByStoreIds(storeIds)) {
            ProductDraft draft = draftMap.get(product.productDraftId());
            CandidateProduct candidate = draft == null ? null : candidateMap.get(draft.candidateProductId());
            poolItems.add(toPoolItem(storeId, candidate, draft, product));
        }
        for (ProductDraft draft : draftMap.values()) {
            boolean published = poolItems.stream().anyMatch(item -> draft.productDraftId().equals(item.productDraftId()));
            if (!published) {
                CandidateProduct candidate = candidateMap.get(draft.candidateProductId());
                poolItems.add(toPoolItem(storeId, candidate, draft, null));
            }
        }
        for (CandidateProduct candidate : candidateMap.values()) {
            boolean consumed = poolItems.stream().anyMatch(item -> candidate.candidateProductId().equals(item.candidateProductId()));
            if (!consumed) {
                poolItems.add(toPoolItem(storeId, candidate, null, null));
            }
        }
        poolItems.sort((left, right) -> {
            int leftRank = rankPoolItem(left);
            int rightRank = rankPoolItem(right);
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }
            return left.title().compareToIgnoreCase(right.title());
        });
        return poolItems;
    }

    public List<LiveBoundProductItemView> listBoundLiveProducts(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        return listBoundLiveProductsByPlan(livePlan);
    }

    public LiveBoundProductItemView bindLiveProduct(String tenantId, String livePlanId, BindLiveProductRequest request) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        if (!List.of("draft", "ready", "scheduled").contains(livePlan.planStatus())) {
            throw new BusinessException("7121", "live plan status does not allow bind product", HttpStatus.BAD_REQUEST);
        }
        ResolvedLiveProductSource resolved = resolveLiveProductSource(livePlan, request);
        List<LiveProductItem> existingItems = liveProductItemRepository.findByLivePlanId(livePlanId);
        LiveProductItem existing = findExistingBoundItem(existingItems, resolved).orElse(null);
        int sortOrder = request.sortOrder() != null
                ? request.sortOrder()
                : existing != null ? existing.sortOrder() : nextSortOrder(existingItems);
        LiveProductItem saved = liveProductItemRepository.save(new LiveProductItem(
                existing == null ? null : existing.liveProductItemId(),
                livePlanId,
                resolved.candidateProductId(),
                resolved.productDraftId(),
                resolved.productId(),
                sortOrder,
                existing == null ? OffsetDateTime.now() : existing.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "BIND_LIVE_PRODUCT", "live_plan", livePlanId);
        rebuildActiveScriptForPlanIfPresent(livePlan);
        return toBoundItemView(livePlan.storeId(), livePlanId, saved);
    }

    public LiveBoundProductItemView removeBoundLiveProduct(String tenantId, String livePlanId, String liveProductItemId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        if (!List.of("draft", "ready", "scheduled").contains(livePlan.planStatus())) {
            throw new BusinessException("7125", "live plan status does not allow remove product", HttpStatus.BAD_REQUEST);
        }
        LiveProductItem liveProductItem = liveProductItemRepository.findByLiveProductItemId(liveProductItemId)
                .orElseThrow(() -> new BusinessException("7124", "live product item not found", HttpStatus.NOT_FOUND));
        if (!livePlanId.equals(liveProductItem.livePlanId())) {
            throw new BusinessException("7124", "live product item not found", HttpStatus.NOT_FOUND);
        }
        LiveBoundProductItemView removed = toBoundItemView(livePlan.storeId(), livePlanId, liveProductItem);
        liveProductItemRepository.deleteByLiveProductItemId(liveProductItemId);
        auditLogService.recordForTenant(tenantId, "REMOVE_LIVE_PRODUCT", "live_plan", livePlanId);
        rebuildActiveScriptForPlanIfPresent(livePlan);
        return removed;
    }

    public List<LiveAccountView> listLiveAccounts(String tenantId) {
        List<String> organizationIds = organizationRepository.findByTenantId(tenantId).stream()
                .map(Organization::id)
                .toList();
        return channelAccountRepository.findByOrganizationIds(organizationIds).stream()
                .map(account -> {
                    LiveSession runningSession = liveSessionRepository.findRunningByLiveAccountId(account.channelAccountId()).orElse(null);
                    return new LiveAccountView(
                            account.channelAccountId(),
                            account.organizationId(),
                            account.channelType(),
                            account.accountName(),
                            account.authStatus(),
                            runningSession != null,
                            runningSession == null ? null : runningSession.liveSessionId(),
                            account.expiresAt()
                    );
                })
                .toList();
    }

    public List<LiveAccountGovernanceView> listLiveAccountGovernance(String tenantId) {
        List<LiveAccountView> liveAccounts = listLiveAccounts(tenantId);
        Map<String, Long> runningSessionCountByAccount = listLiveSessions(tenantId).stream()
                .filter(session -> "running".equals(session.sessionStatus()))
                .collect(Collectors.groupingBy(LiveSession::liveAccountId, Collectors.counting()));
        Map<String, Long> queuedPlanCountByAccount = buildConcurrencyQueueViews(tenantId).stream()
                .filter(item -> item.liveAccountId() != null && !item.liveAccountId().isBlank())
                .collect(Collectors.groupingBy(LiveConcurrencyQueueView::liveAccountId, Collectors.counting()));
        OffsetDateTime now = OffsetDateTime.now();
        return liveAccounts.stream()
                .map(account -> {
                    int runningSessionCount = runningSessionCountByAccount.getOrDefault(account.liveAccountId(), 0L).intValue();
                    int queuedPlanCount = queuedPlanCountByAccount.getOrDefault(account.liveAccountId(), 0L).intValue();
                    boolean expiringSoon = account.expiresAt() != null && !account.expiresAt().isAfter(now.plusDays(7));
                    String governanceRiskLevel = resolveGovernanceRiskLevel(account, runningSessionCount, queuedPlanCount, expiringSoon);
                    return new LiveAccountGovernanceView(
                            account.liveAccountId(),
                            account.organizationId(),
                            account.accountName(),
                            account.authStatus(),
                            account.occupied(),
                            account.occupiedSessionId(),
                            runningSessionCount,
                            queuedPlanCount,
                            expiringSoon,
                            governanceRiskLevel,
                            account.expiresAt()
                    );
                })
                .toList();
    }

    public LiveScript generateScript(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        for (LiveScript existing : liveScriptRepository.findByLivePlanId(livePlanId)) {
            if (existing.active()) {
                liveScriptRepository.save(existing.withActive(false));
            }
        }
        Store store = requireOwnedStore(tenantId, livePlan.storeId());
        String scriptVersion = "v" + (liveScriptRepository.findByLivePlanId(livePlanId).size() + 1);
        String content = """
                Generate a live script for store %s, plan %s, and anchor %s. Focus on product highlights, trust signals, conversion prompts, and smooth transitions.
                """.formatted(
                store.shopName(),
                livePlan.planName(),
                livePlan.anchorProfileName() == null || livePlan.anchorProfileName().isBlank() ? "default anchor" : livePlan.anchorProfileName()).trim();
        List<LiveBoundProductItemView> boundItems = listBoundLiveProductsByPlan(livePlan);
        String activeProductId = null;
        List<String> scriptSegments = buildScriptSegments(store, livePlan, boundItems);
        if (!scriptSegments.isEmpty()) {
            content = String.join(System.lineSeparator(), scriptSegments);
        }
        if (!boundItems.isEmpty()) {
            activeProductId = boundItems.get(0).productId();
        }
        LiveScript liveScript = liveScriptRepository.save(new LiveScript(
                null,
                livePlanId,
                activeProductId,
                scriptVersion,
                content,
                true,
                OffsetDateTime.now()
        ));
        livePlanRepository.save(livePlan.withStatus("ready"));
        auditLogService.recordForTenant(tenantId, "GENERATE_LIVE_SCRIPT", "live_plan", livePlanId);
        return liveScript;
    }

    public LiveScriptReviewView reviewScript(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        if (!List.of("draft", "ready", "scheduled").contains(livePlan.planStatus())) {
            throw new BusinessException("7119", "live plan status does not allow script review", HttpStatus.BAD_REQUEST);
        }
        LiveScript activeScript = requireActiveScript(livePlanId);
        auditLogService.recordForTenant(tenantId, "REVIEW_LIVE_SCRIPT", "live_plan", livePlanId);
        return new LiveScriptReviewView(
                livePlanId,
                activeScript.scriptVersion(),
                toSnippet(activeScript.scriptContent()),
                true,
                true
        );
    }

    public LivePlan publishLivePlan(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        if (!List.of("draft", "ready", "scheduled").contains(livePlan.planStatus())) {
            throw new BusinessException("7120", "live plan status does not allow publish", HttpStatus.BAD_REQUEST);
        }
        requireActiveScript(livePlanId);
        LivePlan published = "draft".equals(livePlan.planStatus())
                ? livePlanRepository.save(livePlan.withStatus("ready"))
                : livePlan;
        auditLogService.recordForTenant(tenantId, "PUBLISH_LIVE_PLAN", "live_plan", livePlanId);
        return published;
    }

    public LivePlan scheduleLivePlan(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        if (!List.of("ready", "scheduled").contains(livePlan.planStatus())) {
            throw new BusinessException("7111", "live plan status does not allow schedule", HttpStatus.BAD_REQUEST);
        }
        if (livePlan.scheduledStartAt() == null) {
            throw new BusinessException("7112", "scheduled start time is required", HttpStatus.BAD_REQUEST);
        }
        LivePlan updated = livePlanRepository.save(livePlan.withStatus("scheduled"));
        auditLogService.recordForTenant(tenantId, "SCHEDULE_LIVE_PLAN", "live_plan", livePlanId);
        return updated;
    }

    public LivePlan duplicateLivePlan(String tenantId, String livePlanId) {
        LivePlan source = getLivePlan(tenantId, livePlanId);
        if (source.liveAccountId() != null && !source.liveAccountId().isBlank()) {
            requireOwnedChannelAccount(tenantId, source.liveAccountId());
        }
        LivePlan duplicated = livePlanRepository.save(new LivePlan(
                null,
                source.storeId(),
                source.liveAccountId(),
                source.planName() + "-copy",
                "draft",
                source.scheduledStartAt(),
                source.scheduledEndAt(),
                source.anchorProfileName(),
                OffsetDateTime.now()
        ));
        copyBoundProducts(source.livePlanId(), duplicated.livePlanId());
        auditLogService.recordForTenant(tenantId, "DUPLICATE_LIVE_PLAN", "live_plan", duplicated.livePlanId());
        return duplicated;
    }

    public LivePlan cancelLivePlan(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        if (!List.of("draft", "ready", "scheduled").contains(livePlan.planStatus())) {
            throw new BusinessException("7113", "live plan status does not allow cancel", HttpStatus.BAD_REQUEST);
        }
        LivePlan updated = livePlanRepository.save(livePlan.withStatus("cancelled"));
        auditLogService.recordForTenant(tenantId, "CANCEL_LIVE_PLAN", "live_plan", livePlanId);
        return updated;
    }

    public LiveConcurrencyCheckView validateConcurrency(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        RuntimeReadinessSnapshot runtimeSnapshot = buildRuntimeReadinessSnapshot();
        int tenantQuotaLimit = usageQuotaRepository.findByTenantId(tenantId).stream()
                .filter(quota -> "live_concurrency".equals(quota.quotaCode()))
                .findFirst()
                .map(UsageQuota::quotaLimit)
                .orElse(0);
        int tenantRunningCount = liveSessionRepository.countRunningByTenantId(tenantId);
        if (livePlan.liveAccountId() == null || livePlan.liveAccountId().isBlank()) {
            return new LiveConcurrencyCheckView(
                    livePlanId,
                    false,
                    false,
                    tenantRunningCount >= tenantQuotaLimit,
                    tenantRunningCount,
                    tenantQuotaLimit,
                    runtimeSnapshot.runtimeReady(),
                    runtimeSnapshot.callbackReady(),
                    runtimeSnapshot.providerName(),
                    "live account not configured"
            );
        }
        requireOwnedChannelAccount(tenantId, livePlan.liveAccountId());
        boolean accountOccupied = liveSessionRepository.findRunningByLiveAccountId(livePlan.liveAccountId()).isPresent();
        boolean tenantQuotaExceeded = tenantRunningCount >= tenantQuotaLimit;
        boolean runtimeBlocked = runtimeSnapshot.strictProviderGate() && !runtimeSnapshot.runtimeReady();
        boolean callbackBlocked = runtimeSnapshot.strictCallbackGate() && !runtimeSnapshot.callbackReady();
        boolean allowed = !accountOccupied && !tenantQuotaExceeded && !runtimeBlocked && !callbackBlocked;
        String reason = allowed ? "passed"
                : accountOccupied ? "live account occupied"
                : tenantQuotaExceeded ? "tenant live concurrency quota exceeded"
                : runtimeBlocked ? "live runtime provider not ready"
                : "live callback not ready";
        auditLogService.recordForTenant(tenantId, "VALIDATE_LIVE_CONCURRENCY", "live_plan", livePlanId);
        return new LiveConcurrencyCheckView(
                livePlanId,
                allowed,
                accountOccupied,
                tenantQuotaExceeded,
                tenantRunningCount,
                tenantQuotaLimit,
                runtimeSnapshot.runtimeReady(),
                runtimeSnapshot.callbackReady(),
                runtimeSnapshot.providerName(),
                reason
        );
    }

    public LiveSession startLive(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        RuntimeReadinessSnapshot runtimeSnapshot = buildRuntimeReadinessSnapshot();
        if (!"ready".equals(livePlan.planStatus()) && !"scheduled".equals(livePlan.planStatus())) {
            throw new BusinessException("7101", "live plan status does not allow start", HttpStatus.BAD_REQUEST);
        }
        LiveConcurrencyCheckView checkView = validateConcurrency(tenantId, livePlanId);
        if (!checkView.allowed()) {
            throw new BusinessException("7102", checkView.reason(), HttpStatus.BAD_REQUEST);
        }
        LiveSession liveSession = liveSessionRepository.save(new LiveSession(
                null,
                livePlanId,
                tenantId,
                livePlan.storeId(),
                livePlan.liveAccountId(),
                "running",
                runtimeSnapshot.runtimeReady() || !runtimeSnapshot.allowMockRoomId()
                        ? "room-" + System.currentTimeMillis()
                        : "mock-room-" + System.currentTimeMillis(),
                OffsetDateTime.now(),
                null,
                null,
                "standard",
                "default_scene",
                "auto",
                null,
                "not_reviewed",
                null,
                OffsetDateTime.now()
        ));
        livePlanRepository.save(livePlan.withStatus("running"));
        auditLogService.recordForTenant(tenantId, "START_LIVE", "live_plan", livePlanId);
        return liveSession;
    }

    public LiveSession pauseLive(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        LiveSession current = requireLatestSession(livePlanId);
        if (!"running".equals(current.sessionStatus())) {
            throw new BusinessException("7103", "live session status does not allow pause", HttpStatus.BAD_REQUEST);
        }
        LiveSession updated = liveSessionRepository.save(current.withStatus("paused", null, current.errorMessage()));
        livePlanRepository.save(livePlan.withStatus("ready"));
        auditLogService.recordForTenant(tenantId, "PAUSE_LIVE", "live_plan", livePlanId);
        return updated;
    }

    public LiveSession resumeLive(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        LiveSession current = requireLatestSession(livePlanId);
        if (!"paused".equals(current.sessionStatus())) {
            throw new BusinessException("7117", "live session status does not allow resume", HttpStatus.BAD_REQUEST);
        }
        if (!Objects.equals(livePlan.liveAccountId(), current.liveAccountId())) {
            throw new BusinessException("7118", "live plan account changed after pause, please start a new live", HttpStatus.BAD_REQUEST);
        }
        LiveConcurrencyCheckView checkView = validateConcurrency(tenantId, livePlanId);
        if (!checkView.allowed()) {
            throw new BusinessException("7102", checkView.reason(), HttpStatus.BAD_REQUEST);
        }
        LiveSession updated = liveSessionRepository.save(ensureSessionRunning(current));
        livePlanRepository.save(livePlan.withStatus("running"));
        auditLogService.recordForTenant(tenantId, "RESUME_LIVE", "live_plan", livePlanId);
        return updated;
    }

    public LiveSession stopLive(String tenantId, String livePlanId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        LiveSession current = requireLatestSession(livePlanId);
        if (!"running".equals(current.sessionStatus()) && !"paused".equals(current.sessionStatus())) {
            throw new BusinessException("7104", "live session status does not allow stop", HttpStatus.BAD_REQUEST);
        }
        LiveSession updated = liveSessionRepository.save(current.withStatus("ended", OffsetDateTime.now(), current.errorMessage()));
        livePlanRepository.save(livePlan.withStatus("ended"));
        auditLogService.recordForTenant(tenantId, "STOP_LIVE", "live_plan", livePlanId);
        return updated;
    }

    public LiveSession simulateCallback(String tenantId, String liveSessionId, SimulateLiveCallbackRequest request) {
        LiveSession current = requireOwnedSession(tenantId, liveSessionId);
        LivePlan livePlan = getLivePlan(tenantId, current.livePlanId());
        String normalizedEventType = request.eventType().trim().toLowerCase(Locale.ROOT);
        LiveSession updated;
        LivePlan updatedPlan;
        String auditAction;
        switch (normalizedEventType) {
            case "started", "running" -> {
                updated = ensureSessionRunning(current);
                updatedPlan = livePlan.withStatus("running");
                auditAction = "SIMULATE_LIVE_CALLBACK_STARTED";
            }
            case "paused" -> {
                if (!"running".equals(current.sessionStatus()) && !"paused".equals(current.sessionStatus())) {
                    throw new BusinessException("7105", "live session status does not allow paused callback", HttpStatus.BAD_REQUEST);
                }
                updated = ensureSessionPaused(current);
                updatedPlan = livePlan.withStatus("ready");
                auditAction = "SIMULATE_LIVE_CALLBACK_PAUSED";
            }
            case "resumed" -> {
                if (!"paused".equals(current.sessionStatus()) && !"running".equals(current.sessionStatus())) {
                    throw new BusinessException("7106", "live session status does not allow resumed callback", HttpStatus.BAD_REQUEST);
                }
                updated = ensureSessionRunning(current);
                updatedPlan = livePlan.withStatus("running");
                auditAction = "SIMULATE_LIVE_CALLBACK_RESUMED";
            }
            case "ended", "stopped" -> {
                if (!List.of("running", "paused", "ended").contains(current.sessionStatus())) {
                    throw new BusinessException("7107", "live session status does not allow ended callback", HttpStatus.BAD_REQUEST);
                }
                updated = ensureSessionEnded(current);
                updatedPlan = livePlan.withStatus("ended");
                auditAction = "SIMULATE_LIVE_CALLBACK_ENDED";
            }
            case "failed", "interrupted" -> {
                if (!List.of("running", "paused", "failed").contains(current.sessionStatus())) {
                    throw new BusinessException("7108", "live session status does not allow failed callback", HttpStatus.BAD_REQUEST);
                }
                updated = ensureSessionFailed(current, request.errorMessage());
                updatedPlan = livePlan.withStatus("ready");
                auditAction = "SIMULATE_LIVE_CALLBACK_FAILED";
            }
            default -> throw new BusinessException("7109", "unsupported live callback event type", HttpStatus.BAD_REQUEST);
        }
        LiveSession savedSession = liveSessionRepository.save(updated);
        livePlanRepository.save(updatedPlan);
        auditLogService.recordForTenant(tenantId, auditAction, "live_session", liveSessionId);
        return savedSession;
    }

    public RunDueLivePlansResultView runDueScheduledLives(String tenantId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<LivePlan> duePlans = listLivePlans(tenantId).stream()
                .filter(plan -> "scheduled".equals(plan.planStatus()))
                .filter(plan -> plan.scheduledStartAt() != null)
                .filter(plan -> !plan.scheduledStartAt().isAfter(now))
                .toList();
        List<LiveSession> startedSessions = new ArrayList<>();
        List<DueLivePlanFailureView> failures = new ArrayList<>();
        for (LivePlan duePlan : duePlans) {
            try {
                startedSessions.add(startLive(tenantId, duePlan.livePlanId()));
            } catch (BusinessException ex) {
                failures.add(new DueLivePlanFailureView(
                        duePlan.livePlanId(),
                        duePlan.planName(),
                        ex.getMessage()
                ));
            }
        }
        auditLogService.recordForTenant(tenantId, "RUN_DUE_LIVE_PLANS", "live_plan", "batch");
        return new RunDueLivePlansResultView(
                duePlans.size(),
                startedSessions.size(),
                failures.size(),
                startedSessions,
                failures
        );
    }

    public SkipCurrentProductResultView skipCurrentProduct(String tenantId, String liveSessionId) {
        LiveSession liveSession = requireOwnedSession(tenantId, liveSessionId);
        if (!"running".equals(liveSession.sessionStatus())) {
            throw new BusinessException("7114", "live session status does not allow skip current product", HttpStatus.BAD_REQUEST);
        }
        LivePlan livePlan = getLivePlan(tenantId, liveSession.livePlanId());
        LiveScript activeScript = requireActiveScript(livePlan.livePlanId());
        List<String> segments = splitScriptSegments(activeScript.scriptContent());
        if (segments.size() < 2) {
            throw new BusinessException("7115", "no next script segment to skip to", HttpStatus.BAD_REQUEST);
        }
        liveScriptRepository.save(activeScript.withActive(false));
        String nextContent = String.join(System.lineSeparator(), segments.subList(1, segments.size()));
        String nextProductId = activeScript.productId();
        List<LiveBoundProductItemView> boundItems = listBoundLiveProductsByPlan(livePlan);
        if (!boundItems.isEmpty()) {
            int currentIndex = resolveCurrentBoundItemIndex(requireOwnedStore(tenantId, livePlan.storeId()), livePlan, boundItems, segments.get(0));
            if (currentIndex >= 0 && currentIndex + 1 < boundItems.size()) {
                nextProductId = boundItems.get(currentIndex + 1).productId();
            } else {
                nextProductId = null;
            }
        }
        LiveScript nextScript = liveScriptRepository.save(new LiveScript(
                null,
                livePlan.livePlanId(),
                nextProductId,
                nextScriptVersion(activeScript.scriptVersion()),
                nextContent,
                true,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "SKIP_LIVE_CURRENT_PRODUCT", "live_session", liveSessionId);
        return new SkipCurrentProductResultView(
                liveSessionId,
                livePlan.livePlanId(),
                toSnippet(segments.get(0)),
                toSnippet(segments.get(1)),
                nextScript.scriptVersion()
        );
    }

    public LiveSession strongControl(String tenantId, String liveSessionId, StrongControlRequest request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        LiveSession updated = liveSessionRepository.save(session.withGovernance(
                request.controlMode(),
                defaultIfBlank(session.currentScene(), "default_scene"),
                defaultIfBlank(session.takeoverStatus(), "auto"),
                session.takeoverOperator(),
                defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"),
                session.promiseAuditRemark()
        ));
        auditLogService.recordForTenant(tenantId, "SET_LIVE_STRONG_CONTROL", "live_session", liveSessionId);
        return updated;
    }

    public LiveControlModeOperationView enableControlMode(String tenantId,
                                                          String liveSessionId,
                                                          EnableControlModeCommand request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        String normalizedControlMode = normalizeDocumentedControlMode(request.controlMode());
        liveSessionRepository.save(session.withGovernance(
                normalizedControlMode,
                defaultIfBlank(session.currentScene(), "default_scene"),
                defaultIfBlank(session.takeoverStatus(), "auto"),
                session.takeoverOperator(),
                defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"),
                session.promiseAuditRemark()
        ));
        auditLogService.recordForTenant(tenantId, "ENABLE_LIVE_CONTROL_MODE", "live_session", liveSessionId);
        return new LiveControlModeOperationView(liveSessionId, normalizedControlMode, OffsetDateTime.now());
    }

    public LiveSession switchScene(String tenantId, String liveSessionId, SwitchSceneRequest request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        LiveSession updated = liveSessionRepository.save(session.withGovernance(
                defaultIfBlank(session.controlMode(), "standard"),
                request.targetScene(),
                defaultIfBlank(session.takeoverStatus(), "auto"),
                session.takeoverOperator(),
                defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"),
                session.promiseAuditRemark()
        ));
        auditLogService.recordForTenant(tenantId, "SWITCH_LIVE_SCENE", "live_session", liveSessionId);
        return updated;
    }

    public LiveSession manualTakeover(String tenantId, String liveSessionId, ManualTakeoverRequest request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        LiveSession updated = liveSessionRepository.save(session.withGovernance(
                defaultIfBlank(session.controlMode(), "standard"),
                defaultIfBlank(session.currentScene(), "default_scene"),
                "manual",
                request.takeoverOperator(),
                defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"),
                session.promiseAuditRemark()
        ));
        auditLogService.recordForTenant(tenantId, "MANUAL_TAKEOVER_LIVE", "live_session", liveSessionId);
        return updated;
    }

    public LiveControlModeOperationView resumeSystemMode(String tenantId, String liveSessionId) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        liveSessionRepository.save(session.withGovernance(
                "standard",
                defaultIfBlank(session.currentScene(), "default_scene"),
                "auto",
                null,
                defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"),
                session.promiseAuditRemark()
        ));
        auditLogService.recordForTenant(tenantId, "RESUME_LIVE_SYSTEM_MODE", "live_session", liveSessionId);
        return new LiveControlModeOperationView(liveSessionId, "standard", OffsetDateTime.now());
    }

    public LiveInteractionTransferOperationView transferInteraction(String tenantId,
                                                                    String liveSessionId,
                                                                    InteractionTransferCommand request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        liveSessionRepository.save(session.withGovernance(
                defaultIfBlank(session.controlMode(), "standard"),
                defaultIfBlank(session.currentScene(), "default_scene"),
                "manual",
                request.targetUserId(),
                defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"),
                session.promiseAuditRemark()
        ));
        auditLogService.recordForTenant(tenantId, "TRANSFER_LIVE_INTERACTION", "live_session", liveSessionId);
        return new LiveInteractionTransferOperationView(request.interactionEventId(), "transferred", "assigned");
    }

    public LiveInteractionReplyView replyInteraction(String tenantId,
                                                     String liveSessionId,
                                                     InteractionReplyCommand request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        CommitmentWhitelistConfig config = commitmentWhitelistConfigs.getOrDefault(
                session.livePlanId(),
                CommitmentWhitelistConfig.empty(session.livePlanId())
        );
        CommitmentWhitelistMatch match = matchCommitmentLevel(config, request.customerQuestion());
        String commitmentType = resolveCommitmentType(request.commitmentType(), match.phrase());
        if ("prohibited".equals(match.level())) {
            liveSessionRepository.save(session.withGovernance(
                    defaultIfBlank(session.controlMode(), "standard"),
                    defaultIfBlank(session.currentScene(), "default_scene"),
                    defaultIfBlank(session.takeoverStatus(), "auto"),
                    session.takeoverOperator(),
                    "rejected",
                    "blocked phrase: " + match.phrase()
            ));
            auditLogService.recordForTenant(tenantId, "BLOCK_LIVE_INTERACTION_REPLY", "live_session", liveSessionId);
            throw new BusinessException(
                    "7106",
                    "commitment blocked",
                    Map.of(
                            "commitmentType", commitmentType,
                            "commitmentLevel", "prohibited",
                            "reason", "blocked phrase: " + match.phrase()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }
        if ("manual_confirm".equals(match.level())) {
            String approvalId = generateGovernanceOperationId("approval", liveSessionId);
            liveSessionRepository.save(session.withGovernance(
                    defaultIfBlank(session.controlMode(), "standard"),
                    defaultIfBlank(session.currentScene(), "default_scene"),
                    defaultIfBlank(session.takeoverStatus(), "auto"),
                    session.takeoverOperator(),
                    "pending_confirm",
                    approvalId
            ));
            auditLogService.recordForTenant(tenantId, "REQUIRE_LIVE_COMMITMENT_APPROVAL", "live_session", liveSessionId);
            throw new BusinessException(
                    "7107",
                    "commitment approval required",
                    Map.of(
                            "commitmentType", commitmentType,
                            "commitmentLevel", "manual_confirm",
                            "approvalId", approvalId
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }
        auditLogService.recordForTenant(tenantId, "AUTO_REPLY_LIVE_INTERACTION", "live_session", liveSessionId);
        return new LiveInteractionReplyView(
                request.interactionEventId(),
                "auto_replied",
                defaultIfBlank(request.draftReplyText(), "已根据知识库自动回复：" + request.customerQuestion()),
                "white_list".equals(match.level()) ? "auto_allowed" : null,
                defaultIfBlank(request.knowledgeSourceSummary(), "live_knowledge_base")
        );
    }

    public LiveSession promiseAudit(String tenantId, String liveSessionId, PromiseAuditRequest request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        LiveSession updated = liveSessionRepository.save(session.withGovernance(
                defaultIfBlank(session.controlMode(), "standard"),
                defaultIfBlank(session.currentScene(), "default_scene"),
                defaultIfBlank(session.takeoverStatus(), "auto"),
                session.takeoverOperator(),
                request.decision(),
                request.remark()
        ));
        auditLogService.recordForTenant(tenantId, "AUDIT_LIVE_PROMISE", "live_session", liveSessionId);
        return updated;
    }

    public LiveCommitmentAuditOperationView confirmCommitment(String tenantId,
                                                              String liveSessionId,
                                                              CommitmentDecisionCommand request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        String auditId = generateGovernanceOperationId("commitment", liveSessionId);
        liveSessionRepository.save(session.withGovernance(
                defaultIfBlank(session.controlMode(), "standard"),
                defaultIfBlank(session.currentScene(), "default_scene"),
                defaultIfBlank(session.takeoverStatus(), "auto"),
                session.takeoverOperator(),
                "approved",
                defaultIfBlank(request.commitmentText(), request.approvalId())
        ));
        auditLogService.recordForTenant(tenantId, "CONFIRM_LIVE_COMMITMENT", "live_session", liveSessionId);
        return new LiveCommitmentAuditOperationView(
                auditId,
                "approved",
                !"prohibited".equals(defaultIfBlank(request.commitmentLevel(), "").toLowerCase(Locale.ROOT))
        );
    }

    public LiveCommitmentAuditOperationView rejectCommitment(String tenantId,
                                                             String liveSessionId,
                                                             CommitmentDecisionCommand request) {
        LiveSession session = requireGovernableSession(tenantId, liveSessionId);
        String auditId = generateGovernanceOperationId("commitment", liveSessionId);
        liveSessionRepository.save(session.withGovernance(
                defaultIfBlank(session.controlMode(), "standard"),
                defaultIfBlank(session.currentScene(), "default_scene"),
                defaultIfBlank(session.takeoverStatus(), "auto"),
                session.takeoverOperator(),
                "rejected",
                defaultIfBlank(request.remark(), request.commitmentText())
        ));
        auditLogService.recordForTenant(tenantId, "REJECT_LIVE_COMMITMENT", "live_session", liveSessionId);
        return new LiveCommitmentAuditOperationView(auditId, "rejected", false);
    }

    public LiveSession createRunningSessionSeed(String tenantId, String livePlanId, String liveAccountId) {
        LivePlan livePlan = getLivePlan(tenantId, livePlanId);
        requireOwnedChannelAccount(tenantId, liveAccountId);
        return liveSessionRepository.save(new LiveSession(
                null,
                livePlanId,
                tenantId,
                livePlan.storeId(),
                liveAccountId,
                "running",
                "room-" + livePlanId,
                OffsetDateTime.now(),
                null,
                null,
                "standard",
                "default_scene",
                "auto",
                null,
                "not_reviewed",
                null,
                OffsetDateTime.now()
        ));
    }

    public record EnableControlModeCommand(
            String controlMode,
            String operatorId,
            String reason
    ) {
    }

    public record ConfigureCommitmentWhitelistCommand(
            List<String> whitelistPhrases,
            List<String> manualConfirmPhrases,
            List<String> prohibitedPhrases
    ) {
    }

    public record LiveCommitmentWhitelistConfigView(
            String livePlanId,
            List<String> whitelistPhrases,
            List<String> manualConfirmPhrases,
            List<String> prohibitedPhrases,
            OffsetDateTime updatedAt
    ) {
    }

    public record LiveControlModeOperationView(
            String liveSessionId,
            String controlMode,
            OffsetDateTime effectiveAt
    ) {
    }

    public record InteractionTransferCommand(
            String interactionEventId,
            String transferType,
            String targetUserId,
            String transferReason
    ) {
    }

    public record LiveInteractionTransferOperationView(
            String interactionEventId,
            String replyStatus,
            String transferStatus
    ) {
    }

    public record InteractionReplyCommand(
            String interactionEventId,
            String customerQuestion,
            String draftReplyText,
            String knowledgeSourceSummary,
            String commitmentType
    ) {
    }

    public record LiveInteractionReplyView(
            String interactionEventId,
            String replyStatus,
            String replyText,
            String commitmentAuditStatus,
            String knowledgeSourceSummary
    ) {
    }

    public record CommitmentDecisionCommand(
            String interactionEventId,
            String commitmentType,
            String commitmentLevel,
            String commitmentText,
            String approvedBy,
            String approvalId,
            String remark
    ) {
    }

    public record LiveCommitmentAuditOperationView(
            String commitmentAuditId,
            String auditStatus,
            boolean fulfillmentTrackingRequired
    ) {
    }

    private List<LiveConcurrencyQueueView> buildConcurrencyQueueViews(String tenantId) {
        List<LivePlan> candidatePlans = listLivePlans(tenantId).stream()
                .filter(plan -> List.of("ready", "scheduled").contains(plan.planStatus()))
                .toList();
        List<LiveSession> runningSessions = listLiveSessions(tenantId).stream()
                .filter(session -> "running".equals(session.sessionStatus()))
                .toList();
        int tenantQuotaLimit = usageQuotaRepository.findByTenantId(tenantId).stream()
                .filter(quota -> "live_concurrency".equals(quota.quotaCode()))
                .findFirst()
                .map(UsageQuota::quotaLimit)
                .orElse(0);
        Map<String, ChannelAccount> ownedAccountMap = channelAccountRepository
                .findByOrganizationIds(organizationRepository.findByTenantId(tenantId).stream().map(Organization::id).toList())
                .stream()
                .collect(Collectors.toMap(ChannelAccount::channelAccountId, Function.identity(), (left, right) -> left));
        return candidatePlans.stream()
                .map(plan -> buildQueueItem(plan, runningSessions, tenantQuotaLimit, ownedAccountMap))
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    OffsetDateTime leftTime = left.scheduledStartAt() == null ? OffsetDateTime.MAX : left.scheduledStartAt();
                    OffsetDateTime rightTime = right.scheduledStartAt() == null ? OffsetDateTime.MAX : right.scheduledStartAt();
                    return leftTime.compareTo(rightTime);
                })
                .toList();
    }

    private LiveConcurrencyQueueView buildQueueItem(LivePlan plan,
                                                    List<LiveSession> runningSessions,
                                                    int tenantQuotaLimit,
                                                    Map<String, ChannelAccount> ownedAccountMap) {
        RuntimeReadinessSnapshot runtimeSnapshot = buildRuntimeReadinessSnapshot();
        String blockedType = null;
        String blockedReason = null;
        if (plan.liveAccountId() == null || plan.liveAccountId().isBlank() || !ownedAccountMap.containsKey(plan.liveAccountId())) {
            blockedType = "account_missing";
            blockedReason = "live account not configured";
        } else if (!isLiveAccountReady(ownedAccountMap.get(plan.liveAccountId()))) {
            blockedType = "account_auth_invalid";
            blockedReason = "live account authorization invalid";
        } else if (runningSessions.stream().anyMatch(session -> plan.liveAccountId().equals(session.liveAccountId()))) {
            blockedType = "account_occupied";
            blockedReason = "live account occupied";
        } else if (runningSessions.size() >= tenantQuotaLimit) {
            blockedType = "tenant_quota_exceeded";
            blockedReason = "tenant live concurrency quota exceeded";
        } else if (runtimeSnapshot.strictProviderGate() && !runtimeSnapshot.runtimeReady()) {
            blockedType = "runtime_provider_not_ready";
            blockedReason = runtimeSnapshot.readinessMessage();
        } else if (runtimeSnapshot.strictCallbackGate() && !runtimeSnapshot.callbackReady()) {
            blockedType = "callback_not_ready";
            blockedReason = runtimeSnapshot.readinessMessage();
        }
        if (blockedType == null) {
            return null;
        }
        return new LiveConcurrencyQueueView(
                plan.livePlanId(),
                plan.planName(),
                plan.liveAccountId(),
                blockedType,
                blockedReason,
                plan.scheduledStartAt()
        );
    }

    private List<LiveRiskEventView> buildLiveRiskEvents(String tenantId) {
        List<LiveRiskEventView> events = new ArrayList<>();
        for (LiveSession session : listLiveSessions(tenantId)) {
            OffsetDateTime occurredAt = session.actualEndAt() != null
                    ? session.actualEndAt()
                    : session.actualStartAt() != null ? session.actualStartAt() : session.createdAt();
            if (!"standard".equals(defaultIfBlank(session.controlMode(), "standard"))) {
                events.add(new LiveRiskEventView(
                        "live_strong_control_enabled",
                        "medium",
                        session.liveSessionId(),
                        session.livePlanId(),
                        session.liveAccountId(),
                        "control mode=" + session.controlMode(),
                        occurredAt
                ));
            }
            if ("manual".equals(defaultIfBlank(session.takeoverStatus(), "auto"))) {
                events.add(new LiveRiskEventView(
                        "manual_takeover_active",
                        "high",
                        session.liveSessionId(),
                        session.livePlanId(),
                        session.liveAccountId(),
                        "takeover operator=" + defaultIfBlank(session.takeoverOperator(), "unknown"),
                        occurredAt
                ));
            }
            if ("rejected".equals(defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"))) {
                events.add(new LiveRiskEventView(
                        "live_promise_audit_rejected",
                        "high",
                        session.liveSessionId(),
                        session.livePlanId(),
                        session.liveAccountId(),
                        defaultIfBlank(session.promiseAuditRemark(), "promise audit rejected"),
                        occurredAt
                ));
            }
            if ("failed".equals(session.sessionStatus()) || (session.errorMessage() != null && !session.errorMessage().isBlank())) {
                events.add(new LiveRiskEventView(
                        "live_session_failed",
                        "critical",
                        session.liveSessionId(),
                        session.livePlanId(),
                        session.liveAccountId(),
                        defaultIfBlank(session.errorMessage(), "live session failed"),
                        occurredAt
                ));
            }
        }
        events.sort((left, right) -> right.occurredAt().compareTo(left.occurredAt()));
        return events;
    }

    private boolean isLiveAccountReady(ChannelAccount account) {
        String authStatus = defaultIfBlank(account.authStatus(), "unknown").toLowerCase(Locale.ROOT);
        return List.of("connected", "authorized", "active").contains(authStatus);
    }

    private String resolveGovernanceRiskLevel(LiveAccountView account,
                                              int runningSessionCount,
                                              int queuedPlanCount,
                                              boolean expiringSoon) {
        if (!isLiveAccountHealthy(account.authStatus()) || queuedPlanCount > 0 || expiringSoon) {
            return "high";
        }
        if (runningSessionCount > 0) {
            return "medium";
        }
        return "low";
    }

    private boolean isLiveAccountHealthy(String authStatus) {
        String normalized = defaultIfBlank(authStatus, "unknown").toLowerCase(Locale.ROOT);
        return List.of("connected", "authorized", "active").contains(normalized);
    }

    private int riskRank(String riskLevel) {
        return switch (defaultIfBlank(riskLevel, "low")) {
            case "critical" -> 4;
            case "high" -> 3;
            case "medium" -> 2;
            default -> 1;
        };
    }

    public void clear() {
        commitmentWhitelistConfigs.clear();
        liveSessionRepository.deleteAll();
        liveScriptRepository.deleteAll();
        liveProductItemRepository.deleteAll();
        livePlanRepository.deleteAll();
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private ChannelAccount requireOwnedChannelAccount(String tenantId, String channelAccountId) {
        ChannelAccount channelAccount = channelAccountRepository.findByChannelAccountId(channelAccountId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        Organization organization = organizationRepository.findById(channelAccount.organizationId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(organization.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return channelAccount;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private LiveSession requireLatestSession(String livePlanId) {
        return liveSessionRepository.findLatestByLivePlanId(livePlanId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
    }

    private void validateScheduleWindow(OffsetDateTime scheduledStartAt, OffsetDateTime scheduledEndAt) {
        if (scheduledEndAt != null && scheduledEndAt.isBefore(scheduledStartAt)) {
            throw new BusinessException("1002", "scheduled end time must be after start time", HttpStatus.BAD_REQUEST);
        }
    }

    private LiveSession requireOwnedSession(String tenantId, String liveSessionId) {
        return listLiveSessions(tenantId).stream()
                .filter(session -> liveSessionId.equals(session.liveSessionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
    }

    private LiveSession requireGovernableSession(String tenantId, String liveSessionId) {
        LiveSession session = requireOwnedSession(tenantId, liveSessionId);
        if (!List.of("running", "paused").contains(session.sessionStatus())) {
            throw new BusinessException("7126", "live session status does not allow governance operation", HttpStatus.BAD_REQUEST);
        }
        return session;
    }

    private LiveSession ensureSessionRunning(LiveSession session) {
        if ("running".equals(session.sessionStatus()) && session.actualEndAt() == null) {
            return session;
        }
        return new LiveSession(
                session.liveSessionId(),
                session.livePlanId(),
                session.tenantId(),
                session.storeId(),
                session.liveAccountId(),
                "running",
                session.roomId(),
                session.actualStartAt() == null ? OffsetDateTime.now() : session.actualStartAt(),
                null,
                null,
                defaultIfBlank(session.controlMode(), "standard"),
                defaultIfBlank(session.currentScene(), "default_scene"),
                defaultIfBlank(session.takeoverStatus(), "auto"),
                session.takeoverOperator(),
                defaultIfBlank(session.promiseAuditStatus(), "not_reviewed"),
                session.promiseAuditRemark(),
                session.createdAt()
        );
    }

    private LiveSession ensureSessionPaused(LiveSession session) {
        if ("paused".equals(session.sessionStatus())) {
            return session;
        }
        return session.withStatus("paused", null, session.errorMessage());
    }

    private LiveSession ensureSessionEnded(LiveSession session) {
        if ("ended".equals(session.sessionStatus())) {
            return session.actualEndAt() == null
                    ? session.withStatus("ended", OffsetDateTime.now(), session.errorMessage())
                    : session;
        }
        return session.withStatus("ended", OffsetDateTime.now(), session.errorMessage());
    }

    private LiveSession ensureSessionFailed(LiveSession session, String errorMessage) {
        String failureMessage = errorMessage == null || errorMessage.isBlank()
                ? session.errorMessage() == null || session.errorMessage().isBlank()
                    ? "live interrupted by callback"
                    : session.errorMessage()
                : errorMessage;
        if ("failed".equals(session.sessionStatus()) && session.actualEndAt() != null && failureMessage.equals(session.errorMessage())) {
            return session;
        }
        return session.withStatus("failed", OffsetDateTime.now(), failureMessage);
    }

    private long calculateDurationSeconds(LiveSession session) {
        if (session.actualStartAt() == null) {
            return 0L;
        }
        OffsetDateTime endTime = session.actualEndAt() == null ? OffsetDateTime.now() : session.actualEndAt();
        return Math.max(Duration.between(session.actualStartAt(), endTime).getSeconds(), 0L);
    }

    private LiveProductPoolItemView toPoolItem(String storeId,
                                               CandidateProduct candidate,
                                               ProductDraft draft,
                                               Product product) {
        return new LiveProductPoolItemView(
                storeId,
                candidate == null ? null : candidate.candidateProductId(),
                draft == null ? null : draft.productDraftId(),
                product == null ? null : product.productId(),
                product != null ? product.title()
                        : draft != null ? draft.title()
                        : candidate != null ? candidate.title() : null,
                candidate == null ? null : candidate.sourceType(),
                candidate == null ? null : candidate.category(),
                candidate == null ? null : candidate.status(),
                draft == null ? null : draft.status(),
                product == null ? null : product.status(),
                product == null ? null : product.healthScore(),
                candidate == null ? null : candidate.riskLevel(),
                candidate == null ? null : candidate.recommendationReason()
        );
    }

    private int rankPoolItem(LiveProductPoolItemView item) {
        if (item.productId() != null) {
            return 0;
        }
        if (item.productDraftId() != null) {
            return 1;
        }
        return 2;
    }

    private List<LiveBoundProductItemView> listBoundLiveProductsByPlan(LivePlan livePlan) {
        return liveProductItemRepository.findByLivePlanId(livePlan.livePlanId()).stream()
                .map(item -> toBoundItemView(livePlan.storeId(), livePlan.livePlanId(), item))
                .toList();
    }

    private void copyBoundProducts(String sourceLivePlanId, String targetLivePlanId) {
        OffsetDateTime copiedAt = OffsetDateTime.now();
        for (LiveProductItem item : liveProductItemRepository.findByLivePlanId(sourceLivePlanId)) {
            liveProductItemRepository.save(new LiveProductItem(
                    null,
                    targetLivePlanId,
                    item.candidateProductId(),
                    item.productDraftId(),
                    item.productId(),
                    item.sortOrder(),
                    copiedAt
            ));
        }
    }

    private void rebuildActiveScriptForPlanIfPresent(LivePlan livePlan) {
        LiveScript activeScript = liveScriptRepository.findByLivePlanId(livePlan.livePlanId()).stream()
                .filter(LiveScript::active)
                .findFirst()
                .orElse(null);
        if (activeScript == null) {
            return;
        }
        liveScriptRepository.save(activeScript.withActive(false));
        createScriptForPlan(livePlan, nextScriptVersion(activeScript.scriptVersion()));
    }

    private LiveScript createScriptForPlan(LivePlan livePlan, String scriptVersion) {
        Store store = storeRepository.findByStoreId(livePlan.storeId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        String content = """
                Summarize the live performance for store %s, plan %s, and anchor %s. Cover script quality, product binding, conversion readiness, and next improvements.
                """.formatted(
                store.shopName(),
                livePlan.planName(),
                livePlan.anchorProfileName() == null || livePlan.anchorProfileName().isBlank() ? "default anchor" : livePlan.anchorProfileName()).trim();
        List<LiveBoundProductItemView> boundItems = listBoundLiveProductsByPlan(livePlan);
        String activeProductId = null;
        List<String> scriptSegments = buildScriptSegments(store, livePlan, boundItems);
        if (!scriptSegments.isEmpty()) {
            content = String.join(System.lineSeparator(), scriptSegments);
        }
        if (!boundItems.isEmpty()) {
            activeProductId = boundItems.get(0).productId();
        }
        return liveScriptRepository.save(new LiveScript(
                null,
                livePlan.livePlanId(),
                activeProductId,
                scriptVersion,
                content,
                true,
                OffsetDateTime.now()
        ));
    }

    private LiveBoundProductItemView resolveCurrentBoundProduct(LivePlan livePlan, LiveScript activeScript) {
        if (activeScript == null) {
            return null;
        }
        List<LiveBoundProductItemView> boundItems = listBoundLiveProductsByPlan(livePlan);
        if (boundItems.isEmpty()) {
            return null;
        }
        if (activeScript.productId() != null && !activeScript.productId().isBlank()) {
            LiveBoundProductItemView matchedByProductId = boundItems.stream()
                    .filter(item -> Objects.equals(item.productId(), activeScript.productId()))
                    .findFirst()
                    .orElse(null);
            if (matchedByProductId != null) {
                return matchedByProductId;
            }
        }
        List<String> segments = splitScriptSegments(activeScript.scriptContent());
        if (segments.isEmpty()) {
            return null;
        }
        Store store = storeRepository.findByStoreId(livePlan.storeId()).orElse(null);
        if (store == null) {
            return null;
        }
        int currentIndex = resolveCurrentBoundItemIndex(store, livePlan, boundItems, segments.get(0));
        if (currentIndex < 0 || currentIndex >= boundItems.size()) {
            return null;
        }
        return boundItems.get(currentIndex);
    }

    private LiveBoundProductItemView toBoundItemView(String storeId, String livePlanId, LiveProductItem liveProductItem) {
        CandidateProduct candidate = liveProductItem.candidateProductId() == null
                ? null
                : candidateProductRepository.findByCandidateProductId(liveProductItem.candidateProductId()).orElse(null);
        ProductDraft draft = liveProductItem.productDraftId() == null
                ? null
                : productDraftRepository.findByProductDraftId(liveProductItem.productDraftId()).orElse(null);
        Product product = liveProductItem.productId() == null
                ? null
                : productRepository.findByProductId(liveProductItem.productId()).orElse(null);
        LiveProductPoolItemView poolItem = toPoolItem(storeId, candidate, draft, product);
        return new LiveBoundProductItemView(
                liveProductItem.liveProductItemId(),
                livePlanId,
                liveProductItem.sortOrder(),
                liveProductItem.createdAt(),
                poolItem.storeId(),
                poolItem.candidateProductId(),
                poolItem.productDraftId(),
                poolItem.productId(),
                poolItem.title(),
                poolItem.sourceType(),
                poolItem.category(),
                poolItem.candidateStatus(),
                poolItem.draftStatus(),
                poolItem.productStatus(),
                poolItem.healthScore(),
                poolItem.riskLevel(),
                poolItem.recommendationReason()
        );
    }

    private ResolvedLiveProductSource resolveLiveProductSource(LivePlan livePlan, BindLiveProductRequest request) {
        int sourceCount = countNonBlank(request.productId(), request.productDraftId(), request.candidateProductId());
        if (sourceCount != 1) {
            throw new BusinessException("7122", "exactly one live product source is required", HttpStatus.BAD_REQUEST);
        }
        if (request.productId() != null && !request.productId().isBlank()) {
            Product product = productRepository.findByProductId(request.productId())
                    .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
            ensureProductOwnedByPlanStore(livePlan, product.storeId());
            ProductDraft draft = product.productDraftId() == null || product.productDraftId().isBlank()
                    ? null
                    : productDraftRepository.findByProductDraftId(product.productDraftId()).orElse(null);
            CandidateProduct candidate = draft == null || draft.candidateProductId() == null || draft.candidateProductId().isBlank()
                    ? null
                    : candidateProductRepository.findByCandidateProductId(draft.candidateProductId()).orElse(null);
            return new ResolvedLiveProductSource(
                    candidate == null ? null : candidate.candidateProductId(),
                    draft == null ? null : draft.productDraftId(),
                    product.productId()
            );
        }
        if (request.productDraftId() != null && !request.productDraftId().isBlank()) {
            ProductDraft draft = productDraftRepository.findByProductDraftId(request.productDraftId())
                    .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
            ensureProductOwnedByPlanStore(livePlan, draft.storeId());
            CandidateProduct candidate = draft.candidateProductId() == null || draft.candidateProductId().isBlank()
                    ? null
                    : candidateProductRepository.findByCandidateProductId(draft.candidateProductId()).orElse(null);
            Product product = productRepository.findByStoreIds(List.of(livePlan.storeId())).stream()
                    .filter(item -> Objects.equals(item.productDraftId(), draft.productDraftId()))
                    .findFirst()
                    .orElse(null);
            return new ResolvedLiveProductSource(
                    candidate == null ? null : candidate.candidateProductId(),
                    draft.productDraftId(),
                    product == null ? null : product.productId()
            );
        }
        CandidateProduct candidate = candidateProductRepository.findByCandidateProductId(request.candidateProductId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        ensureProductOwnedByPlanStore(livePlan, candidate.storeId());
        ProductDraft draft = productDraftRepository.findByStoreIds(List.of(livePlan.storeId())).stream()
                .filter(item -> Objects.equals(item.candidateProductId(), candidate.candidateProductId()))
                .findFirst()
                .orElse(null);
        Product product = draft == null ? null : productRepository.findByStoreIds(List.of(livePlan.storeId())).stream()
                .filter(item -> Objects.equals(item.productDraftId(), draft.productDraftId()))
                .findFirst()
                .orElse(null);
        return new ResolvedLiveProductSource(
                candidate.candidateProductId(),
                draft == null ? null : draft.productDraftId(),
                product == null ? null : product.productId()
        );
    }

    private Optional<LiveProductItem> findExistingBoundItem(List<LiveProductItem> existingItems,
                                                            ResolvedLiveProductSource resolved) {
        return existingItems.stream()
                .filter(item -> sameSource(item.productId(), resolved.productId())
                        || sameSource(item.productDraftId(), resolved.productDraftId())
                        || sameSource(item.candidateProductId(), resolved.candidateProductId()))
                .findFirst();
    }

    private boolean sameSource(String left, String right) {
        return left != null && right != null && left.equals(right);
    }

    private int nextSortOrder(List<LiveProductItem> existingItems) {
        return existingItems.stream()
                .mapToInt(LiveProductItem::sortOrder)
                .max()
                .orElse(0) + 10;
    }

    private List<String> buildBoundProductScriptSegments(Store store,
                                                         LivePlan livePlan,
                                                         List<LiveBoundProductItemView> boundItems) {
        String anchorName = livePlan.anchorProfileName() == null || livePlan.anchorProfileName().isBlank()
                ? "default-anchor"
                : livePlan.anchorProfileName();
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < boundItems.size(); i++) {
            LiveBoundProductItemView item = boundItems.get(i);
            String title = item.title() == null || item.title().isBlank() ? "unnamed-product" : item.title();
            String category = item.category() == null || item.category().isBlank() ? "uncategorized" : item.category();
            String sourceType = item.sourceType() == null || item.sourceType().isBlank() ? "manual" : item.sourceType();
            segments.add(
                    "Segment %02d | Plan: %s | Product: %s | Anchor: %s | Store: %s | Category: %s | Source: %s"
                            .formatted(i + 1, livePlan.planName(), title, anchorName, store.shopName(), category, sourceType)
            );
        }
        return segments;
    }

    private List<String> buildScriptSegments(Store store,
                                             LivePlan livePlan,
                                             List<LiveBoundProductItemView> boundItems) {
        if (!boundItems.isEmpty()) {
            return buildBoundProductScriptSegments(store, livePlan, boundItems);
        }
        return buildDefaultScriptSegments(store, livePlan);
    }

    private List<String> buildDefaultScriptSegments(Store store, LivePlan livePlan) {
        String anchorName = livePlan.anchorProfileName() == null || livePlan.anchorProfileName().isBlank()
                ? "default-anchor"
                : livePlan.anchorProfileName();
        String planName = livePlan.planName() == null || livePlan.planName().isBlank()
                ? "unnamed-live-plan"
                : livePlan.planName();
        return List.of(
                "Segment 01 | Plan: %s | Anchor: %s | Focus: opening welcome and trust warmup".formatted(planName, anchorName),
                "Segment 02 | Plan: %s | Anchor: %s | Focus: audience interaction and benefit teaser".formatted(planName, anchorName),
                "Segment 03 | Plan: %s | Anchor: %s | Focus: conversion prompt and offer explanation".formatted(planName, anchorName),
                "Segment 04 | Plan: %s | Anchor: %s | Focus: closing recap and follow-up retention".formatted(planName, anchorName)
        );
    }

    private int resolveCurrentBoundItemIndex(Store store,
                                             LivePlan livePlan,
                                             List<LiveBoundProductItemView> boundItems,
                                             String currentSegment) {
        List<String> generatedSegments = buildBoundProductScriptSegments(store, livePlan, boundItems);
        for (int i = 0; i < generatedSegments.size(); i++) {
            if (Objects.equals(generatedSegments.get(i), currentSegment)) {
                return i;
            }
        }
        return -1;
    }

    private int countNonBlank(String... values) {
        int count = 0;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private void ensureProductOwnedByPlanStore(LivePlan livePlan, String storeId) {
        if (!Objects.equals(livePlan.storeId(), storeId)) {
            throw new BusinessException("7123", "live product source does not belong to plan store", HttpStatus.BAD_REQUEST);
        }
    }

    private LiveScript requireActiveScript(String livePlanId) {
        return liveScriptRepository.findByLivePlanId(livePlanId).stream()
                .filter(LiveScript::active)
                .findFirst()
                .orElseThrow(() -> new BusinessException("7116", "active live script not found", HttpStatus.BAD_REQUEST));
    }

    private List<String> splitScriptSegments(String scriptContent) {
        return scriptContent == null ? List.of() : scriptContent.lines()
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .filter(segment -> !Objects.equals(segment, "\uFEFF"))
                .toList();
    }

    private String nextScriptVersion(String scriptVersion) {
        if (scriptVersion != null && scriptVersion.matches("v\\d+")) {
            int current = Integer.parseInt(scriptVersion.substring(1));
            return "v" + (current + 1);
        }
        return "v-next";
    }

    private String normalizeDocumentedControlMode(String controlMode) {
        String normalized = defaultIfBlank(controlMode, "").trim().toLowerCase(Locale.ROOT);
        if (!List.of("standard", "enhanced", "manual").contains(normalized)) {
            throw new BusinessException("7127", "unsupported control mode", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private List<String> normalizePhraseList(List<String> phrases) {
        if (phrases == null || phrases.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(phrases.stream()
                .map(this::normalizeReplyText)
                .filter(phrase -> !phrase.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private String normalizeReplyText(String text) {
        return defaultIfBlank(text, "").trim().toLowerCase(Locale.ROOT);
    }

    private CommitmentWhitelistMatch matchCommitmentLevel(CommitmentWhitelistConfig config, String customerQuestion) {
        String normalizedQuestion = normalizeReplyText(customerQuestion);
        Optional<String> prohibited = config.prohibitedPhrases().stream()
                .filter(normalizedQuestion::contains)
                .findFirst();
        if (prohibited.isPresent()) {
            return new CommitmentWhitelistMatch("prohibited", prohibited.get());
        }
        Optional<String> manualConfirm = config.manualConfirmPhrases().stream()
                .filter(normalizedQuestion::contains)
                .findFirst();
        if (manualConfirm.isPresent()) {
            return new CommitmentWhitelistMatch("manual_confirm", manualConfirm.get());
        }
        Optional<String> whiteList = config.whitelistPhrases().stream()
                .filter(normalizedQuestion::contains)
                .findFirst();
        if (whiteList.isPresent()) {
            return new CommitmentWhitelistMatch("white_list", whiteList.get());
        }
        return CommitmentWhitelistMatch.none();
    }

    private String resolveCommitmentType(String commitmentType, String matchedPhrase) {
        String normalizedType = defaultIfBlank(commitmentType, "").trim();
        if (!normalizedType.isBlank()) {
            return normalizedType;
        }
        if (matchedPhrase == null || matchedPhrase.isBlank()) {
            return "general_consultation";
        }
        String normalizedPhrase = matchedPhrase.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return normalizedPhrase.isBlank() ? "general_consultation" : normalizedPhrase;
    }

    private String generateGovernanceOperationId(String prefix, String liveSessionId) {
        return prefix + "-" + liveSessionId + "-" + System.currentTimeMillis();
    }

    private String toSnippet(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.length() <= 120 ? content : content.substring(0, 120);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private RuntimeReadinessSnapshot buildRuntimeReadinessSnapshot() {
        boolean strictProviderGate = environment.getProperty("app.live.runtime.strict-provider-gate", Boolean.class, false);
        boolean strictCallbackGate = environment.getProperty("app.live.runtime.strict-callback-gate", Boolean.class, false);
        boolean providerTokenConfigured = environment.getProperty("app.live.runtime.provider-token-configured", Boolean.class, false);
        boolean callbackSignatureConfigured = environment.getProperty("app.live.runtime.callback-signature-configured", Boolean.class, false);
        boolean allowMockRoomId = environment.getProperty("app.live.runtime.allow-mock-room-id", Boolean.class, true);
        String providerName = defaultIfBlank(environment.getProperty("app.live.runtime.provider-name"), "mock-live-runtime");
        String providerEndpoint = environment.getProperty("app.live.runtime.provider-endpoint");
        String callbackEndpoint = environment.getProperty("app.live.runtime.callback-endpoint");
        boolean runtimeReady = providerEndpoint != null && !providerEndpoint.isBlank() && providerTokenConfigured;
        boolean callbackReady = callbackEndpoint != null && !callbackEndpoint.isBlank() && callbackSignatureConfigured;
        List<String> issues = new ArrayList<>();
        if (!runtimeReady) {
            issues.add("provider endpoint/token incomplete");
        }
        if (!callbackReady) {
            issues.add("callback endpoint/signature incomplete");
        }
        String readinessMessage = issues.isEmpty()
                ? "runtime ready"
                : String.join("; ", issues);
        return new RuntimeReadinessSnapshot(
                strictProviderGate,
                strictCallbackGate,
                runtimeReady,
                callbackReady,
                allowMockRoomId,
                providerName,
                providerEndpoint,
                callbackEndpoint,
                readinessMessage
        );
    }

    private record ResolvedLiveProductSource(
            String candidateProductId,
            String productDraftId,
            String productId
    ) {
    }

    private record RuntimeReadinessSnapshot(
            boolean strictProviderGate,
            boolean strictCallbackGate,
            boolean runtimeReady,
            boolean callbackReady,
            boolean allowMockRoomId,
            String providerName,
            String providerEndpoint,
            String callbackEndpoint,
            String readinessMessage
    ) {
    }

    private record CommitmentWhitelistConfig(
            String livePlanId,
            List<String> whitelistPhrases,
            List<String> manualConfirmPhrases,
            List<String> prohibitedPhrases,
            OffsetDateTime updatedAt
    ) {
        private static CommitmentWhitelistConfig empty(String livePlanId) {
            return new CommitmentWhitelistConfig(livePlanId, List.of(), List.of(), List.of(), null);
        }
    }

    private record CommitmentWhitelistMatch(String level, String phrase) {
        private static CommitmentWhitelistMatch none() {
            return new CommitmentWhitelistMatch("none", null);
        }
    }
}
