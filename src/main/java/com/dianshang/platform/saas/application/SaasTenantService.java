package com.dianshang.platform.saas.application;

import com.dianshang.platform.admin.dto.AdminTenantOverview;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.AuthSecurityConfigVerifier;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.auth.AuthBootstrapService;
import com.dianshang.platform.auth.AuthUserProvisioningService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.inventory.domain.repository.InventorySnapshotRepository;
import com.dianshang.platform.inventory.domain.repository.ReplenishmentTaskRepository;
import com.dianshang.platform.member.domain.repository.MemberProfileRepository;
import com.dianshang.platform.member.domain.repository.MemberTagRepository;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.organization.model.Organization;
import com.dianshang.platform.order.domain.repository.OrderItemRepository;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.model.OrderMain;
import com.dianshang.platform.product.domain.repository.ProductRepository;
import com.dianshang.platform.saas.domain.repository.ComplianceAcceptanceRepository;
import com.dianshang.platform.saas.domain.repository.SubscriptionPlanRepository;
import com.dianshang.platform.saas.domain.repository.TenantCleanupTaskRepository;
import com.dianshang.platform.saas.domain.repository.TenantDataExportTaskRepository;
import com.dianshang.platform.saas.domain.repository.TenantProfileRepository;
import com.dianshang.platform.saas.domain.repository.TenantSubscriptionRepository;
import com.dianshang.platform.saas.domain.repository.UsageQuotaRepository;
import com.dianshang.platform.saas.domain.repository.BillingOrderRepository;
import com.dianshang.platform.saas.domain.repository.InvoiceRequestRepository;
import com.dianshang.platform.saas.dto.QuotaConsumeRequest;
import com.dianshang.platform.saas.dto.RegisterTenantRequest;
import com.dianshang.platform.saas.dto.RegisterTenantResponse;
import com.dianshang.platform.saas.model.BillingOrder;
import com.dianshang.platform.saas.model.ComplianceAcceptanceRecord;
import com.dianshang.platform.saas.model.InvoiceRequest;
import com.dianshang.platform.saas.model.SubscriptionPlan;
import com.dianshang.platform.saas.model.TenantCleanupTaskRecord;
import com.dianshang.platform.saas.model.TenantDataExportTaskRecord;
import com.dianshang.platform.saas.model.TenantProfile;
import com.dianshang.platform.saas.model.TenantSubscription;
import com.dianshang.platform.saas.model.UsageQuota;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import com.dianshang.platform.tenant.TenantContext;
import com.dianshang.platform.tenant.TenantContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class SaasTenantService {
    private static final int DEFAULT_ACCEPTANCE_EVIDENCE_MAX_AGE_DAYS = 7;
    private static final String ACCEPTANCE_STATUS_ACCEPTED_CURRENT_VERSION = "accepted_current_version";
    private static final String ACCEPTANCE_STATUS_PENDING_REACCEPTANCE = "pending_reacceptance";
    private static final String ACCEPTANCE_STATUS_NEVER_ACCEPTED = "never_accepted";
    private static final String CLEANUP_SCOPE_ORDERS = "orders";
    private static final String CLEANUP_SCOPE_PRODUCTS = "products";
    private static final String CLEANUP_SCOPE_INVENTORY = "inventory";
    private static final String CLEANUP_SCOPE_MEMBERS = "members";
    private static final List<String> DEFAULT_CLEANUP_SCOPES = List.of(
            CLEANUP_SCOPE_ORDERS,
            CLEANUP_SCOPE_PRODUCTS,
            CLEANUP_SCOPE_INVENTORY,
            CLEANUP_SCOPE_MEMBERS
    );
    private static final Set<String> SENSITIVE_COMPLIANCE_PERMISSION_CODES = Set.of(
            AuthPermissionCodes.TENANT_AUDIT_EXPORT,
            AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE,
            AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE,
            AuthPermissionCodes.OPENPLATFORM_MANAGE
    );

    private final AtomicLong tenantSequence = new AtomicLong(1000);
    private final AtomicLong subscriptionSequence = new AtomicLong(20000);
    private final Map<String, ComplianceDocument> complianceDocuments = new LinkedHashMap<>();
    private final OrganizationService organizationService;
    private final AuditLogService auditLogService;
    private final TenantProfileRepository tenantProfileRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final UsageQuotaRepository usageQuotaRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final BillingOrderRepository billingOrderRepository;
    private final InvoiceRequestRepository invoiceRequestRepository;
    private final TenantCleanupTaskRepository tenantCleanupTaskRepository;
    private final TenantDataExportTaskRepository tenantDataExportTaskRepository;
    private final ComplianceAcceptanceRepository complianceAcceptanceRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ReplenishmentTaskRepository replenishmentTaskRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberTagRepository memberTagRepository;
    private final AuthUserProvisioningService authUserProvisioningService;
    private final AuthBootstrapService authBootstrapService;
    private final AuthSecurityConfigVerifier authSecurityConfigVerifier;
    private final int releaseAutomationEvidenceSuiteCount;
    private final String releaseAutomationEvidenceEnvironment;
    private final String releaseAutomationEvidenceExecutedAt;
    private final String releaseAutomationEvidenceSummary;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public SaasTenantService(OrganizationService organizationService,
                             AuditLogService auditLogService,
                             TenantProfileRepository tenantProfileRepository,
                             TenantSubscriptionRepository tenantSubscriptionRepository,
                             UsageQuotaRepository usageQuotaRepository,
                             SubscriptionPlanRepository subscriptionPlanRepository,
                             BillingOrderRepository billingOrderRepository,
                             InvoiceRequestRepository invoiceRequestRepository,
                             TenantCleanupTaskRepository tenantCleanupTaskRepository,
                             TenantDataExportTaskRepository tenantDataExportTaskRepository,
                             ComplianceAcceptanceRepository complianceAcceptanceRepository,
                             StoreRepository storeRepository,
                             ProductRepository productRepository,
                             InventorySnapshotRepository inventorySnapshotRepository,
                             ReplenishmentTaskRepository replenishmentTaskRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             MemberProfileRepository memberProfileRepository,
                             MemberTagRepository memberTagRepository,
                             AuthUserProvisioningService authUserProvisioningService,
                             AuthBootstrapService authBootstrapService,
                             AuthSecurityConfigVerifier authSecurityConfigVerifier,
                             @Value("${app.release.automation-evidence.suite-count:0}") int releaseAutomationEvidenceSuiteCount,
                             @Value("${app.release.automation-evidence.environment:}") String releaseAutomationEvidenceEnvironment,
                             @Value("${app.release.automation-evidence.executed-at:}") String releaseAutomationEvidenceExecutedAt,
                             @Value("${app.release.automation-evidence.summary:}") String releaseAutomationEvidenceSummary,
                             ObjectMapper objectMapper,
                             JdbcTemplate jdbcTemplate,
                             Environment environment) {
        this.organizationService = organizationService;
        this.auditLogService = auditLogService;
        this.tenantProfileRepository = tenantProfileRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.usageQuotaRepository = usageQuotaRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.billingOrderRepository = billingOrderRepository;
        this.invoiceRequestRepository = invoiceRequestRepository;
        this.tenantCleanupTaskRepository = tenantCleanupTaskRepository;
        this.tenantDataExportTaskRepository = tenantDataExportTaskRepository;
        this.complianceAcceptanceRepository = complianceAcceptanceRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.replenishmentTaskRepository = replenishmentTaskRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.memberTagRepository = memberTagRepository;
        this.authUserProvisioningService = authUserProvisioningService;
        this.authBootstrapService = authBootstrapService;
        this.authSecurityConfigVerifier = authSecurityConfigVerifier;
        this.releaseAutomationEvidenceSuiteCount = releaseAutomationEvidenceSuiteCount;
        this.releaseAutomationEvidenceEnvironment = releaseAutomationEvidenceEnvironment;
        this.releaseAutomationEvidenceExecutedAt = releaseAutomationEvidenceExecutedAt;
        this.releaseAutomationEvidenceSummary = releaseAutomationEvidenceSummary;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        seedSubscriptionPlans();
        seedComplianceDocuments();
    }

    public RegisterTenantResponse register(RegisterTenantRequest request) {
        long nextTenantNumber = tenantSequence.incrementAndGet();
        String tenantId = "tenant-" + nextTenantNumber;
        String tenantCode = "tenant_" + nextTenantNumber;
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime trialEndAt = now.plusDays(14);

        Organization defaultOrganization = organizationService.createDefaultOrganization(
                tenantId,
                request.ownerName(),
                request.mobile()
        );

        TenantProfile profile = new TenantProfile(
                tenantId,
                tenantCode,
                request.tenantName(),
                "trial",
                request.ownerName(),
                request.mobile(),
                defaultOrganization.id(),
                Map.of(),
                trialEndAt,
                now
        );
        tenantProfileRepository.save(profile);

        SubscriptionPlan trialPlan = requirePlan("trial");
        TenantSubscription subscription = new TenantSubscription(
                "sub-" + subscriptionSequence.incrementAndGet(),
                tenantId,
                trialPlan.planCode(),
                trialPlan.planName(),
                "trialing",
                now,
                trialEndAt,
                1,
                false
        );
        tenantSubscriptionRepository.save(subscription);

        usageQuotaRepository.saveAll(tenantId, defaultQuotas(tenantId));
        authUserProvisioningService.provisionTenantOwner(
                tenantId,
                defaultOrganization.id(),
                request.ownerName(),
                request.mobile()
        );

        auditLogService.record("REGISTER_TENANT", "tenant", tenantId);

        return new RegisterTenantResponse(
                tenantId,
                tenantCode,
                profile.tenantName(),
                profile.tenantStatus(),
                profile.defaultOrganizationId(),
                trialEndAt
        );
    }

    public List<SubscriptionPlan> getSubscriptionPlans() {
        return subscriptionPlanRepository.findAll();
    }

    public TenantProfile startTrial(String tenantId) {
        TenantProfile profile = requireTenantProfile(tenantId);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime trialEndAt = now.plusDays(14);
        tenantProfileRepository.save(profile.withStatusAndTrialEndAt("trial", trialEndAt));
        TenantSubscription existing = tenantSubscriptionRepository.findByTenantId(tenantId).orElse(null);
        tenantSubscriptionRepository.save(new TenantSubscription(
                existing == null ? "sub-" + subscriptionSequence.incrementAndGet() : existing.subscriptionId(),
                tenantId,
                "trial",
                requirePlan("trial").planName(),
                "trialing",
                now,
                trialEndAt,
                1,
                false
        ));
        usageQuotaRepository.saveAll(tenantId, defaultQuotas(tenantId));
        auditLogService.recordForTenant(tenantId, "START_TRIAL", "tenant", tenantId);
        return requireTenantProfile(tenantId);
    }

    public TenantSubscription getSubscription(String tenantId) {
        requireTenantProfile(tenantId);
        return requireSubscription(tenantId);
    }

    public TenantSubscription subscribe(String tenantId, SubscriptionActionRequest request) {
        TenantProfile profile = requireTenantProfile(tenantId);
        SubscriptionPlan plan = requirePaidPlan(request.planCode());
        if (request.seatCount() > plan.seatLimit()) {
            throw new BusinessException(
                    "6010",
                    "seat count exceeds plan default limit",
                    Map.of(
                            "seatCount", request.seatCount(),
                            "planCode", plan.planCode(),
                            "planSeatLimit", plan.seatLimit()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }
        TenantSubscription current = tenantSubscriptionRepository.findByTenantId(tenantId).orElse(null);
        OffsetDateTime now = OffsetDateTime.now();
        TenantSubscription updated = new TenantSubscription(
                current == null ? "sub-" + subscriptionSequence.incrementAndGet() : current.subscriptionId(),
                tenantId,
                plan.planCode(),
                plan.planName(),
                "active",
                now,
                now.plusMonths(1),
                request.seatCount(),
                request.autoRenew() != null && request.autoRenew()
        );
        tenantSubscriptionRepository.save(updated);
        usageQuotaRepository.saveAll(tenantId, buildQuotas(tenantId, plan.planCode(), request.seatCount()));
        tenantProfileRepository.save(profile.withStatusAndTrialEndAt("active", updated.expiredAt()));
        billingOrderRepository.save(newBillingOrder(
                tenantId,
                plan.planCode(),
                plan.planName(),
                "subscribe",
                BigDecimal.valueOf((long) plan.monthlyPrice() * request.seatCount())
        ));
        auditLogService.recordForTenant(tenantId, "SUBSCRIBE_PLAN", "tenant_subscription", updated.subscriptionId());
        return updated;
    }

    public TenantSubscription renew(String tenantId, SubscriptionRenewRequest request) {
        requireTenantProfile(tenantId);
        TenantSubscription current = requireSubscription(tenantId);
        if ("trial".equals(current.planCode())) {
            throw new BusinessException("6011", "trial subscription cannot be renewed", HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime base = current.expiredAt() != null && current.expiredAt().isAfter(OffsetDateTime.now())
                ? current.expiredAt()
                : OffsetDateTime.now();
        TenantSubscription renewed = new TenantSubscription(
                current.subscriptionId(),
                tenantId,
                current.planCode(),
                current.planName(),
                "active",
                current.startedAt(),
                base.plusMonths(request.months()),
                current.seatCount(),
                current.autoRenew()
        );
        tenantSubscriptionRepository.save(renewed);
        tenantProfileRepository.save(requireTenantProfile(tenantId).withStatusAndTrialEndAt("active", renewed.expiredAt()));
        billingOrderRepository.save(newBillingOrder(
                tenantId,
                current.planCode(),
                current.planName(),
                "renew",
                BigDecimal.valueOf((long) requirePlan(current.planCode()).monthlyPrice() * current.seatCount() * request.months())
        ));
        auditLogService.recordForTenant(tenantId, "RENEW_SUBSCRIPTION", "tenant_subscription", renewed.subscriptionId());
        return renewed;
    }

    public TenantSubscription upgrade(String tenantId, PlanChangeRequest request) {
        return changePlan(tenantId, request.planCode(), true);
    }

    public TenantSubscription downgrade(String tenantId, PlanChangeRequest request) {
        return changePlan(tenantId, request.planCode(), false);
    }

    public List<UsageQuota> getQuotas(String tenantId) {
        requireTenantProfile(tenantId);
        return usageQuotaRepository.findByTenantId(tenantId);
    }

    public List<BillingOrder> getBillingOrders(String tenantId) {
        requireTenantProfile(tenantId);
        return billingOrderRepository.findByTenantId(tenantId);
    }

    public BillingOrder settleBillingOrder(String tenantId, String billingOrderId) {
        TenantProfile profile = requireTenantProfile(tenantId);
        BillingOrder billingOrder = billingOrderRepository.findByBillingOrderId(billingOrderId)
                .filter(order -> tenantId.equals(order.tenantId()))
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if ("paid".equals(billingOrder.paymentStatus())) {
            throw new BusinessException("1008", "billing order is already paid", HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime paidAt = OffsetDateTime.now();
        BillingOrder settled = billingOrderRepository.save(new BillingOrder(
                billingOrder.billingOrderId(),
                billingOrder.tenantId(),
                billingOrder.planCode(),
                billingOrder.planName(),
                billingOrder.orderType(),
                billingOrder.payableAmount(),
                "paid",
                billingOrder.externalOrderNo(),
                paidAt,
                billingOrder.createdAt()
        ));
        if ("auto_renew".equals(billingOrder.orderType())) {
            recoverSubscriptionAfterAutoRenewSettlement(tenantId, profile);
        }
        auditLogService.recordForTenant(tenantId, "SETTLE_BILLING_ORDER", "billing_order", billingOrderId);
        return settled;
    }

    public TenantProfile getTenantProfile(String tenantId) {
        return requireTenantProfile(tenantId);
    }

    public TenantProfile suspend(String tenantId) {
        TenantProfile profile = requireTenantProfile(tenantId);
        if ("suspended".equals(profile.tenantStatus())) {
            throw new BusinessException("1008", "状态不允许操作", HttpStatus.BAD_REQUEST);
        }
        TenantProfile updated = profile.withStatusAndTrialEndAt("suspended", profile.trialEndAt());
        tenantProfileRepository.save(updated);
        auditLogService.recordForTenant(tenantId, "SUSPEND_TENANT", "tenant", tenantId);
        return updated;
    }

    public TenantProfile resume(String tenantId) {
        TenantProfile profile = requireTenantProfile(tenantId);
        TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(tenantId).orElse(null);
        OffsetDateTime now = OffsetDateTime.now();
        if (subscription != null
                && subscription.expiredAt() != null
                && subscription.expiredAt().isBefore(now)) {
            throw new BusinessException(
                    "6001",
                    "subscription expired",
                    Map.of("tenantId", tenantId, "expiredAt", subscription.expiredAt().toString()),
                    HttpStatus.BAD_REQUEST
            );
        }
        String nextStatus = subscription != null && "trialing".equals(subscription.subscriptionStatus()) ? "trial" : "active";
        TenantProfile updated = profile.withStatusAndTrialEndAt(nextStatus, profile.trialEndAt());
        tenantProfileRepository.save(updated);
        auditLogService.recordForTenant(tenantId, "RESUME_TENANT", "tenant", tenantId);
        return updated;
    }

    public TenantProfile requestOffboarding(String tenantId) {
        TenantProfile profile = requireTenantProfile(tenantId);
        if ("offboarding_requested".equals(profile.tenantStatus())
                || "pending_cleanup".equals(profile.tenantStatus())
                || "cleaned_archived".equals(profile.tenantStatus())) {
            throw new BusinessException("1008", "状态不允许操作", HttpStatus.BAD_REQUEST);
        }
        TenantProfile updated = profile.withStatusAndTrialEndAt("offboarding_requested", profile.trialEndAt());
        tenantProfileRepository.save(updated);
        auditLogService.recordForTenant(tenantId, "REQUEST_TENANT_OFFBOARDING", "tenant", tenantId);
        return updated;
    }

    public ComplianceDocument getPrivacyPolicy() {
        return requireComplianceDocument("privacy-policy");
    }

    public ComplianceDocument getUserAgreement() {
        return requireComplianceDocument("user-agreement");
    }

    public List<ComplianceAcceptance> listComplianceAcceptances(String tenantId) {
        return latestComplianceAcceptancesByDocument(tenantId).values().stream()
                .map(record -> new ComplianceAcceptance(
                        record.documentCode(),
                        record.documentVersion(),
                        record.acceptedBy(),
                        record.acceptedAt()
                ))
                .sorted(java.util.Comparator.comparing(ComplianceAcceptance::documentCode))
                .toList();
    }

    public List<ComplianceAcceptance> acceptComplianceDocuments(String tenantId, ComplianceAcceptanceRequest request) {
        if (request.documentCodes() == null || request.documentCodes().isEmpty()) {
            throw new BusinessException("1010", "至少选择一个协议文档", HttpStatus.BAD_REQUEST);
        }
        LinkedHashSet<String> uniqueCodes = new LinkedHashSet<>(request.documentCodes());
        String operatorId = currentOperatorId();
        for (String documentCode : uniqueCodes) {
            ComplianceDocument document = requireComplianceDocument(documentCode);
            complianceAcceptanceRepository.save(new ComplianceAcceptanceRecord(
                    "ca-" + UUID.randomUUID(),
                    tenantId,
                    document.documentCode(),
                    document.version(),
                    operatorId,
                    "api",
                    OffsetDateTime.now()
            ));
            auditLogService.recordForTenant(
                    tenantId,
                    actionTypeForDocument(document.documentCode()),
                    "compliance_document",
                    document.documentCode() + ":" + document.version()
            );
        }
        return listComplianceAcceptances(tenantId);
    }

    public List<AdminComplianceDocumentView> listAdminComplianceDocuments() {
        List<TenantProfile> tenantProfiles = tenantProfileRepository.findAll();
        List<AdminComplianceDocumentView> result = new ArrayList<>();
        for (ComplianceDocument document : complianceDocuments.values()) {
            int acceptedCurrentVersionTenantCount = 0;
            int pendingReacceptanceTenantCount = 0;
            int neverAcceptedTenantCount = 0;
            for (TenantProfile tenantProfile : tenantProfiles) {
                String acceptanceStatus = resolveAcceptanceStatus(
                        document.version(),
                        latestComplianceAcceptancesByDocument(tenantProfile.tenantId()).get(document.documentCode())
                );
                switch (acceptanceStatus) {
                    case ACCEPTANCE_STATUS_ACCEPTED_CURRENT_VERSION -> acceptedCurrentVersionTenantCount++;
                    case ACCEPTANCE_STATUS_PENDING_REACCEPTANCE -> pendingReacceptanceTenantCount++;
                    default -> neverAcceptedTenantCount++;
                }
            }
            result.add(new AdminComplianceDocumentView(
                    document.documentCode(),
                    document.version(),
                    document.title(),
                    document.updatedAt(),
                    acceptedCurrentVersionTenantCount,
                    pendingReacceptanceTenantCount,
                    neverAcceptedTenantCount
            ));
        }
        return result;
    }

    public ComplianceDocument publishComplianceDocument(String documentCode, ComplianceDocumentPublishRequest request) {
        ComplianceDocument current = requireComplianceDocument(documentCode);
        if (request == null
                || request.version() == null || request.version().isBlank()
                || request.title() == null || request.title().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new BusinessException("1010", "协议版本、标题和内容不能为空", HttpStatus.BAD_REQUEST);
        }
        if (current.version().equals(request.version())) {
            throw new BusinessException("1013", "协议版本未变化", HttpStatus.BAD_REQUEST);
        }
        ComplianceDocument updated = new ComplianceDocument(
                current.documentCode(),
                request.version(),
                request.title(),
                request.content(),
                OffsetDateTime.now()
        );
        complianceDocuments.put(documentCode, updated);
        auditLogService.record("PUBLISH_COMPLIANCE_DOCUMENT", "compliance_document", documentCode + ":" + request.version());
        return updated;
    }

    public List<AdminComplianceAcceptanceView> listAdminComplianceAcceptances(String tenantId,
                                                                              String documentCode,
                                                                              String acceptanceStatus) {
        validateAcceptanceStatusFilter(acceptanceStatus);
        List<ComplianceDocument> targetDocuments = documentCode == null || documentCode.isBlank()
                ? new ArrayList<>(complianceDocuments.values())
                : List.of(requireComplianceDocument(documentCode));

        return tenantProfileRepository.findAll().stream()
                .filter(profile -> tenantId == null || tenantId.isBlank() || tenantId.equals(profile.tenantId()))
                .flatMap(profile -> targetDocuments.stream()
                        .map(document -> toAdminComplianceAcceptanceView(profile, document)))
                .filter(view -> acceptanceStatus == null
                        || acceptanceStatus.isBlank()
                        || acceptanceStatus.equals(view.acceptanceStatus()))
                .sorted(Comparator.comparing(AdminComplianceAcceptanceView::tenantId)
                        .thenComparing(AdminComplianceAcceptanceView::documentCode))
                .toList();
    }

    public void assertSensitiveOperationCompliance(String tenantId, String permissionCode) {
        if (!SENSITIVE_COMPLIANCE_PERMISSION_CODES.contains(permissionCode)) {
            return;
        }
        List<String> pendingDocumentCodes = complianceDocuments.values().stream()
                .filter(document -> ACCEPTANCE_STATUS_PENDING_REACCEPTANCE.equals(
                        resolveAcceptanceStatus(
                                document.version(),
                                latestComplianceAcceptancesByDocument(tenantId).get(document.documentCode())
                        )
                ))
                .map(ComplianceDocument::documentCode)
                .sorted()
                .toList();
        if (pendingDocumentCodes.isEmpty()) {
            return;
        }
        throw new BusinessException(
                "1014",
                "存在待重签协议，禁止执行敏感操作",
                Map.of(
                        "tenantId", tenantId,
                        "permissionCode", permissionCode,
                        "pendingDocumentCodes", pendingDocumentCodes
                ),
                HttpStatus.FORBIDDEN
        );
    }

    public List<TenantDataExportTaskSummary> listTenantDataExports(String tenantId) {
        requireTenantProfile(tenantId);
        return tenantDataExportTaskRepository.findByTenantId(tenantId).stream()
                .map(this::toSummary)
                .toList();
    }

    public TenantDataExportTaskSummary createTenantDataExport(String tenantId, TenantDataExportRequest request) {
        requireTenantProfile(tenantId);
        DataExportScope scope = requireExportScope(request.scopeCode());
        OffsetDateTime now = OffsetDateTime.now();
        TenantDataExportTaskRecord task = new TenantDataExportTaskRecord(
                "export-" + UUID.randomUUID(),
                tenantId,
                scope.scopeCode(),
                scope.scopeName(),
                currentOperatorId(),
                "completed",
                scope.defaultFileName(tenantId),
                scope.contentType(),
                buildExportContent(tenantId, scope),
                request.maskingStrategy(),
                request.timeRangeStart(),
                request.timeRangeEnd(),
                now.plusDays(3),
                now,
                now
        );
        tenantDataExportTaskRepository.save(task);
        auditLogService.recordForTenant(tenantId, "CREATE_TENANT_DATA_EXPORT", "tenant_data_export_task", task.exportTaskId());
        return toSummary(task);
    }

    public TenantDataExportDownload downloadTenantDataExport(String tenantId, String exportTaskId) {
        requireTenantProfile(tenantId);
        TenantDataExportTaskRecord task = tenantDataExportTaskRepository.findByTaskId(exportTaskId)
                .filter(record -> tenantId.equals(record.tenantId()))
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        if (task.downloadExpiresAt() != null && task.downloadExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("1011", "导出文件已过期", HttpStatus.BAD_REQUEST);
        }
        auditLogService.recordForTenant(tenantId, "DOWNLOAD_TENANT_DATA_EXPORT", "tenant_data_export_task", exportTaskId);
        return new TenantDataExportDownload(
                task.exportTaskId(),
                task.fileName(),
                task.contentType(),
                task.exportContent()
        );
    }

    public List<TenantCleanupTaskView> listTenantCleanupTasks(String tenantId) {
        requireTenantProfile(tenantId);
        return tenantCleanupTaskRepository.findByTenantId(tenantId).stream()
                .map(this::toCleanupTaskView)
                .toList();
    }

    public TenantCleanupTaskView createTenantCleanupTask(String tenantId, TenantCleanupPlanRequest request) {
        TenantProfile profile = requireTenantProfile(tenantId);
        if (!"offboarding_requested".equals(profile.tenantStatus()) && !"pending_cleanup".equals(profile.tenantStatus())) {
            throw new BusinessException("1008", "当前租户未进入退租清理阶段", HttpStatus.BAD_REQUEST);
        }
        boolean hasOpenTask = tenantCleanupTaskRepository.findByTenantId(tenantId).stream()
                .anyMatch(task -> "planned".equals(task.status())
                        || "reviewed".equals(task.status())
                        || "executing".equals(task.status()));
        if (hasOpenTask) {
            throw new BusinessException("1012", "已有未完成的清理任务", HttpStatus.BAD_REQUEST);
        }
        List<String> cleanupScopes = normalizeCleanupScopes(request.cleanupScopes());
        Map<String, Integer> impactSummary = buildCleanupImpactSummary(tenantId, Set.copyOf(cleanupScopes));
        OffsetDateTime now = OffsetDateTime.now();
        TenantCleanupTaskRecord task = new TenantCleanupTaskRecord(
                "cleanup-" + UUID.randomUUID(),
                tenantId,
                "planned",
                request.reason(),
                toJson(cleanupScopes),
                currentOperatorId(),
                null,
                null,
                toJson(impactSummary),
                null,
                now,
                null,
                null
        );
        tenantCleanupTaskRepository.save(task);
        tenantProfileRepository.save(profile.withStatusAndTrialEndAt("pending_cleanup", profile.trialEndAt()));
        auditLogService.recordForTenant(tenantId, "PLAN_TENANT_CLEANUP", "tenant_cleanup_task", task.cleanupTaskId());
        return toCleanupTaskView(task);
    }

    @Transactional
    public TenantCleanupTaskView reviewTenantCleanupTask(String tenantId, String cleanupTaskId) {
        TenantProfile profile = requireTenantProfile(tenantId);
        if (!"pending_cleanup".equals(profile.tenantStatus()) && !"offboarding_requested".equals(profile.tenantStatus())) {
            throw new BusinessException("1008", "current tenant status does not allow cleanup review", HttpStatus.BAD_REQUEST);
        }
        TenantCleanupTaskRecord task = tenantCleanupTaskRepository.findByTaskId(cleanupTaskId)
                .filter(record -> tenantId.equals(record.tenantId()))
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!"planned".equals(task.status())) {
            throw new BusinessException("1008", "cleanup task status does not allow review", HttpStatus.BAD_REQUEST);
        }
        String operatorId = currentOperatorId();
        if (operatorId.equals(task.requestedBy())) {
            throw new BusinessException("1009", "cleanup review requires a different operator", HttpStatus.FORBIDDEN);
        }
        TenantCleanupTaskRecord reviewed = new TenantCleanupTaskRecord(
                task.cleanupTaskId(),
                task.tenantId(),
                "reviewed",
                task.reason(),
                task.cleanupScopesJson(),
                task.requestedBy(),
                operatorId,
                task.executedBy(),
                task.impactSummaryJson(),
                task.resultSummaryJson(),
                task.createdAt(),
                task.executedAt(),
                task.completedAt()
        );
        tenantCleanupTaskRepository.save(reviewed);
        auditLogService.recordForTenant(tenantId, "REVIEW_TENANT_CLEANUP", "tenant_cleanup_task", task.cleanupTaskId());
        return toCleanupTaskView(reviewed);
    }

    @Transactional
    public TenantCleanupTaskView executeTenantCleanupTask(String tenantId, String cleanupTaskId) {
        TenantProfile profile = requireTenantProfile(tenantId);
        if (!"pending_cleanup".equals(profile.tenantStatus()) && !"offboarding_requested".equals(profile.tenantStatus())) {
            throw new BusinessException("1008", "当前租户状态不允许执行清理", HttpStatus.BAD_REQUEST);
        }
        TenantCleanupTaskRecord task = tenantCleanupTaskRepository.findByTaskId(cleanupTaskId)
                .filter(record -> tenantId.equals(record.tenantId()))
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        if (!"reviewed".equals(task.status())) {
            throw new BusinessException("1008", "cleanup task must be reviewed before execution", HttpStatus.BAD_REQUEST);
        }
        String operatorId = currentOperatorId();
        if (operatorId.equals(task.requestedBy()) || operatorId.equals(task.reviewedBy())) {
            throw new BusinessException("1009", "cleanup execution requires a different operator", HttpStatus.FORBIDDEN);
        }

        OffsetDateTime executedAt = OffsetDateTime.now();
        tenantCleanupTaskRepository.save(new TenantCleanupTaskRecord(
                task.cleanupTaskId(),
                task.tenantId(),
                "executing",
                task.reason(),
                task.cleanupScopesJson(),
                task.requestedBy(),
                task.reviewedBy(),
                operatorId,
                task.impactSummaryJson(),
                task.resultSummaryJson(),
                task.createdAt(),
                executedAt,
                null
        ));

        Map<String, Integer> resultSummary = executeTenantCleanupInternal(tenantId, Set.copyOf(fromJsonList(task.cleanupScopesJson())));
        tenantProfileRepository.save(profile.withStatusAndTrialEndAt("cleaned_archived", profile.trialEndAt()));
        TenantCleanupTaskRecord completed = new TenantCleanupTaskRecord(
                task.cleanupTaskId(),
                task.tenantId(),
                "completed",
                task.reason(),
                task.cleanupScopesJson(),
                task.requestedBy(),
                task.reviewedBy(),
                operatorId,
                task.impactSummaryJson(),
                toJson(resultSummary),
                task.createdAt(),
                executedAt,
                OffsetDateTime.now()
        );
        tenantCleanupTaskRepository.save(completed);
        auditLogService.recordForTenant(tenantId, "EXECUTE_TENANT_CLEANUP", "tenant_cleanup_task", task.cleanupTaskId());
        return toCleanupTaskView(completed);
    }

    public UsageQuota consumeQuota(String tenantId, QuotaConsumeRequest request) {
        requireTenantProfile(tenantId);
        List<UsageQuota> snapshots = usageQuotaRepository.findByTenantId(tenantId);
        for (int index = 0; index < snapshots.size(); index++) {
            UsageQuota current = snapshots.get(index);
            if (current.quotaCode().equals(request.quotaCode())) {
                int nextUsedAmount = current.usedAmount() + request.amount();
                if (nextUsedAmount > current.quotaLimit()) {
                    throw new BusinessException(
                            "6002",
                            "quota exceeded",
                            Map.of(
                                    "quotaCode", current.quotaCode(),
                                    "quotaLimit", current.quotaLimit(),
                                    "usedAmount", current.usedAmount()
                            ),
                            HttpStatus.BAD_REQUEST
                    );
                }
                UsageQuota updated = current.withUsedAmount(nextUsedAmount);
                ArrayList<UsageQuota> copied = new ArrayList<>(snapshots);
                copied.set(index, updated);
                usageQuotaRepository.saveAll(tenantId, copied);
                auditLogService.recordForTenant(tenantId, "CONSUME_QUOTA", "usage_quota", current.quotaCode());
                return updated;
            }
        }
        throw new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND);
    }

    public TenantSubscription purchaseSeats(String tenantId, SeatPurchaseRequest request) {
        requireTenantProfile(tenantId);
        TenantSubscription current = requireSubscription(tenantId);
        int nextSeatCount = current.seatCount() + request.seatCount();
        TenantSubscription updated = new TenantSubscription(
                current.subscriptionId(),
                tenantId,
                current.planCode(),
                current.planName(),
                current.subscriptionStatus(),
                current.startedAt(),
                current.expiredAt(),
                nextSeatCount,
                current.autoRenew()
        );
        tenantSubscriptionRepository.save(updated);
        usageQuotaRepository.saveAll(tenantId, buildQuotas(tenantId, current.planCode(), nextSeatCount));
        SubscriptionPlan currentPlan = requirePlan(current.planCode());
        billingOrderRepository.save(newBillingOrder(
                tenantId,
                current.planCode(),
                current.planName(),
                "seat_purchase",
                BigDecimal.valueOf((long) currentPlan.monthlyPrice() * request.seatCount())
        ));
        auditLogService.recordForTenant(tenantId, "PURCHASE_SEATS", "tenant_subscription", updated.subscriptionId());
        return updated;
    }

    public InvoiceRequest createInvoiceRequest(String tenantId, InvoiceCreateRequest request) {
        requireTenantProfile(tenantId);
        BillingOrder billingOrder = billingOrderRepository.findByBillingOrderId(request.billingOrderId())
                .filter(order -> tenantId.equals(order.tenantId()))
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        if (!"paid".equals(billingOrder.paymentStatus())) {
            throw new BusinessException("6015", "billing order is not paid", HttpStatus.BAD_REQUEST);
        }
        InvoiceRequest invoiceRequest = invoiceRequestRepository.save(new InvoiceRequest(
                null,
                tenantId,
                request.billingOrderId(),
                request.invoiceTitle(),
                request.invoiceTaxNo(),
                "pending",
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_INVOICE_REQUEST", "invoice_request", invoiceRequest.invoiceRequestId());
        return invoiceRequest;
    }

    public TenantProfile updateFeatureToggles(String tenantId, Map<String, Boolean> featureFlags) {
        TenantProfile profile = requireTenantProfile(tenantId);
        TenantProfile updated = profile.withFeatureFlags(featureFlags);
        tenantProfileRepository.save(updated);
        auditLogService.recordForTenant(tenantId, "UPDATE_FEATURE_TOGGLES", "tenant", tenantId);
        return updated;
    }

    public List<AdminTenantOverview> listAdminTenantOverviews() {
        return tenantProfileRepository.findAll().stream()
                .map(profile -> {
                    TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(profile.tenantId()).orElse(null);
                    List<UsageQuota> quotas = usageQuotaRepository.findByTenantId(profile.tenantId());
                    int totalQuotaLimit = quotas.stream().mapToInt(UsageQuota::quotaLimit).sum();
                    int totalUsedAmount = quotas.stream().mapToInt(UsageQuota::usedAmount).sum();
                    return new AdminTenantOverview(
                            profile.tenantId(),
                            profile.tenantCode(),
                            profile.tenantName(),
                            profile.tenantStatus(),
                            profile.ownerName(),
                            profile.mobile(),
                            subscription == null ? null : subscription.subscriptionId(),
                            subscription == null ? null : subscription.planCode(),
                            subscription == null ? null : subscription.planName(),
                            subscription == null ? null : subscription.subscriptionStatus(),
                            subscription == null ? 0 : subscription.seatCount(),
                            quotas.size(),
                            totalQuotaLimit,
                            totalUsedAmount,
                            profile.trialEndAt()
                    );
                })
                .toList();
    }

    public SubscriptionAutomationSummary reconcileSubscriptionAutomation() {
        OffsetDateTime now = OffsetDateTime.now();
        List<String> autoRenewedTenantIds = new ArrayList<>();
        List<String> autoSuspendedTenantIds = new ArrayList<>();
        List<String> autoChargeFailedTenantIds = new ArrayList<>();

        for (TenantProfile profile : tenantProfileRepository.findAll()) {
            TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(profile.tenantId()).orElse(null);
            if (subscription == null
                    || "trial".equals(subscription.planCode())
                    || subscription.expiredAt() == null
                    || subscription.expiredAt().isAfter(now)
                    || "offboarding_requested".equals(profile.tenantStatus())
                    || "pending_cleanup".equals(profile.tenantStatus())
                    || "cleaned_archived".equals(profile.tenantStatus())
                    || "payment_overdue".equals(profile.tenantStatus())
                    || "past_due".equals(subscription.subscriptionStatus())) {
                continue;
            }

            if (subscription.autoRenew()) {
                if (!isAutoChargeAuthorized(profile)) {
                    if (hasPendingAutoRenewBillingOrder(profile.tenantId())) {
                        autoChargeFailedTenantIds.add(profile.tenantId());
                        continue;
                    }
                    tenantSubscriptionRepository.save(new TenantSubscription(
                            subscription.subscriptionId(),
                            subscription.tenantId(),
                            subscription.planCode(),
                            subscription.planName(),
                            "past_due",
                            subscription.startedAt(),
                            subscription.expiredAt(),
                            subscription.seatCount(),
                            true
                    ));
                    tenantProfileRepository.save(profile.withStatusAndTrialEndAt("payment_overdue", subscription.expiredAt()));
                    billingOrderRepository.save(newBillingOrder(
                            profile.tenantId(),
                            subscription.planCode(),
                            subscription.planName(),
                            "auto_renew",
                            BigDecimal.valueOf((long) requirePlan(subscription.planCode()).monthlyPrice() * subscription.seatCount()),
                            "pending",
                            null
                    ));
                    auditLogService.recordForTenant(profile.tenantId(), "AUTO_RENEW_CHARGE_PENDING", "tenant_subscription", subscription.subscriptionId());
                    autoChargeFailedTenantIds.add(profile.tenantId());
                    continue;
                }
                TenantSubscription renewed = new TenantSubscription(
                        subscription.subscriptionId(),
                        subscription.tenantId(),
                        subscription.planCode(),
                        subscription.planName(),
                        "active",
                        subscription.startedAt(),
                        subscription.expiredAt().plusMonths(1),
                        subscription.seatCount(),
                        true
                );
                tenantSubscriptionRepository.save(renewed);
                if ("expired_suspended".equals(profile.tenantStatus())
                        || "active".equals(profile.tenantStatus())
                        || "trial".equals(profile.tenantStatus())) {
                    tenantProfileRepository.save(profile.withStatusAndTrialEndAt("active", renewed.expiredAt()));
                }
                SubscriptionPlan currentPlan = requirePlan(subscription.planCode());
                billingOrderRepository.save(newBillingOrder(
                        profile.tenantId(),
                        subscription.planCode(),
                        subscription.planName(),
                        "auto_renew",
                        BigDecimal.valueOf((long) currentPlan.monthlyPrice() * subscription.seatCount())
                ));
                auditLogService.recordForTenant(profile.tenantId(), "AUTO_RENEW_SUBSCRIPTION", "tenant_subscription", subscription.subscriptionId());
                autoRenewedTenantIds.add(profile.tenantId());
                continue;
            }

            if (!"expired".equals(subscription.subscriptionStatus())) {
                tenantSubscriptionRepository.save(new TenantSubscription(
                        subscription.subscriptionId(),
                        subscription.tenantId(),
                        subscription.planCode(),
                        subscription.planName(),
                        "expired",
                        subscription.startedAt(),
                        subscription.expiredAt(),
                        subscription.seatCount(),
                        false
                ));
            }
            if (!"expired_suspended".equals(profile.tenantStatus())) {
                tenantProfileRepository.save(profile.withStatusAndTrialEndAt("expired_suspended", subscription.expiredAt()));
                auditLogService.recordForTenant(profile.tenantId(), "AUTO_SUSPEND_EXPIRED_TENANT", "tenant", profile.tenantId());
                autoSuspendedTenantIds.add(profile.tenantId());
            }
        }

        return new SubscriptionAutomationSummary(
                autoRenewedTenantIds.size(),
                autoSuspendedTenantIds.size(),
                autoChargeFailedTenantIds.size(),
                autoRenewedTenantIds,
                autoSuspendedTenantIds,
                autoChargeFailedTenantIds,
                now
        );
    }

    public ReleaseReadinessView getReleaseReadiness(String tenantId) {
        requireTenantProfile(tenantId);
        ReleaseEvidenceSummary evidenceSummary = buildReleaseEvidenceSummary(tenantId);
        List<String> blockingReasons = new ArrayList<>();
        List<ReleaseChecklistItemView> checklistItems = List.of(
                buildConfigHardeningChecklistItem(blockingReasons),
                buildCriticalStateEvidenceChecklistItem(evidenceSummary),
                buildAuditTraceabilityChecklistItem(tenantId),
                buildAutomationRegressionChecklistItem(),
                buildExternalIntegrationChecklistItem(blockingReasons),
                buildObservabilityStackChecklistItem(blockingReasons),
                buildDeliveryPipelineChecklistItem(blockingReasons),
                buildDualDeliveryAcceptanceChecklistItem(blockingReasons)
        );
        return new ReleaseReadinessView(
                tenantId,
                resolveReleaseConclusion(checklistItems),
                blockingReasons,
                evidenceSummary,
                checklistItems
        );
    }

    public DeliveryReadinessView getDeliveryReadiness(String tenantId) {
        requireTenantProfile(tenantId);
        return new DeliveryReadinessView(
                tenantId,
                buildDeliveryPipelineSnapshot(),
                buildExternalIntegrationConnectivitySnapshot(),
                buildDualDeliveryAcceptanceSnapshot()
        );
    }

    public ObservabilityReadinessView getObservabilityReadiness(String tenantId) {
        requireTenantProfile(tenantId);
        ObservabilityStackConnectivitySnapshot snapshot = getObservabilityStackConnectivitySnapshot();
        List<String> blockingReasons = new ArrayList<>();
        ReleaseChecklistItemView checklistItem = buildObservabilityStackChecklistItem(blockingReasons);
        List<com.dianshang.platform.audit.AuditLogRecord> auditLogs = auditLogService.findByTenantId(tenantId);
        List<String> latestTraceIds = auditLogs.stream()
                .map(com.dianshang.platform.audit.AuditLogRecord::traceId)
                .filter(traceId -> traceId != null && !traceId.isBlank())
                .distinct()
                .limit(5)
                .toList();
        int traceableAuditLogCount = (int) auditLogs.stream()
                .filter(this::hasTraceabilityEvidence)
                .count();
        return new ObservabilityReadinessView(
                tenantId,
                buildObservabilityStackReadinessSnapshot(snapshot, checklistItem),
                new AuditTraceabilitySummary(
                        auditLogs.size(),
                        traceableAuditLogCount,
                        latestTraceIds
                ),
                List.copyOf(blockingReasons)
        );
    }

    public ExternalIntegrationConnectivitySnapshot getExternalIntegrationConnectivitySnapshot() {
        return buildExternalIntegrationConnectivitySnapshot();
    }

    public int countReadyRequiredExternalSystems() {
        return (int) resolveRequiredExternalSystems().stream()
                .filter(this::isRequiredExternalSystemReady)
                .count();
    }

    public boolean isRequiredExternalSystemReady(String systemCode) {
        return evaluateExternalSystem(systemCode).ready();
    }

    public List<String> getExternalIntegrationBlockingReasons() {
        return resolveRequiredExternalSystems().stream()
                .map(this::evaluateExternalSystem)
                .flatMap(readiness -> readiness.blockingReasons().stream())
                .toList();
    }

    public DeliveryPipelineSnapshot getDeliveryPipelineSnapshot() {
        return buildDeliveryPipelineSnapshot();
    }

    public ExternalMessagingCallbackWorkerSnapshot getExternalMessagingCallbackWorkerSnapshot() {
        ExternalSystemReadiness readiness = evaluateMessagingCallbackWorker();
        ExternalSystemConnectivitySnapshot connectivity = probeMessagingCallbackWorker();
        return new ExternalMessagingCallbackWorkerSnapshot(
                environment.getProperty("app.integrations.external.messaging.callback-worker-enabled", Boolean.class, false),
                normalizedProperty("app.integrations.external.messaging.callback-worker-provider", "spring-event"),
                maskEndpoint(environment.getProperty("app.integrations.external.messaging.callback-worker-endpoint", "")),
                normalizedProperty("app.integrations.external.messaging.callback-worker-consumer-group", ""),
                readiness.ready(),
                readiness.missingParts(),
                connectivity.reachable(),
                connectivity.detail()
        );
    }

    public ObservabilityStackConnectivitySnapshot getObservabilityStackConnectivitySnapshot() {
        return new ObservabilityStackConnectivitySnapshot(
                probeObservabilityEndpoint("log-aggregation", "app.observability.log-aggregation-endpoint"),
                probeObservabilityEndpoint("trace", "app.observability.trace-endpoint"),
                probeObservabilityEndpoint("alert-router", "app.observability.alert-router-endpoint"),
                probeObservabilityEndpoint("dashboard", "app.observability.dashboard-url")
        );
    }

    public ExternalSystemConnectivitySnapshot probeHttpExternalEndpoint(String systemCode,
                                                                        String provider,
                                                                        String endpoint) {
        return probeHttpExternalSystemDirect(systemCode, provider, endpoint);
    }

    public void clear() {
        tenantCleanupTaskRepository.deleteAll();
        tenantDataExportTaskRepository.deleteAll();
        complianceAcceptanceRepository.deleteAll();
        invoiceRequestRepository.deleteAll();
        billingOrderRepository.deleteAll();
        tenantProfileRepository.deleteAll();
        tenantSubscriptionRepository.deleteAll();
        usageQuotaRepository.deleteAll();
        authBootstrapService.resetForTests();
        tenantSequence.set(1000);
        subscriptionSequence.set(20000);
        seedComplianceDocuments();
    }

    private TenantProfile requireTenantProfile(String tenantId) {
        return tenantProfileRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
    }

    private TenantSubscription requireSubscription(String tenantId) {
        return tenantSubscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
    }

    private void seedSubscriptionPlans() {
        if (!subscriptionPlanRepository.findAll().isEmpty()) {
            return;
        }
        subscriptionPlanRepository.saveAll(List.of(
                new SubscriptionPlan("trial", "试用版", "fixed", 0, 0, 1),
                new SubscriptionPlan("basic", "基础版", "fixed", 299, 2990, 3),
                new SubscriptionPlan("pro", "专业版", "fixed", 999, 9990, 10)
        ));
    }

    private void seedComplianceDocuments() {
        complianceDocuments.clear();
        complianceDocuments.put("privacy-policy", new ComplianceDocument(
                "privacy-policy",
                "2026.06",
                "隐私政策",
                """
                        平台仅在租户经营、认证鉴权、审计追溯、计费结算和支持协同所必需的范围内处理数据。
                        租户级业务数据默认按 tenant_id 隔离，导出、停服、退租和清理动作必须留痕。
                        涉及争议处理、财务对账和安全审计的数据按最小必要原则保留，不对无关岗位开放明细访问。
                        """,
                OffsetDateTime.parse("2026-06-03T00:00:00+08:00")
        ));
        complianceDocuments.put("user-agreement", new ComplianceDocument(
                "user-agreement",
                "2026.06",
                "用户协议",
                """
                        租户在试用、订阅、停服、退租和注销阶段应遵守平台的权限、数据导出、结算和审计规则。
                        停服后仅保留续费、账单、导出和退租相关能力；退租申请提交后平台可冻结新增业务写入。
                        敏感操作包括日志导出、停复服、退租和开放平台变更，必须经授权并写入审计日志。
                        """,
                OffsetDateTime.parse("2026-06-03T00:00:00+08:00")
        ));
    }

    private List<UsageQuota> defaultQuotas(String tenantId) {
        return buildQuotas(tenantId, "trial", 1);
    }

    private List<UsageQuota> buildQuotas(String tenantId, String planCode, int seatCount) {
        int storeLimit = switch (planCode) {
            case "basic" -> 10;
            case "pro" -> 50;
            default -> 3;
        };
        int liveConcurrency = switch (planCode) {
            case "basic" -> 2;
            case "pro" -> 5;
            default -> 1;
        };
        return List.of(
                new UsageQuota(tenantId, "store_count", storeLimit, 0, null),
                new UsageQuota(tenantId, "seat_count", seatCount, 0, null),
                new UsageQuota(tenantId, "live_concurrency", liveConcurrency, 0, null)
        );
    }

    private SubscriptionPlan requirePlan(String planCode) {
        return subscriptionPlanRepository.findByPlanCode(planCode)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
    }

    private SubscriptionPlan requirePaidPlan(String planCode) {
        SubscriptionPlan plan = requirePlan(planCode);
        if ("trial".equals(plan.planCode())) {
            throw new BusinessException("6012", "trial plan must use start-trial", HttpStatus.BAD_REQUEST);
        }
        return plan;
    }

    private TenantSubscription changePlan(String tenantId, String targetPlanCode, boolean upgrade) {
        requireTenantProfile(tenantId);
        TenantSubscription current = requireSubscription(tenantId);
        SubscriptionPlan targetPlan = requirePaidPlan(targetPlanCode);
        int currentLevel = planLevel(current.planCode());
        int targetLevel = planLevel(targetPlanCode);
        if (upgrade && targetLevel <= currentLevel) {
            throw new BusinessException("6013", "target plan is not an upgrade", HttpStatus.BAD_REQUEST);
        }
        if (!upgrade && targetLevel >= currentLevel) {
            throw new BusinessException("6014", "target plan is not a downgrade", HttpStatus.BAD_REQUEST);
        }
        TenantSubscription updated = new TenantSubscription(
                current.subscriptionId(),
                tenantId,
                targetPlan.planCode(),
                targetPlan.planName(),
                "active",
                current.startedAt(),
                current.expiredAt(),
                current.seatCount(),
                current.autoRenew()
        );
        tenantSubscriptionRepository.save(updated);
        usageQuotaRepository.saveAll(tenantId, buildQuotas(tenantId, targetPlan.planCode(), current.seatCount()));
        tenantProfileRepository.save(requireTenantProfile(tenantId).withStatusAndTrialEndAt("active", updated.expiredAt()));
        BigDecimal amount = BigDecimal.valueOf(Math.max(
                0,
                (targetPlan.monthlyPrice() - requirePlan(current.planCode()).monthlyPrice()) * current.seatCount()
        ));
        if (upgrade) {
            billingOrderRepository.save(newBillingOrder(
                    tenantId,
                    targetPlan.planCode(),
                    targetPlan.planName(),
                    "upgrade",
                    amount
            ));
        }
        auditLogService.recordForTenant(
                tenantId,
                upgrade ? "UPGRADE_SUBSCRIPTION" : "DOWNGRADE_SUBSCRIPTION",
                "tenant_subscription",
                updated.subscriptionId()
        );
        return updated;
    }

    private int planLevel(String planCode) {
        return switch (planCode) {
            case "trial" -> 0;
            case "basic" -> 1;
            case "pro" -> 2;
            default -> -1;
        };
    }

    private BillingOrder newBillingOrder(String tenantId,
                                         String planCode,
                                         String planName,
                                         String orderType,
                                         BigDecimal payableAmount) {
        return newBillingOrder(tenantId, planCode, planName, orderType, payableAmount, "paid", OffsetDateTime.now());
    }

    private BillingOrder newBillingOrder(String tenantId,
                                         String planCode,
                                         String planName,
                                         String orderType,
                                         BigDecimal payableAmount,
                                         String paymentStatus,
                                         OffsetDateTime paidAt) {
        return new BillingOrder(
                null,
                tenantId,
                planCode,
                planName,
                orderType,
                payableAmount,
                paymentStatus,
                "bo-" + UUID.randomUUID(),
                paidAt,
                OffsetDateTime.now()
        );
    }

    private boolean isAutoChargeAuthorized(TenantProfile profile) {
        return profile.featureFlags().getOrDefault("billing_auto_charge_authorized", true);
    }

    private boolean hasPendingAutoRenewBillingOrder(String tenantId) {
        return billingOrderRepository.findByTenantId(tenantId).stream()
                .anyMatch(order -> "auto_renew".equals(order.orderType()) && "pending".equals(order.paymentStatus()));
    }

    private void recoverSubscriptionAfterAutoRenewSettlement(String tenantId, TenantProfile profile) {
        TenantSubscription subscription = requireSubscription(tenantId);
        OffsetDateTime base = subscription.expiredAt() != null && subscription.expiredAt().isAfter(OffsetDateTime.now())
                ? subscription.expiredAt()
                : OffsetDateTime.now();
        TenantSubscription renewed = new TenantSubscription(
                subscription.subscriptionId(),
                subscription.tenantId(),
                subscription.planCode(),
                subscription.planName(),
                "active",
                subscription.startedAt(),
                base.plusMonths(1),
                subscription.seatCount(),
                subscription.autoRenew()
        );
        tenantSubscriptionRepository.save(renewed);
        tenantProfileRepository.save(profile.withStatusAndTrialEndAt("active", renewed.expiredAt()));
        auditLogService.recordForTenant(tenantId, "RECOVER_AUTO_RENEW_SUBSCRIPTION", "tenant_subscription", renewed.subscriptionId());
    }

    public record SubscriptionActionRequest(
            String planCode,
            Integer seatCount,
            Boolean autoRenew
    ) {
    }

    public record SubscriptionRenewRequest(
            Integer months
    ) {
    }

    public record PlanChangeRequest(
            String planCode
    ) {
    }

    public record SeatPurchaseRequest(
            Integer seatCount
    ) {
    }

    public record InvoiceCreateRequest(
            String billingOrderId,
            String invoiceTitle,
            String invoiceTaxNo
    ) {
    }

    public record TenantDataExportRequest(
            String scopeCode,
            String maskingStrategy,
            OffsetDateTime timeRangeStart,
            OffsetDateTime timeRangeEnd
    ) {
    }

    public record TenantCleanupPlanRequest(
            String reason,
            List<String> cleanupScopes
    ) {
    }

    public record ComplianceAcceptanceRequest(
            List<String> documentCodes
    ) {
    }

    public record ComplianceDocumentPublishRequest(
            String version,
            String title,
            String content
    ) {
    }

    public record ComplianceDocument(
            String documentCode,
            String version,
            String title,
            String content,
            OffsetDateTime updatedAt
    ) {
    }

    public record ComplianceAcceptance(
            String documentCode,
            String version,
            String operatorId,
            OffsetDateTime acceptedAt
    ) {
    }

    public record AdminComplianceDocumentView(
            String documentCode,
            String version,
            String title,
            OffsetDateTime updatedAt,
            int acceptedCurrentVersionTenantCount,
            int pendingReacceptanceTenantCount,
            int neverAcceptedTenantCount
    ) {
    }

    public record AdminComplianceAcceptanceView(
            String tenantId,
            String tenantName,
            String tenantStatus,
            String documentCode,
            String currentVersion,
            String latestAcceptedVersion,
            String latestAcceptedBy,
            OffsetDateTime latestAcceptedAt,
            String acceptanceStatus,
            boolean reacceptRequired
    ) {
    }

    public record TenantDataExportTaskSummary(
            String exportTaskId,
            String scopeCode,
            String scopeName,
            String requestedBy,
            String status,
            String fileName,
            String contentType,
            String maskingStrategy,
            OffsetDateTime timeRangeStart,
            OffsetDateTime timeRangeEnd,
            OffsetDateTime downloadExpiresAt,
            OffsetDateTime createdAt,
            OffsetDateTime completedAt
    ) {
    }

    public record TenantDataExportDownload(
            String exportTaskId,
            String fileName,
            String contentType,
            String content
    ) {
    }

    public record TenantCleanupTaskView(
            String cleanupTaskId,
            String status,
            String reason,
            List<String> cleanupScopes,
            String requestedBy,
            String reviewedBy,
            String executedBy,
            Map<String, Integer> impactSummary,
            Map<String, Integer> resultSummary,
            OffsetDateTime createdAt,
            OffsetDateTime executedAt,
            OffsetDateTime completedAt
    ) {
    }

    public record SubscriptionAutomationSummary(
            int autoRenewedCount,
            int autoSuspendedCount,
            int autoChargeFailedCount,
            List<String> autoRenewedTenantIds,
            List<String> autoSuspendedTenantIds,
            List<String> autoChargeFailedTenantIds,
            OffsetDateTime executedAt
    ) {
    }

    public record ReleaseReadinessView(
            String tenantId,
            String conclusion,
            List<String> blockingReasons,
            ReleaseEvidenceSummary evidenceSummary,
            List<ReleaseChecklistItemView> checklistItems
    ) {
    }

    public record DeliveryReadinessView(
            String tenantId,
            DeliveryPipelineSnapshot pipeline,
            ExternalIntegrationConnectivitySnapshot externalIntegrations,
            DualDeliveryAcceptanceSnapshot acceptance
    ) {
    }

    public record ObservabilityReadinessView(
            String tenantId,
            ObservabilityStackReadinessSnapshot stack,
            AuditTraceabilitySummary auditTraceability,
            List<String> blockingReasons
    ) {
    }

    public record ReleaseChecklistItemView(
            String itemCode,
            String status,
            int evidenceCount,
            String detail
    ) {
    }

    public record ReleaseEvidenceSummary(
            int exportTaskCount,
            int cleanupTaskCount,
            int complianceAcceptanceCount,
            int billingOrderCount
    ) {
    }

    public record DeliveryPipelineSnapshot(
            boolean ready,
            String githubOwner,
            String githubRepository,
            String repository,
            String registry,
            String imageRepository,
            boolean releaseKeyConfigured,
            boolean canaryEnabled,
            DeliveryControlDiagnosticSnapshot releaseKeyControl,
            DeliveryControlDiagnosticSnapshot registryAuthControl,
            DeliveryControlDiagnosticSnapshot githubPublishingControl,
            DeliveryControlDiagnosticSnapshot canaryControl,
            DeliveryAssetSnapshot workflowAsset,
            DeliveryAssetSnapshot standardSaasComposeAsset,
            DeliveryAssetSnapshot privateComposeAsset,
            ExternalSystemConnectivitySnapshot githubProbe,
            ExternalSystemConnectivitySnapshot registryProbe
        ) {
    }

    public record DeliveryControlDiagnosticSnapshot(
            String controlCode,
            String value,
            boolean configured,
            String sourceType,
            String sourceName,
            boolean defaultValue,
            boolean trusted,
            String status,
            String detail
    ) {
    }

    public record DeliveryAssetSnapshot(
            String assetCode,
            String path,
            boolean present,
            boolean ready,
            String detail
    ) {
    }

    public record ObservabilityStackReadinessSnapshot(
            boolean ready,
            int configuredCount,
            int reachableCount,
            ExternalSystemConnectivitySnapshot logAggregation,
            ExternalSystemConnectivitySnapshot trace,
            ExternalSystemConnectivitySnapshot alertRouter,
            ExternalSystemConnectivitySnapshot dashboard,
            String releaseGateStatus,
            String releaseGateDetail
    ) {
    }

    public record AuditTraceabilitySummary(
            int auditLogCount,
            int traceableAuditLogCount,
            List<String> latestTraceIds
    ) {
    }

    public record ExternalIntegrationConnectivitySnapshot(
            boolean ready,
            int configuredCount,
            int reachableCount,
            ExternalSystemConnectivitySnapshot erp,
            ExternalSystemConnectivitySnapshot wms,
            ExternalSystemConnectivitySnapshot messaging,
            ExternalSystemConnectivitySnapshot bi,
            ExternalSystemConnectivitySnapshot routing
    ) {
    }

    public record ExternalSystemConnectivitySnapshot(
            String systemCode,
            String provider,
            String protocol,
            boolean configured,
            boolean reachable,
            String host,
            String maskedTarget,
            String sourceType,
            String sourceName,
            boolean defaultValue,
            boolean trusted,
            String status,
            String detail
    ) {
    }

    public record ExternalMessagingCallbackWorkerSnapshot(
            boolean enabled,
            String provider,
            String maskedEndpoint,
            String consumerGroup,
            boolean ready,
            List<String> missingParts,
            boolean probeReachable,
            String probeDetail
    ) {
    }

    public record ObservabilityStackConnectivitySnapshot(
            ExternalSystemConnectivitySnapshot logAggregation,
            ExternalSystemConnectivitySnapshot trace,
            ExternalSystemConnectivitySnapshot alertRouter,
            ExternalSystemConnectivitySnapshot dashboard
    ) {
    }

    public record DualDeliveryAcceptanceSnapshot(
            boolean ready,
            DeliveryEndpointSnapshot standardSaas,
            DeliveryEndpointSnapshot privateDeployment
    ) {
    }

    public record DeliveryEndpointSnapshot(
            String mode,
            boolean configured,
            String protocol,
            boolean reachable,
            String host,
            String maskedBaseUrl,
            String verifiedAt,
            boolean verificationFresh,
            String verificationStatus,
            long verificationAgeDays,
            String detail
    ) {
    }

    private record DeliveryEvidenceFreshness(
            boolean fresh,
            String status,
            long ageDays
    ) {
    }

    private record DeliveryPropertySourceResolution(
            String value,
            String sourceType,
            String sourceName,
            boolean defaultValue,
            boolean trusted
    ) {
    }

    private record PropertySourceMatch(
            String sourceName,
            String value
    ) {
    }

    private TenantDataExportTaskSummary toSummary(TenantDataExportTaskRecord task) {
        return new TenantDataExportTaskSummary(
                task.exportTaskId(),
                task.scopeCode(),
                task.scopeName(),
                task.requestedBy(),
                task.status(),
                task.fileName(),
                task.contentType(),
                task.maskingStrategy(),
                task.timeRangeStart(),
                task.timeRangeEnd(),
                task.downloadExpiresAt(),
                task.createdAt(),
                task.completedAt()
        );
    }

    private TenantCleanupTaskView toCleanupTaskView(TenantCleanupTaskRecord task) {
        return new TenantCleanupTaskView(
                task.cleanupTaskId(),
                task.status(),
                task.reason(),
                fromJsonList(task.cleanupScopesJson()),
                task.requestedBy(),
                task.reviewedBy(),
                task.executedBy(),
                fromJson(task.impactSummaryJson()),
                fromJson(task.resultSummaryJson()),
                task.createdAt(),
                task.executedAt(),
                task.completedAt()
        );
    }

    private ReleaseEvidenceSummary buildReleaseEvidenceSummary(String tenantId) {
        return new ReleaseEvidenceSummary(
                tenantDataExportTaskRepository.findByTenantId(tenantId).size(),
                tenantCleanupTaskRepository.findByTenantId(tenantId).size(),
                complianceAcceptanceRepository.findByTenantId(tenantId).size(),
                billingOrderRepository.findByTenantId(tenantId).size()
        );
    }

    private ReleaseChecklistItemView buildConfigHardeningChecklistItem(List<String> blockingReasons) {
        List<String> reasons = new ArrayList<>();
        int totalChecks = 4;
        if (!authSecurityConfigVerifier.isTokenSecretStrong()) {
            reasons.add("default auth secret is still enabled");
            blockingReasons.add("default auth secret is still enabled");
        }
        if (!authSecurityConfigVerifier.isBootstrapPasswordStrong()) {
            reasons.add("bootstrap password is still using insecure defaults");
            blockingReasons.add("bootstrap password is still using insecure defaults");
        }
        if (!authSecurityConfigVerifier.requireExplicitSecrets()) {
            reasons.add("explicit auth secret enforcement is disabled");
            blockingReasons.add("explicit auth secret enforcement is disabled");
        }
        if (authSecurityConfigVerifier.isLegacyHeaderContextEnabled()) {
            reasons.add("legacy header context fallback is still enabled");
            blockingReasons.add("legacy header context fallback is still enabled");
        }
        if (reasons.isEmpty()) {
            return new ReleaseChecklistItemView(
                    "config_hardening",
                    "passed",
                    totalChecks,
                    "auth secret, bootstrap password, explicit secret enforcement and header context governance are production-ready"
            );
        }
        return new ReleaseChecklistItemView(
                "config_hardening",
                "blocked",
                Math.max(0, totalChecks - reasons.size()),
                String.join("; ", reasons)
        );
    }

    private ReleaseChecklistItemView buildCriticalStateEvidenceChecklistItem(ReleaseEvidenceSummary evidenceSummary) {
        int evidenceCount = 0;
        if (evidenceSummary.exportTaskCount() > 0) {
            evidenceCount++;
        }
        if (evidenceSummary.cleanupTaskCount() > 0) {
            evidenceCount++;
        }
        if (evidenceSummary.complianceAcceptanceCount() > 0) {
            evidenceCount++;
        }
        if (evidenceSummary.billingOrderCount() > 0) {
            evidenceCount++;
        }
        return new ReleaseChecklistItemView(
                "critical_state_evidence",
                evidenceCount == 4 ? "passed" : "blocked",
                evidenceCount,
                "export, cleanup, compliance and billing evidence are required"
        );
    }

    private ReleaseChecklistItemView buildAuditTraceabilityChecklistItem(String tenantId) {
        List<com.dianshang.platform.audit.AuditLogRecord> auditLogs = auditLogService.findByTenantId(tenantId);
        boolean traceable = !auditLogs.isEmpty() && auditLogs.stream().anyMatch(this::hasTraceabilityEvidence);
        return new ReleaseChecklistItemView(
                "audit_traceability",
                traceable ? "passed" : "blocked",
                auditLogs.size(),
                traceable
                        ? "tenant audit samples include traceId, operatorId, targetId, actionType and createdAt"
                        : "tenant audit samples are missing release traceability fields"
        );
    }

    private ReleaseChecklistItemView buildAutomationRegressionChecklistItem() {
        if (releaseAutomationEvidenceSuiteCount <= 0 || releaseAutomationEvidenceExecutedAt == null
                || releaseAutomationEvidenceExecutedAt.isBlank()) {
            return new ReleaseChecklistItemView(
                    "automation_regression_evidence",
                    "pending",
                    0,
                    "cross-environment automation regression evidence is not attached yet"
            );
        }
        StringBuilder detail = new StringBuilder("automation regression evidence attached at ")
                .append(releaseAutomationEvidenceExecutedAt);
        if (releaseAutomationEvidenceEnvironment != null && !releaseAutomationEvidenceEnvironment.isBlank()) {
            detail.append(" for ").append(releaseAutomationEvidenceEnvironment);
        }
        detail.append("; passed suites=").append(releaseAutomationEvidenceSuiteCount);
        if (releaseAutomationEvidenceSummary != null && !releaseAutomationEvidenceSummary.isBlank()) {
            detail.append("; ").append(releaseAutomationEvidenceSummary);
        }
        return new ReleaseChecklistItemView(
                "automation_regression_evidence",
                "passed",
                releaseAutomationEvidenceSuiteCount,
                detail.toString()
        );
    }

    private ReleaseChecklistItemView buildExternalIntegrationChecklistItem(List<String> blockingReasons) {
        List<String> requiredSystems = resolveRequiredExternalSystems();
        if (requiredSystems.isEmpty()) {
            return new ReleaseChecklistItemView(
                    "external_integration_readiness",
                    "blocked",
                    0,
                    "required external integrations are not declared"
            );
        }
        ExternalIntegrationConnectivitySnapshot connectivitySnapshot = buildExternalIntegrationConnectivitySnapshot();
        List<String> missing = new ArrayList<>();
        int readyCount = 0;
        for (String systemCode : requiredSystems) {
            ExternalSystemReadiness readiness = evaluateExternalSystem(systemCode);
            if (!readiness.ready()) {
                missing.addAll(readiness.blockingReasons());
                blockingReasons.addAll(readiness.blockingReasons());
                continue;
            }
            ExternalSystemConnectivitySnapshot connectivity = findConnectivitySnapshot(connectivitySnapshot, systemCode);
            if (connectivity != null && !connectivity.reachable()) {
                String detail = buildExternalSystemDisplayName(systemCode) + " integration probe is unreachable";
                missing.add(detail);
                blockingReasons.add(detail);
                continue;
            }
            if ("messaging".equals(systemCode)) {
                ExternalSystemConnectivitySnapshot callbackWorkerConnectivity = probeMessagingCallbackWorker();
                if (!callbackWorkerConnectivity.reachable()) {
                    String detail = buildExternalSystemDisplayName(systemCode) + " callback worker probe is unreachable";
                    missing.add(detail);
                    blockingReasons.add(detail);
                    continue;
                }
            }
            readyCount++;
        }
        String readyDetail = requiredSystems.stream()
                .map(this::buildExternalSystemDisplayName)
                .collect(Collectors.joining(", "));
        return new ReleaseChecklistItemView(
                "external_integration_readiness",
                missing.isEmpty() ? "passed" : "blocked",
                readyCount,
                missing.isEmpty()
                        ? buildExternalIntegrationReadyDetail(readyDetail, connectivitySnapshot, requiredSystems)
                        : String.join("; ", missing)
        );
    }

    private ReleaseChecklistItemView buildObservabilityStackChecklistItem(List<String> blockingReasons) {
        ObservabilityStackConnectivitySnapshot snapshot = getObservabilityStackConnectivitySnapshot();
        List<String> missing = new ArrayList<>();
        List<String> untrusted = new ArrayList<>();
        List<String> unreachable = new ArrayList<>();
        collectObservabilityEndpointStatus(snapshot.logAggregation(), "log aggregation endpoint", missing, untrusted, unreachable, blockingReasons);
        collectObservabilityEndpointStatus(snapshot.trace(), "trace endpoint", missing, untrusted, unreachable, blockingReasons);
        collectObservabilityEndpointStatus(snapshot.alertRouter(), "alert router endpoint", missing, untrusted, unreachable, blockingReasons);
        collectObservabilityEndpointStatus(snapshot.dashboard(), "dashboard url", missing, untrusted, unreachable, blockingReasons);
        int totalChecks = 4;
        int reachableCount = countReachableObservabilityEndpoints(snapshot);
        List<String> detailParts = new ArrayList<>();
        if (missing.isEmpty() && untrusted.isEmpty() && unreachable.isEmpty()) {
            detailParts.add("log aggregation, tracing, alert routing and dashboard access are configured");
        } else {
            if (!missing.isEmpty()) {
                detailParts.add("missing " + String.join(", ", missing));
            }
            if (!untrusted.isEmpty()) {
                detailParts.add("untrusted " + String.join(", ", untrusted));
            }
            if (!unreachable.isEmpty()) {
                detailParts.add("unreachable " + String.join(", ", unreachable));
            }
        }
        detailParts.add("controls " + buildObservabilityControlSummary(snapshot));
        detailParts.add("probe reachable " + reachableCount + "/" + totalChecks);
        return new ReleaseChecklistItemView(
                "observability_stack_readiness",
                missing.isEmpty() && untrusted.isEmpty() && unreachable.isEmpty() ? "passed" : "blocked",
                reachableCount,
                String.join("; ", detailParts)
        );
    }

    private ReleaseChecklistItemView buildDeliveryPipelineChecklistItem(List<String> blockingReasons) {
        List<String> missing = new ArrayList<>();
        DeliveryPipelineSnapshot snapshot = buildDeliveryPipelineSnapshot();
        if (!isConfigured("app.delivery.github-owner")) {
            missing.add("github owner");
            blockingReasons.add("delivery pipeline is missing github owner");
        }
        if (!isConfigured("app.delivery.github-repository")) {
            missing.add("github repository");
            blockingReasons.add("delivery pipeline is missing github repository");
        }
        if (!isConfigured("app.delivery.registry")) {
            missing.add("container registry");
            blockingReasons.add("delivery pipeline is missing container registry");
        }
        if (!isConfigured("app.delivery.image-repository")) {
            missing.add("image repository");
            blockingReasons.add("delivery pipeline is missing image repository");
        }
        collectDeliveryControlStatus(snapshot.releaseKeyControl(), "release key injection", missing, blockingReasons);
        collectDeliveryControlStatus(snapshot.registryAuthControl(), "container registry publish credentials", missing, blockingReasons);
        collectDeliveryControlStatus(snapshot.githubPublishingControl(), "github publish mode", missing, blockingReasons);
        collectDeliveryControlStatus(snapshot.canaryControl(), "canary strategy", missing, blockingReasons);
        collectDeliveryAssetStatus(snapshot.workflowAsset(), "workflow asset", missing, blockingReasons);
        collectDeliveryAssetStatus(snapshot.standardSaasComposeAsset(), "standard saas compose asset", missing, blockingReasons);
        collectDeliveryAssetStatus(snapshot.privateComposeAsset(), "private compose asset", missing, blockingReasons);
        List<String> unreachable = new ArrayList<>();
        collectDeliveryPipelineProbeStatus(
                snapshot.githubProbe(),
                "github repository probe endpoint",
                "github repository probe",
                missing,
                unreachable,
                blockingReasons
        );
        collectDeliveryPipelineProbeStatus(
                snapshot.registryProbe(),
                "container registry probe endpoint",
                "container registry probe",
                missing,
                unreachable,
                blockingReasons
        );
        int totalChecks = 13;
        int reachableCount = countReachableDeliveryPipelineProbes(snapshot);
        List<String> detailParts = new ArrayList<>();
        if (!missing.isEmpty()) {
            detailParts.add("missing " + String.join(", ", missing));
        }
        if (!unreachable.isEmpty()) {
            detailParts.add("unreachable " + String.join(", ", unreachable));
        }
        detailParts.add("controls " + buildDeliveryControlSummary(snapshot));
        detailParts.add("probe reachable " + reachableCount + "/2");
        return new ReleaseChecklistItemView(
                "delivery_pipeline_readiness",
                missing.isEmpty() && unreachable.isEmpty() ? "passed" : "blocked",
                totalChecks - missing.size() - unreachable.size(),
                missing.isEmpty() && unreachable.isEmpty()
                        ? "github, registry, release key, canary controls and local delivery assets are configured; controls "
                        + buildDeliveryControlSummary(snapshot) + "; probe reachable 2/2"
                        : String.join("; ", detailParts)
        );
    }

    private ReleaseChecklistItemView buildDualDeliveryAcceptanceChecklistItem(List<String> blockingReasons) {
        List<String> missing = new ArrayList<>();
        if (!isConfigured("app.delivery.standard-saas-base-url")) {
            missing.add("standard saas base url");
            blockingReasons.add("dual delivery acceptance is missing standard saas base url");
        }
        if (!isConfigured("app.delivery.standard-saas-verified-at")) {
            missing.add("standard saas verification timestamp");
            blockingReasons.add("dual delivery acceptance is missing standard saas verification timestamp");
        }
        if (!isConfigured("app.delivery.private-base-url")) {
            missing.add("private deployment base url");
            blockingReasons.add("dual delivery acceptance is missing private deployment base url");
        }
        if (!isConfigured("app.delivery.private-verified-at")) {
            missing.add("private deployment verification timestamp");
            blockingReasons.add("dual delivery acceptance is missing private deployment verification timestamp");
        }
        DeliveryEndpointSnapshot standardSaas = buildDeliveryEndpointSnapshot(
                "standard-saas",
                "app.delivery.standard-saas-base-url",
                "app.delivery.standard-saas-verified-at"
        );
        DeliveryEndpointSnapshot privateDeployment = buildDeliveryEndpointSnapshot(
                "private-deployment",
                "app.delivery.private-base-url",
                "app.delivery.private-verified-at"
        );
        List<String> unreachable = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        collectAcceptanceVerificationStatus(standardSaas, "standard-saas", stale, invalid, blockingReasons);
        collectAcceptanceVerificationStatus(privateDeployment, "private-deployment", stale, invalid, blockingReasons);
        if (standardSaas.configured() && !standardSaas.reachable()) {
            unreachable.add("standard saas");
            blockingReasons.add("dual delivery acceptance standard saas probe is unreachable");
        }
        if (privateDeployment.configured() && !privateDeployment.reachable()) {
            unreachable.add("private deployment");
            blockingReasons.add("dual delivery acceptance private deployment probe is unreachable");
        }
        int totalChecks = 8;
        long reachableCount = countReachableDeliveryEndpoints(standardSaas, privateDeployment);
        List<String> detailParts = new ArrayList<>();
        if (!missing.isEmpty()) {
            detailParts.add("missing " + String.join(", ", missing));
        }
        if (!invalid.isEmpty()) {
            detailParts.add("invalid " + String.join(", ", invalid));
        }
        if (!stale.isEmpty()) {
            detailParts.add("stale " + String.join(", ", stale));
        }
        if (!unreachable.isEmpty()) {
            detailParts.add("unreachable " + String.join(", ", unreachable));
        }
        detailParts.add("verification fresh " + countFreshDeliveryEndpoints(standardSaas, privateDeployment) + "/2");
        detailParts.add("probe reachable " + reachableCount + "/2");
        return new ReleaseChecklistItemView(
                "dual_delivery_acceptance",
                missing.isEmpty() && invalid.isEmpty() && stale.isEmpty() && unreachable.isEmpty() ? "passed" : "blocked",
                totalChecks - missing.size() - invalid.size() - stale.size() - unreachable.size(),
                missing.isEmpty() && invalid.isEmpty() && stale.isEmpty() && unreachable.isEmpty()
                        ? "standard saas and private deployment acceptance evidence is attached; verification fresh 2/2; probe reachable 2/2"
                        : String.join("; ", detailParts)
        );
    }

    private DeliveryPipelineSnapshot buildDeliveryPipelineSnapshot() {
        String owner = environment.getProperty("app.delivery.github-owner", "");
        String repository = environment.getProperty("app.delivery.github-repository", "");
        String registry = environment.getProperty("app.delivery.registry", "");
        String imageRepository = environment.getProperty("app.delivery.image-repository", "");
        boolean releaseKeyConfigured = environment.getProperty("app.delivery.release-key-configured", Boolean.class, false);
        boolean registryAuthConfigured = environment.getProperty("app.delivery.registry-auth-configured", Boolean.class, false);
        boolean canaryEnabled = environment.getProperty("app.delivery.canary-enabled", Boolean.class, false);
        DeliveryAssetSnapshot workflowAsset = evaluateDeliveryAsset(
                "workflow",
                environment.getProperty("app.delivery.workflow-path", ".github/workflows/backend-delivery.yml"),
                List.of(
                        "workflow_dispatch:",
                        "docker/build-push-action@v6",
                        "docker compose -f docker-compose.saas.yml config",
                        "docker compose -f docker-compose.private.yml config"
                ),
                "workflow dispatch, image publish and compose validation are configured"
        );
        DeliveryAssetSnapshot standardSaasComposeAsset = evaluateDeliveryAsset(
                "standard-saas-compose",
                environment.getProperty("app.delivery.standard-saas-compose-path", "docker-compose.saas.yml"),
                List.of(
                        "services:",
                        "app:",
                        "postgres:",
                        "redis:",
                        "APP_DEPLOYMENT_DEFAULT_MODE: standard-saas"
                ),
                "standard saas compose baseline is present"
        );
        DeliveryAssetSnapshot privateComposeAsset = evaluateDeliveryAsset(
                "private-compose",
                environment.getProperty("app.delivery.private-compose-path", "docker-compose.private.yml"),
                List.of(
                        "services:",
                        "app:",
                        "postgres:",
                        "redis:",
                        "APP_DEPLOYMENT_DEFAULT_MODE: private-deployment"
                ),
                "private deployment compose baseline is present"
        );
        ExternalSystemConnectivitySnapshot githubProbe = probeHttpExternalEndpoint(
                "github-repository",
                "github",
                environment.getProperty("app.delivery.github-probe-endpoint", "")
        );
        ExternalSystemConnectivitySnapshot registryProbe = probeHttpExternalEndpoint(
                "container-registry",
                environment.getProperty("app.delivery.registry", "registry"),
                environment.getProperty("app.delivery.registry-probe-endpoint", "")
        );
        DeliveryControlDiagnosticSnapshot releaseKeyControl = buildBooleanDeliveryControl(
                "release-key",
                "app.delivery.release-key-configured",
                "APP_DELIVERY_RELEASE_KEY_CONFIGURED",
                releaseKeyConfigured,
                true,
                "release key injection is explicitly configured",
                "release key injection must be explicitly configured",
                "release key injection still relies on default disabled value",
                "release key injection is explicitly disabled"
        );
        DeliveryControlDiagnosticSnapshot registryAuthControl = buildBooleanDeliveryControl(
                "registry-auth",
                "app.delivery.registry-auth-configured",
                "APP_DELIVERY_REGISTRY_AUTH_CONFIGURED",
                registryAuthConfigured,
                true,
                "container registry publish credentials are explicitly configured",
                "container registry publish credentials must be explicitly configured",
                "container registry publish credentials still rely on default disabled value",
                "container registry publish credentials are explicitly disabled"
        );
        DeliveryControlDiagnosticSnapshot githubPublishingControl = buildGithubPublishingControl(owner, repository, workflowAsset);
        DeliveryControlDiagnosticSnapshot canaryControl = buildCanaryControl(canaryEnabled);
        return new DeliveryPipelineSnapshot(
                isDeliveryPipelineReady(
                        workflowAsset,
                        standardSaasComposeAsset,
                        privateComposeAsset,
                        releaseKeyControl,
                        registryAuthControl,
                        githubPublishingControl,
                        canaryControl,
                        githubProbe,
                        registryProbe
                ),
                owner,
                repository,
                owner.isBlank() || repository.isBlank() ? "" : owner + "/" + repository,
                registry,
                imageRepository,
                releaseKeyConfigured,
                canaryEnabled,
                releaseKeyControl,
                registryAuthControl,
                githubPublishingControl,
                canaryControl,
                workflowAsset,
                standardSaasComposeAsset,
                privateComposeAsset,
                githubProbe,
                registryProbe
        );
    }

    private DeliveryAssetSnapshot evaluateDeliveryAsset(String assetCode,
                                                        String configuredPath,
                                                        List<String> requiredMarkers,
                                                        String readyDetail) {
        Path path = Path.of(configuredPath).normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return new DeliveryAssetSnapshot(assetCode, configuredPath, false, false, "file not found");
        }
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return new DeliveryAssetSnapshot(assetCode, configuredPath, true, false, "file read failed");
        }
        List<String> missingMarkers = requiredMarkers.stream()
                .filter(marker -> !content.contains(marker))
                .toList();
        if (!missingMarkers.isEmpty()) {
            return new DeliveryAssetSnapshot(
                    assetCode,
                    configuredPath,
                    true,
                    false,
                    "missing markers: " + String.join(", ", missingMarkers)
            );
        }
        return new DeliveryAssetSnapshot(assetCode, configuredPath, true, true, readyDetail);
    }

    private ExternalIntegrationConnectivitySnapshot buildExternalIntegrationConnectivitySnapshot() {
        ExternalSystemConnectivitySnapshot erp = probeHttpExternalSystem(
                "erp",
                "app.integrations.external.erp.provider",
                "app.integrations.external.erp.endpoint"
        );
        ExternalSystemConnectivitySnapshot wms = probeHttpExternalSystem(
                "wms",
                "app.integrations.external.wms.provider",
                "app.integrations.external.wms.endpoint"
        );
        ExternalSystemConnectivitySnapshot messaging = probeTcpExternalSystem(
                "messaging",
                "app.integrations.external.messaging.provider",
                "app.integrations.external.messaging.endpoint"
        );
        ExternalSystemConnectivitySnapshot bi = probeHttpExternalSystem(
                "bi",
                "app.integrations.external.bi.provider",
                "app.integrations.external.bi.endpoint"
        );
        ExternalSystemConnectivitySnapshot routing = probeHttpExternalSystem(
                "routing",
                "app.integrations.external.routing.provider",
                "app.integrations.external.routing.endpoint"
        );
        List<ExternalSystemConnectivitySnapshot> snapshots = List.of(erp, wms, messaging, bi, routing);
        int configuredCount = (int) snapshots.stream().filter(ExternalSystemConnectivitySnapshot::configured).count();
        int reachableCount = (int) snapshots.stream().filter(ExternalSystemConnectivitySnapshot::reachable).count();
        ExternalIntegrationConnectivitySnapshot snapshot = new ExternalIntegrationConnectivitySnapshot(
                false,
                configuredCount,
                reachableCount,
                erp,
                wms,
                messaging,
                bi,
                routing
        );
        boolean ready = resolveRequiredExternalSystems().stream()
                .allMatch(this::isRequiredExternalSystemReady);
        if (ready) {
            for (String systemCode : resolveRequiredExternalSystems()) {
                ExternalSystemConnectivitySnapshot connectivity = findConnectivitySnapshot(snapshot, systemCode);
                if (connectivity != null && (!connectivity.configured() || !connectivity.trusted() || !connectivity.reachable())) {
                    ready = false;
                    break;
                }
            }
        }
        return new ExternalIntegrationConnectivitySnapshot(
                ready,
                configuredCount,
                reachableCount,
                erp,
                wms,
                messaging,
                bi,
                routing
        );
    }

    private ExternalSystemConnectivitySnapshot findConnectivitySnapshot(ExternalIntegrationConnectivitySnapshot snapshot, String systemCode) {
        return switch (systemCode) {
            case "erp" -> snapshot.erp();
            case "wms" -> snapshot.wms();
            case "messaging" -> snapshot.messaging();
            case "bi" -> snapshot.bi();
            case "routing" -> snapshot.routing();
            default -> null;
        };
    }

    private DualDeliveryAcceptanceSnapshot buildDualDeliveryAcceptanceSnapshot() {
        DeliveryEndpointSnapshot standardSaas = buildDeliveryEndpointSnapshot(
                "standard-saas",
                "app.delivery.standard-saas-base-url",
                "app.delivery.standard-saas-verified-at"
        );
        DeliveryEndpointSnapshot privateDeployment = buildDeliveryEndpointSnapshot(
                "private-deployment",
                "app.delivery.private-base-url",
                "app.delivery.private-verified-at"
        );
        return new DualDeliveryAcceptanceSnapshot(
                standardSaas.configured() && standardSaas.verificationFresh() && standardSaas.reachable()
                        && privateDeployment.configured() && privateDeployment.verificationFresh() && privateDeployment.reachable(),
                standardSaas,
                privateDeployment
        );
    }

    private DeliveryEndpointSnapshot buildDeliveryEndpointSnapshot(String mode, String baseUrlKey, String verifiedAtKey) {
        String baseUrl = environment.getProperty(baseUrlKey, "");
        String verifiedAt = environment.getProperty(verifiedAtKey, "");
        ExternalSystemConnectivitySnapshot connectivity = probeHttpExternalEndpoint(mode, "delivery", baseUrl);
        boolean verifiedAtConfigured = isConfigured(verifiedAtKey);
        boolean configured = isConfigured(baseUrlKey) && verifiedAtConfigured;
        DeliveryEvidenceFreshness freshness = evaluateDeliveryEvidenceFreshness(verifiedAt, verifiedAtConfigured);
        String detail = connectivity.detail();
        if (isConfigured(baseUrlKey) && !verifiedAtConfigured) {
            detail = "verification timestamp missing; " + detail;
        } else if ("invalid".equals(freshness.status())) {
            detail = "verification timestamp invalid; " + detail;
        } else if ("stale".equals(freshness.status())) {
            detail = "verification evidence is stale; " + detail;
        }
        return new DeliveryEndpointSnapshot(
                mode,
                configured,
                connectivity.protocol(),
                connectivity.reachable(),
                connectivity.host(),
                connectivity.maskedTarget(),
                verifiedAt == null ? "" : verifiedAt,
                freshness.fresh(),
                freshness.status(),
                freshness.ageDays(),
                detail
        );
    }

    private DeliveryEvidenceFreshness evaluateDeliveryEvidenceFreshness(String verifiedAt, boolean verifiedAtConfigured) {
        if (!verifiedAtConfigured) {
            return new DeliveryEvidenceFreshness(false, "missing", -1);
        }
        try {
            OffsetDateTime verifiedAtTime = OffsetDateTime.parse(verifiedAt);
            OffsetDateTime now = OffsetDateTime.now();
            long ageDays = Math.max(0, Duration.between(verifiedAtTime.toInstant(), now.toInstant()).toDays());
            boolean fresh = !verifiedAtTime.isBefore(now.minusDays(resolveAcceptanceEvidenceMaxAgeDays()));
            return new DeliveryEvidenceFreshness(fresh, fresh ? "fresh" : "stale", ageDays);
        } catch (DateTimeParseException exception) {
            return new DeliveryEvidenceFreshness(false, "invalid", -1);
        }
    }

    private int resolveAcceptanceEvidenceMaxAgeDays() {
        return Math.max(1, environment.getProperty("app.delivery.acceptance-evidence-max-age-days", Integer.class, DEFAULT_ACCEPTANCE_EVIDENCE_MAX_AGE_DAYS));
    }

    private ExternalSystemConnectivitySnapshot probeHttpExternalSystem(String systemCode,
                                                                       String providerKey,
                                                                       String endpointKey) {
        return probeHttpExternalSystemResolved(
                systemCode,
                normalizedProperty(providerKey, systemCode),
                endpointKey
        );
    }

    private ExternalSystemConnectivitySnapshot probeObservabilityEndpoint(String systemCode, String endpointKey) {
        return probeHttpExternalSystemResolved(
                systemCode,
                "observability",
                endpointKey
        );
    }

    private ObservabilityStackReadinessSnapshot buildObservabilityStackReadinessSnapshot(ObservabilityStackConnectivitySnapshot snapshot,
                                                                                         ReleaseChecklistItemView checklistItem) {
        List<ExternalSystemConnectivitySnapshot> endpoints = List.of(
                snapshot.logAggregation(),
                snapshot.trace(),
                snapshot.alertRouter(),
                snapshot.dashboard()
        );
        int configuredCount = (int) endpoints.stream()
                .filter(ExternalSystemConnectivitySnapshot::configured)
                .count();
        int reachableCount = (int) endpoints.stream()
                .filter(ExternalSystemConnectivitySnapshot::reachable)
                .count();
        return new ObservabilityStackReadinessSnapshot(
                "passed".equals(checklistItem.status()),
                configuredCount,
                reachableCount,
                snapshot.logAggregation(),
                snapshot.trace(),
                snapshot.alertRouter(),
                snapshot.dashboard(),
                checklistItem.status(),
                checklistItem.detail()
        );
    }

    private ExternalSystemConnectivitySnapshot probeHttpExternalSystemDirect(String systemCode,
                                                                             String provider,
                                                                             String endpoint) {
        return probeHttpExternalSystemDirect(
                systemCode,
                provider,
                endpoint,
                "direct",
                "direct",
                false,
                true,
                resolveEndpointStatus(endpoint, false, true)
        );
    }

    private ExternalSystemConnectivitySnapshot probeHttpExternalSystemDirect(String systemCode,
                                                                             String provider,
                                                                             String endpoint,
                                                                             String sourceType,
                                                                             String sourceName,
                                                                             boolean defaultValue,
                                                                             boolean trusted,
                                                                             String status) {
        String host = extractHost(endpoint);
        String maskedTarget = maskEndpoint(endpoint);
        if (endpoint == null || endpoint.isBlank()) {
            return new ExternalSystemConnectivitySnapshot(
                    systemCode,
                    provider,
                    "http",
                    false,
                    false,
                    host,
                    maskedTarget,
                    sourceType,
                    sourceName,
                    defaultValue,
                    trusted,
                    status,
                    "endpoint not configured"
            );
        }
        try {
            Duration timeout = Duration.ofMillis(resolveExternalProbeTimeoutMillis());
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .build();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(endpoint))
                            .timeout(timeout)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            boolean reachable = response.statusCode() >= 200 && response.statusCode() < 400;
            return new ExternalSystemConnectivitySnapshot(
                    systemCode,
                    provider,
                    "http",
                    true,
                    reachable,
                    host,
                    maskedTarget,
                    sourceType,
                    sourceName,
                    defaultValue,
                    trusted,
                    status,
                    "http " + response.statusCode()
            );
        } catch (Exception exception) {
            return new ExternalSystemConnectivitySnapshot(
                    systemCode,
                    provider,
                    "http",
                    true,
                    false,
                    host,
                    maskedTarget,
                    sourceType,
                    sourceName,
                    defaultValue,
                    trusted,
                    status,
                    "probe failed: " + exception.getClass().getSimpleName()
            );
        }
    }

    private ExternalSystemConnectivitySnapshot probeTcpExternalSystem(String systemCode,
                                                                      String providerKey,
                                                                      String endpointKey) {
        DeliveryPropertySourceResolution resolution = resolveDeliveryPropertySource(endpointKey, toEnvironmentKey(endpointKey), "");
        String endpoint = resolution.value();
        String provider = normalizedProperty(providerKey, systemCode);
        String host = extractHost(endpoint);
        String maskedTarget = maskEndpoint(endpoint);
        String status = resolveEndpointStatus(endpoint, resolution.defaultValue(), resolution.trusted());
        if (endpoint == null || endpoint.isBlank()) {
            return new ExternalSystemConnectivitySnapshot(
                    systemCode,
                    provider,
                    "tcp",
                    false,
                    false,
                    host,
                    maskedTarget,
                    resolution.sourceType(),
                    resolution.sourceName(),
                    resolution.defaultValue(),
                    resolution.trusted(),
                    status,
                    "endpoint not configured"
            );
        }
        try {
            URI uri = URI.create(endpoint);
            String resolvedHost = uri.getHost();
            if (resolvedHost == null || resolvedHost.isBlank()) {
                return new ExternalSystemConnectivitySnapshot(
                        systemCode,
                        provider,
                        "tcp",
                        true,
                        false,
                        "",
                        maskedTarget,
                        resolution.sourceType(),
                        resolution.sourceName(),
                        resolution.defaultValue(),
                        resolution.trusted(),
                        status,
                        "probe failed: missing host"
                );
            }
            int port = resolveTcpProbePort(uri);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(resolvedHost, port), resolveExternalProbeTimeoutMillis());
            }
            return new ExternalSystemConnectivitySnapshot(
                    systemCode,
                    provider,
                    "tcp",
                    true,
                    true,
                    resolvedHost,
                    maskedTarget,
                    resolution.sourceType(),
                    resolution.sourceName(),
                    resolution.defaultValue(),
                    resolution.trusted(),
                    status,
                    "tcp connected"
            );
        } catch (Exception exception) {
            return new ExternalSystemConnectivitySnapshot(
                    systemCode,
                    provider,
                    "tcp",
                    true,
                    false,
                    host,
                    maskedTarget,
                    resolution.sourceType(),
                    resolution.sourceName(),
                    resolution.defaultValue(),
                    resolution.trusted(),
                    status,
                    "probe failed: " + exception.getClass().getSimpleName()
            );
        }
    }

    private boolean isDeliveryPipelineReady(ExternalSystemConnectivitySnapshot githubProbe,
                                            ExternalSystemConnectivitySnapshot registryProbe) {
        return isConfigured("app.delivery.github-owner")
                && isConfigured("app.delivery.github-repository")
                && isConfigured("app.delivery.registry")
                && isConfigured("app.delivery.image-repository")
                && environment.getProperty("app.delivery.release-key-configured", Boolean.class, false)
                && environment.getProperty("app.delivery.registry-auth-configured", Boolean.class, false)
                && !environment.getProperty("app.delivery.github-publish-mode", "github-actions").isBlank()
                && environment.getProperty("app.delivery.canary-enabled", Boolean.class, false)
                && !environment.getProperty("app.delivery.canary-strategy", "").isBlank()
                && githubProbe.reachable()
                && registryProbe.reachable();
    }

    private boolean isDeliveryPipelineReady(DeliveryAssetSnapshot workflowAsset,
                                            DeliveryAssetSnapshot standardSaasComposeAsset,
                                            DeliveryAssetSnapshot privateComposeAsset,
                                            DeliveryControlDiagnosticSnapshot releaseKeyControl,
                                            DeliveryControlDiagnosticSnapshot registryAuthControl,
                                            DeliveryControlDiagnosticSnapshot githubPublishingControl,
                                            DeliveryControlDiagnosticSnapshot canaryControl,
                                            ExternalSystemConnectivitySnapshot githubProbe,
                                            ExternalSystemConnectivitySnapshot registryProbe) {
        return isConfigured("app.delivery.github-owner")
                && isConfigured("app.delivery.github-repository")
                && isConfigured("app.delivery.registry")
                && isConfigured("app.delivery.image-repository")
                && isDeliveryControlReady(releaseKeyControl)
                && isDeliveryControlReady(registryAuthControl)
                && isDeliveryControlReady(githubPublishingControl)
                && isDeliveryControlReady(canaryControl)
                && workflowAsset.ready()
                && standardSaasComposeAsset.ready()
                && privateComposeAsset.ready()
                && githubProbe.reachable()
                && registryProbe.reachable();
    }

    private int resolveExternalProbeTimeoutMillis() {
        return Math.max(250, environment.getProperty("app.integrations.external.probe-timeout-millis", Integer.class, 1500));
    }

    private void collectDeliveryControlStatus(DeliveryControlDiagnosticSnapshot control,
                                              String label,
                                              List<String> missing,
                                              List<String> blockingReasons) {
        if (isDeliveryControlReady(control)) {
            return;
        }
        missing.add(label);
        blockingReasons.add(resolveDeliveryControlBlockingReason(control, label));
    }

    private boolean isDeliveryControlReady(DeliveryControlDiagnosticSnapshot control) {
        if (!control.configured()) {
            return false;
        }
        if ("github-publish-mode".equals(control.controlCode())
                || "release-key".equals(control.controlCode())
                || "registry-auth".equals(control.controlCode())) {
            return control.trusted();
        }
        return true;
    }

    private DeliveryControlDiagnosticSnapshot buildBooleanDeliveryControl(String controlCode,
                                                                         String propertyKey,
                                                                         String envKey,
                                                                         boolean currentValue,
                                                                         boolean requireTrustedConfiguration,
                                                                         String enabledDetail,
                                                                         String untrustedEnabledDetail,
                                                                         String defaultDisabledDetail,
                                                                         String disabledDetail) {
        DeliveryPropertySourceResolution resolution = resolveDeliveryPropertySource(propertyKey, envKey, "false");
        String status;
        String detail;
        if (currentValue) {
            if (requireTrustedConfiguration && !resolution.trusted()) {
                status = "configured-untrusted";
                detail = untrustedEnabledDetail;
            } else {
                status = "configured";
                detail = enabledDetail;
            }
        } else {
            status = resolution.defaultValue() ? "disabled-default" : "disabled";
            detail = resolution.defaultValue() ? defaultDisabledDetail : disabledDetail;
        }
        return new DeliveryControlDiagnosticSnapshot(
                controlCode,
                Boolean.toString(currentValue),
                currentValue,
                resolution.sourceType(),
                resolution.sourceName(),
                resolution.defaultValue(),
                resolution.trusted(),
                status,
                detail
        );
    }

    private DeliveryControlDiagnosticSnapshot buildGithubPublishingControl(String owner,
                                                                          String repository,
                                                                          DeliveryAssetSnapshot workflowAsset) {
        DeliveryPropertySourceResolution resolution = resolveDeliveryPropertySource(
                "app.delivery.github-publish-mode",
                "APP_DELIVERY_GITHUB_PUBLISH_MODE",
                "github-actions"
        );
        boolean configured = !resolution.value().isBlank() && !owner.isBlank() && !repository.isBlank() && workflowAsset.ready();
        String status;
        String detail;
        if (resolution.value().isBlank()) {
            status = "missing-mode";
            detail = "github publish mode is missing";
        } else if (owner.isBlank() || repository.isBlank()) {
            status = "missing-repository";
            detail = "github publish repository coordinates are incomplete";
        } else if (!workflowAsset.ready()) {
            status = "workflow-invalid";
            detail = "github publish workflow asset is missing or invalid";
        } else {
            status = "configured";
            detail = resolution.defaultValue()
                    ? "github publish mode relies on default github-actions workflow"
                    : "github publish mode is explicitly configured as " + resolution.value();
        }
        return new DeliveryControlDiagnosticSnapshot(
                "github-publish-mode",
                resolution.value(),
                configured,
                resolution.sourceType(),
                resolution.sourceName(),
                resolution.defaultValue(),
                resolution.trusted(),
                status,
                detail
        );
    }

    private DeliveryControlDiagnosticSnapshot buildCanaryControl(boolean canaryEnabled) {
        DeliveryPropertySourceResolution enabledResolution = resolveDeliveryPropertySource(
                "app.delivery.canary-enabled",
                "APP_DELIVERY_CANARY_ENABLED",
                "false"
        );
        DeliveryPropertySourceResolution strategyResolution = resolveDeliveryPropertySource(
                "app.delivery.canary-strategy",
                "APP_DELIVERY_CANARY_STRATEGY",
                ""
        );
        boolean configured = canaryEnabled && !strategyResolution.value().isBlank();
        DeliveryPropertySourceResolution effectiveResolution = configured ? strategyResolution : enabledResolution;
        String value = configured ? strategyResolution.value() : canaryEnabled ? "" : "disabled";
        String status;
        String detail;
        if (!canaryEnabled) {
            status = enabledResolution.defaultValue() ? "disabled-default" : "disabled";
            detail = enabledResolution.defaultValue()
                    ? "canary control still relies on default disabled setting"
                    : "canary control is explicitly disabled";
        } else if (strategyResolution.value().isBlank()) {
            status = "missing-strategy";
            detail = "canary strategy is enabled but no rollout strategy is configured";
        } else {
            status = "configured";
            detail = "canary rollout strategy is explicitly configured as " + strategyResolution.value();
        }
        return new DeliveryControlDiagnosticSnapshot(
                "canary-strategy",
                value,
                configured,
                effectiveResolution.sourceType(),
                effectiveResolution.sourceName(),
                effectiveResolution.defaultValue(),
                effectiveResolution.trusted(),
                status,
                detail
        );
    }

    private DeliveryPropertySourceResolution resolveDeliveryPropertySource(String propertyKey,
                                                                           String envKey,
                                                                           String defaultValue) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            String environmentValue = findEnvironmentVariableValue(configurableEnvironment, envKey);
            if (environmentValue != null) {
                return new DeliveryPropertySourceResolution(environmentValue, "environment", envKey, false, true);
            }
            PropertySourceMatch match = findExplicitPropertySource(configurableEnvironment, propertyKey);
            if (match != null) {
                return new DeliveryPropertySourceResolution(
                        match.value(),
                        classifyPropertySource(match.sourceName()),
                        match.sourceName(),
                        false,
                        true
                );
            }
        }
        String resolvedValue = environment.getProperty(propertyKey, defaultValue);
        if (defaultValue.equals(resolvedValue)) {
            return new DeliveryPropertySourceResolution(resolvedValue, "default", envKey, true, false);
        }
        return new DeliveryPropertySourceResolution(resolvedValue == null ? "" : resolvedValue, "resolved", propertyKey, false, false);
    }

    private String findEnvironmentVariableValue(ConfigurableEnvironment configurableEnvironment, String envKey) {
        PropertySource<?> systemEnvironment = configurableEnvironment.getPropertySources().get("systemEnvironment");
        if (systemEnvironment == null || !systemEnvironment.containsProperty(envKey)) {
            return null;
        }
        Object rawValue = systemEnvironment.getProperty(envKey);
        if (rawValue == null) {
            return null;
        }
        String normalizedValue = normalizePropertyValue(rawValue);
        return normalizedValue.isBlank() ? null : normalizedValue;
    }

    private PropertySourceMatch findExplicitPropertySource(ConfigurableEnvironment configurableEnvironment, String propertyKey) {
        for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
            String sourceName = propertySource.getName();
            if (sourceName == null
                    || sourceName.contains("configurationProperties")
                    || sourceName.startsWith("systemEnvironment")) {
                continue;
            }
            if (!propertySource.containsProperty(propertyKey)) {
                continue;
            }
            Object rawValue = propertySource.getProperty(propertyKey);
            if (rawValue == null) {
                continue;
            }
            String normalizedValue = normalizePropertyValue(rawValue);
            if (looksLikePlaceholderValue(normalizedValue)) {
                return null;
            }
            if (containsPlaceholderToken(normalizedValue)) {
                String resolvedValue = configurableEnvironment.getProperty(propertyKey, "");
                if (resolvedValue == null || resolvedValue.isBlank() || containsPlaceholderToken(resolvedValue)) {
                    return null;
                }
                return new PropertySourceMatch(sourceName, resolvedValue);
            }
            return new PropertySourceMatch(sourceName, normalizedValue);
        }
        return null;
    }

    private String classifyPropertySource(String sourceName) {
        String normalizedName = sourceName == null ? "" : sourceName.toLowerCase(Locale.ROOT);
        if (normalizedName.contains("dynamic") || normalizedName.contains("inline") || normalizedName.contains("test") || normalizedName.contains("mappropertysource")) {
            return "override";
        }
        if (normalizedName.contains("systemproperties")) {
            return "system-property";
        }
        if (normalizedName.contains("applicationconfig")) {
            return "application";
        }
        return "property-source";
    }

    private String normalizePropertyValue(Object rawValue) {
        return rawValue == null ? "" : rawValue.toString().trim();
    }

    private boolean looksLikePlaceholderValue(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }

    private boolean containsPlaceholderToken(String value) {
        return value != null && value.contains("${") && value.contains("}");
    }

    private String toEnvironmentKey(String propertyKey) {
        return propertyKey == null
                ? ""
                : propertyKey
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    private String resolveEndpointStatus(String endpoint, boolean defaultValue, boolean trusted) {
        if (endpoint == null || endpoint.isBlank()) {
            return defaultValue ? "missing-default" : "missing";
        }
        return trusted ? "configured" : "configured-untrusted";
    }

    private ExternalSystemConnectivitySnapshot probeHttpExternalSystemResolved(String systemCode,
                                                                               String provider,
                                                                               String endpointKey) {
        DeliveryPropertySourceResolution resolution = resolveDeliveryPropertySource(endpointKey, toEnvironmentKey(endpointKey), "");
        return probeHttpExternalSystemDirect(
                systemCode,
                provider,
                resolution.value(),
                resolution.sourceType(),
                resolution.sourceName(),
                resolution.defaultValue(),
                resolution.trusted(),
                resolveEndpointStatus(resolution.value(), resolution.defaultValue(), resolution.trusted())
        );
    }

    private void collectObservabilityEndpointStatus(ExternalSystemConnectivitySnapshot snapshot,
                                                    String label,
                                                    List<String> missing,
                                                    List<String> untrusted,
                                                    List<String> unreachable,
                                                    List<String> blockingReasons) {
        if (!snapshot.configured()) {
            missing.add(label);
            blockingReasons.add("observability stack is missing " + label);
            return;
        }
        if (!snapshot.trusted()) {
            untrusted.add(label);
            blockingReasons.add("observability stack " + label + " must be explicitly configured");
            return;
        }
        if (!snapshot.reachable()) {
            unreachable.add(label);
            blockingReasons.add("observability stack " + label + " probe is unreachable");
        }
    }

    private void collectDeliveryPipelineProbeStatus(ExternalSystemConnectivitySnapshot snapshot,
                                                    String missingLabel,
                                                    String unreachableLabel,
                                                    List<String> missing,
                                                    List<String> unreachable,
                                                    List<String> blockingReasons) {
        if (!snapshot.configured()) {
            missing.add(missingLabel);
            blockingReasons.add("delivery pipeline is missing " + missingLabel);
            return;
        }
        if (!snapshot.reachable()) {
            unreachable.add(unreachableLabel);
            blockingReasons.add("delivery pipeline " + unreachableLabel + " is unreachable");
        }
    }

    private void collectDeliveryAssetStatus(DeliveryAssetSnapshot asset,
                                            String label,
                                            List<String> missing,
                                            List<String> blockingReasons) {
        if (asset.ready()) {
            return;
        }
        missing.add(label);
        blockingReasons.add("delivery pipeline " + label + " is missing or invalid");
    }

    private int countReachableObservabilityEndpoints(ObservabilityStackConnectivitySnapshot snapshot) {
        return (int) List.of(
                        snapshot.logAggregation(),
                        snapshot.trace(),
                        snapshot.alertRouter(),
                        snapshot.dashboard()
                ).stream()
                .filter(ExternalSystemConnectivitySnapshot::reachable)
                .count();
    }

    private String buildObservabilityControlSummary(ObservabilityStackConnectivitySnapshot snapshot) {
        return String.join(", ", List.of(
                formatEndpointControlSummary(snapshot.logAggregation()),
                formatEndpointControlSummary(snapshot.trace()),
                formatEndpointControlSummary(snapshot.alertRouter()),
                formatEndpointControlSummary(snapshot.dashboard())
        ));
    }

    private String formatEndpointControlSummary(ExternalSystemConnectivitySnapshot snapshot) {
        return snapshot.systemCode() + "=" + snapshot.status() + "@" + snapshot.sourceType();
    }

    private int countReachableDeliveryPipelineProbes(DeliveryPipelineSnapshot snapshot) {
        return (int) List.of(snapshot.githubProbe(), snapshot.registryProbe()).stream()
                .filter(ExternalSystemConnectivitySnapshot::reachable)
                .count();
    }

    private long countFreshDeliveryEndpoints(DeliveryEndpointSnapshot standardSaas,
                                             DeliveryEndpointSnapshot privateDeployment) {
        return List.of(standardSaas, privateDeployment).stream()
                .filter(DeliveryEndpointSnapshot::verificationFresh)
                .count();
    }

    private long countReachableDeliveryEndpoints(DeliveryEndpointSnapshot... snapshots) {
        return Arrays.stream(snapshots)
                .filter(DeliveryEndpointSnapshot::configured)
                .filter(DeliveryEndpointSnapshot::reachable)
                .count();
    }

    private String buildDeliveryControlSummary(DeliveryPipelineSnapshot snapshot) {
        return String.join(", ", List.of(
                formatDeliveryControlSummary(snapshot.releaseKeyControl()),
                formatDeliveryControlSummary(snapshot.registryAuthControl()),
                formatDeliveryControlSummary(snapshot.githubPublishingControl()),
                formatDeliveryControlSummary(snapshot.canaryControl())
        ));
    }

    private String formatDeliveryControlSummary(DeliveryControlDiagnosticSnapshot snapshot) {
        return snapshot.controlCode() + "=" + snapshot.status() + "@" + snapshot.sourceType();
    }

    private String resolveDeliveryControlBlockingReason(DeliveryControlDiagnosticSnapshot snapshot, String label) {
        if ("release-key".equals(snapshot.controlCode()) && !snapshot.trusted()) {
            return "delivery pipeline release key injection must be explicitly configured";
        }
        if ("release-key".equals(snapshot.controlCode())) {
            return "delivery pipeline is missing release key injection";
        }
        if ("registry-auth".equals(snapshot.controlCode()) && !snapshot.trusted()) {
            return "delivery pipeline container registry publish credentials must be explicitly configured";
        }
        if ("registry-auth".equals(snapshot.controlCode())) {
            return "delivery pipeline is missing container registry publish credentials";
        }
        if ("canary-strategy".equals(snapshot.controlCode()) && "missing-strategy".equals(snapshot.status())) {
            return "delivery pipeline canary strategy is missing";
        }
        if ("canary-strategy".equals(snapshot.controlCode())) {
            return "delivery pipeline canary strategy is disabled";
        }
        if ("github-publish-mode".equals(snapshot.controlCode()) && "workflow-invalid".equals(snapshot.status())) {
            return "delivery pipeline github publish workflow asset is missing or invalid";
        }
        if ("github-publish-mode".equals(snapshot.controlCode()) && !snapshot.trusted()) {
            return "delivery pipeline github publish mode must be explicitly configured";
        }
        return "delivery pipeline is missing " + label;
    }

    private void collectAcceptanceVerificationStatus(DeliveryEndpointSnapshot endpoint,
                                                     String mode,
                                                     List<String> stale,
                                                     List<String> invalid,
                                                     List<String> blockingReasons) {
        if (!endpoint.configured()) {
            return;
        }
        if ("invalid".equals(endpoint.verificationStatus())) {
            invalid.add(mode + " verification timestamp");
            blockingReasons.add("dual delivery acceptance " + mode + " verification timestamp is invalid");
            return;
        }
        if (!endpoint.verificationFresh()) {
            stale.add(mode + " verification evidence");
            blockingReasons.add("dual delivery acceptance " + mode + " verification evidence is stale");
        }
    }

    private String buildConnectivityProbeSummary(ExternalIntegrationConnectivitySnapshot snapshot, List<String> requiredSystems) {
        List<ExternalSystemConnectivitySnapshot> probeTargets = requiredSystems.stream()
                .map(systemCode -> findConnectivitySnapshot(snapshot, systemCode))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (probeTargets.isEmpty()) {
            return "probe not required";
        }
        long reachable = probeTargets.stream().filter(ExternalSystemConnectivitySnapshot::reachable).count();
        return "probe reachable " + reachable + "/" + probeTargets.size();
    }

    private boolean hasTraceabilityEvidence(com.dianshang.platform.audit.AuditLogRecord auditLogRecord) {
        return auditLogRecord != null
                && auditLogRecord.traceId() != null
                && !auditLogRecord.traceId().isBlank()
                && auditLogRecord.operatorId() != null
                && !auditLogRecord.operatorId().isBlank()
                && auditLogRecord.targetId() != null
                && !auditLogRecord.targetId().isBlank()
                && auditLogRecord.actionType() != null
                && !auditLogRecord.actionType().isBlank()
                && auditLogRecord.createdAt() != null;
    }

    private String resolveReleaseConclusion(List<ReleaseChecklistItemView> checklistItems) {
        if (checklistItems.stream().anyMatch(item -> "blocked".equals(item.status()))) {
            return "reject_release";
        }
        if (checklistItems.stream().anyMatch(item -> "pending".equals(item.status()))) {
            return "conditional_release";
        }
        return "allow_release";
    }

    private List<String> resolveRequiredExternalSystems() {
        return Arrays.stream(environment.getProperty("app.integrations.external.required-systems", "erp,wms,tax,messaging,bi").split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String buildExternalIntegrationReadyDetail(String readyDetail,
                                                       ExternalIntegrationConnectivitySnapshot connectivitySnapshot,
                                                       List<String> requiredSystems) {
        String detail = readyDetail + " integrations expose endpoint, credentials and callback readiness; "
                + buildConnectivityProbeSummary(connectivitySnapshot, requiredSystems);
        if (requiredSystems.contains("messaging")) {
            ExternalSystemConnectivitySnapshot callbackWorkerConnectivity = probeMessagingCallbackWorker();
            detail = detail + "; callback worker ready; callback worker probe " + callbackWorkerConnectivity.detail();
        }
        return detail;
    }

    private ExternalSystemReadiness evaluateExternalSystem(String systemCode) {
        String prefix = "app.integrations.external.systems." + systemCode + ".";
        List<String> missingParts = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>();
        String label = buildExternalSystemDisplayName(systemCode) + " integration";
        collectRequiredTextConfiguration(
                label,
                "endpoint",
                resolveDeliveryPropertySource(prefix + "endpoint", toEnvironmentKey(prefix + "endpoint"), ""),
                missingParts,
                blockingReasons
        );
        collectRequiredBooleanConfiguration(
                label,
                "credentials",
                resolveDeliveryPropertySource(prefix + "credential-configured", toEnvironmentKey(prefix + "credential-configured"), "false"),
                missingParts,
                blockingReasons
        );
        boolean callbackRequired = environment.getProperty(prefix + "callback-required", Boolean.class, false);
        if (callbackRequired) {
            collectRequiredTextConfiguration(
                    label,
                    "callback url",
                    resolveDeliveryPropertySource(prefix + "callback-url", toEnvironmentKey(prefix + "callback-url"), ""),
                    missingParts,
                    blockingReasons
            );
        }
        if ("messaging".equals(systemCode)) {
            ExternalSystemReadiness callbackWorkerReadiness = evaluateMessagingCallbackWorker();
            if (!callbackWorkerReadiness.ready()) {
                missingParts.addAll(callbackWorkerReadiness.missingParts());
                blockingReasons.addAll(callbackWorkerReadiness.blockingReasons());
            }
        }
        return new ExternalSystemReadiness(systemCode, missingParts.isEmpty(), List.copyOf(missingParts), List.copyOf(blockingReasons));
    }

    private ExternalSystemReadiness evaluateMessagingCallbackWorker() {
        List<String> missingParts = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>();
        boolean callbackBridgeEnabled = environment.getProperty("app.integrations.external.messaging.callback-bridge-enabled", Boolean.class, false);
        if (!callbackBridgeEnabled) {
            return new ExternalSystemReadiness("callback-worker", true, List.of(), List.of());
        }
        String label = buildExternalSystemDisplayName("messaging") + " callback worker";
        collectRequiredBooleanConfiguration(
                label,
                "enabled flag",
                resolveDeliveryPropertySource(
                        "app.integrations.external.messaging.callback-worker-enabled",
                        toEnvironmentKey("app.integrations.external.messaging.callback-worker-enabled"),
                        "false"
                ),
                missingParts,
                blockingReasons
        );
        collectRequiredTextConfiguration(
                label,
                "endpoint",
                resolveDeliveryPropertySource(
                        "app.integrations.external.messaging.callback-worker-endpoint",
                        toEnvironmentKey("app.integrations.external.messaging.callback-worker-endpoint"),
                        ""
                ),
                missingParts,
                blockingReasons
        );
        collectRequiredTextConfiguration(
                label,
                "consumer group",
                resolveDeliveryPropertySource(
                        "app.integrations.external.messaging.callback-worker-consumer-group",
                        toEnvironmentKey("app.integrations.external.messaging.callback-worker-consumer-group"),
                        ""
                ),
                missingParts,
                blockingReasons
        );
        return new ExternalSystemReadiness("callback-worker", missingParts.isEmpty(), List.copyOf(missingParts), List.copyOf(blockingReasons));
    }

    private ExternalSystemConnectivitySnapshot probeMessagingCallbackWorker() {
        return probeHttpExternalEndpoint(
                "callback-worker",
                normalizedProperty("app.integrations.external.messaging.callback-worker-provider", "spring-event"),
                environment.getProperty("app.integrations.external.messaging.callback-worker-endpoint", "")
        );
    }

    private String buildExternalSystemDisplayName(String systemCode) {
        String providerKey = switch (systemCode) {
            case "erp" -> "app.integrations.external.erp.provider";
            case "wms" -> "app.integrations.external.wms.provider";
            case "messaging" -> "app.integrations.external.messaging.provider";
            case "bi" -> "app.integrations.external.bi.provider";
            case "routing" -> "app.integrations.external.routing.provider";
            default -> "";
        };
        if (providerKey.isBlank()) {
            return systemCode;
        }
        String provider = environment.getProperty(providerKey, "");
        if (provider == null || provider.isBlank()) {
            return systemCode;
        }
        return systemCode + "(" + provider.trim() + ")";
    }

    private boolean isConfigured(String key) {
        String value = environment.getProperty(key, "");
        return value != null && !value.isBlank();
    }

    private String normalizedProperty(String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private void collectRequiredBooleanConfiguration(String label,
                                                     String requirement,
                                                     DeliveryPropertySourceResolution resolution,
                                                     List<String> missingParts,
                                                     List<String> blockingReasons) {
        if (!Boolean.parseBoolean(resolution.value())) {
            missingParts.add(requirement);
            blockingReasons.add(label + " is missing " + requirement);
            return;
        }
        if (!resolution.trusted()) {
            missingParts.add(requirement + " explicit configuration");
            blockingReasons.add(label + " " + requirement + " must be explicitly configured");
        }
    }

    private void collectRequiredTextConfiguration(String label,
                                                  String requirement,
                                                  DeliveryPropertySourceResolution resolution,
                                                  List<String> missingParts,
                                                  List<String> blockingReasons) {
        if (resolution.value() == null || resolution.value().isBlank()) {
            missingParts.add(requirement);
            blockingReasons.add(label + " is missing " + requirement);
            return;
        }
        if (!resolution.trusted()) {
            missingParts.add(requirement + " explicit configuration");
            blockingReasons.add(label + " " + requirement + " must be explicitly configured");
        }
    }

    private String extractHost(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String maskEndpoint(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            String host = uri.getHost() == null ? "" : uri.getHost();
            String authority = host;
            if (uri.getPort() >= 0) {
                authority = authority + ":" + uri.getPort();
            }
            if (scheme.isBlank() || authority.isBlank()) {
                return "***";
            }
            return scheme + "://" + authority + "/***";
        } catch (IllegalArgumentException exception) {
            return "***";
        }
    }

    private int resolveTcpProbePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        return switch (scheme) {
            case "amqps" -> 5671;
            case "amqp" -> 5672;
            default -> 5672;
        };
    }

    private record ExternalSystemReadiness(
            String systemCode,
            boolean ready,
            List<String> missingParts,
            List<String> blockingReasons
    ) {
    }

    private DataExportScope requireExportScope(String scopeCode) {
        return switch (scopeCode) {
            case "tenant_profile" -> new DataExportScope("tenant_profile", "租户档案摘要", "application/json", ".json");
            case "subscription_summary" -> new DataExportScope("subscription_summary", "订阅与配额摘要", "application/json", ".json");
            case "tenant_audit_logs" -> new DataExportScope("tenant_audit_logs", "租户审计日志", "text/csv;charset=UTF-8", ".csv");
            case "compliance_acceptances" -> new DataExportScope("compliance_acceptances", "协议确认记录", "application/json", ".json");
            case "orders_bundle" -> new DataExportScope("orders_bundle", "订单经营数据", "application/json", ".json");
            case "product_catalog" -> new DataExportScope("product_catalog", "商品经营数据", "application/json", ".json");
            case "inventory_bundle" -> new DataExportScope("inventory_bundle", "库存与补货数据", "application/json", ".json");
            case "member_bundle" -> new DataExportScope("member_bundle", "会员经营数据", "application/json", ".json");
            default -> throw new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND);
        };
    }

    private String buildExportContent(String tenantId, DataExportScope scope) {
        List<Store> stores = storeRepository.findByTenantId(tenantId);
        List<String> storeIds = stores.stream().map(Store::storeId).toList();
        try {
            return switch (scope.scopeCode()) {
                case "tenant_profile" -> objectMapper.writeValueAsString(Map.of(
                        "tenantProfile", requireTenantProfile(tenantId),
                        "subscription", requireSubscription(tenantId)
                ));
                case "subscription_summary" -> objectMapper.writeValueAsString(Map.of(
                        "tenantId", tenantId,
                        "subscription", requireSubscription(tenantId),
                        "quotas", getQuotas(tenantId)
                ));
                case "tenant_audit_logs" -> buildAuditCsv(tenantId);
                case "compliance_acceptances" -> objectMapper.writeValueAsString(listComplianceAcceptances(tenantId));
                case "orders_bundle" -> {
                    List<OrderMain> orders = storeIds.isEmpty() ? List.of() : orderRepository.findByStoreIds(storeIds);
                    Map<String, List<?>> orderItems = new LinkedHashMap<>();
                    for (OrderMain order : orders) {
                        orderItems.put(order.orderId(), orderItemRepository.findByOrderId(order.orderId()));
                    }
                    yield objectMapper.writeValueAsString(Map.of(
                            "stores", stores,
                            "orders", orders,
                            "orderItems", orderItems
                    ));
                }
                case "product_catalog" -> objectMapper.writeValueAsString(Map.of(
                        "stores", stores,
                        "products", storeIds.isEmpty() ? List.of() : productRepository.findByStoreIds(storeIds)
                ));
                case "inventory_bundle" -> objectMapper.writeValueAsString(Map.of(
                        "stores", stores,
                        "inventorySnapshots", storeIds.isEmpty() ? List.of() : inventorySnapshotRepository.findByStoreIds(storeIds),
                        "replenishmentTasks", storeIds.isEmpty() ? List.of() : replenishmentTaskRepository.findByStoreIds(storeIds)
                ));
                case "member_bundle" -> objectMapper.writeValueAsString(Map.of(
                        "stores", stores,
                        "memberProfiles", storeIds.isEmpty() ? List.of() : memberProfileRepository.findByStoreIds(storeIds),
                        "memberTags", storeIds.isEmpty() ? List.of() : memberTagRepository.findByStoreIds(storeIds)
                ));
                default -> throw new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND);
            };
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize export content", exception);
        }
    }

    private String buildAuditCsv(String tenantId) {
        StringBuilder builder = new StringBuilder("tenantId,operatorId,operatorType,actionType,targetType,targetId,traceId,createdAt\n");
        auditLogService.findByTenantId(tenantId).forEach(log -> builder.append(csv(log.tenantId())).append(',')
                .append(csv(log.operatorId())).append(',')
                .append(csv(log.operatorType())).append(',')
                .append(csv(log.actionType())).append(',')
                .append(csv(log.targetType())).append(',')
                .append(csv(log.targetId())).append(',')
                .append(csv(log.traceId())).append(',')
                .append(csv(log.createdAt() == null ? "" : log.createdAt().toString()))
                .append('\n'));
        return builder.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private Map<String, Integer> buildCleanupImpactSummary(String tenantId, Set<String> cleanupScopes) {
        List<Store> stores = storeRepository.findByTenantId(tenantId);
        List<Long> storeDbIds = stores.stream().map(store -> JdbcIdCodec.parseStoreId(store.storeId())).toList();
        List<Long> organizationDbIds = stores.stream()
                .map(store -> JdbcIdCodec.parseOrganizationId(store.organizationId()))
                .distinct()
                .toList();

        Map<String, Integer> summary = new LinkedHashMap<>();
        if (cleanupCoversAllOperationalScopes(cleanupScopes)) {
            summary.put("storeCount", stores.size());
            summary.put("channelAccountCount", countByOrganizationIds("channel_account", organizationDbIds));
        }
        if (cleanupScopes.contains(CLEANUP_SCOPE_PRODUCTS)) {
            summary.put("candidateProductCount", countByStoreIds("candidate_product", storeDbIds));
            summary.put("productDraftCount", countByStoreIds("product_draft", storeDbIds));
            summary.put("productCount", countByStoreIds("product", storeDbIds));
            summary.put("supplierCount", countByStoreIds("supplier", storeDbIds));
            summary.put("productMappingCount", countByStoreIds("product_source_mapping", storeDbIds));
        }
        if (cleanupScopes.contains(CLEANUP_SCOPE_INVENTORY)) {
            summary.put("inventorySnapshotCount", countByStoreIds("inventory_snapshot", storeDbIds));
            summary.put("replenishmentTaskCount", countByStoreIds("replenishment_task", storeDbIds));
        }
        if (cleanupScopes.contains(CLEANUP_SCOPE_ORDERS)) {
            summary.put("orderCount", countByStoreIds("order_main", storeDbIds));
            summary.put("orderItemCount", countOrderItemsByStoreIds(storeDbIds));
            summary.put("fulfillmentTaskCount", countByStoreIds("fulfillment_task", storeDbIds));
            summary.put("logisticsRecordCount", countLogisticsByStoreIds(storeDbIds));
        }
        if (cleanupScopes.contains(CLEANUP_SCOPE_MEMBERS)) {
            summary.put("memberProfileCount", countByStoreIds("member_profile", storeDbIds));
            summary.put("memberTagCount", countByStoreIds("member_tag", storeDbIds));
        }
        return summary;
    }

    private Map<String, Integer> executeTenantCleanupInternal(String tenantId, Set<String> cleanupScopes) {
        List<Store> stores = storeRepository.findByTenantId(tenantId);
        List<Long> storeDbIds = stores.stream().map(store -> JdbcIdCodec.parseStoreId(store.storeId())).toList();
        List<Long> organizationDbIds = stores.stream()
                .map(store -> JdbcIdCodec.parseOrganizationId(store.organizationId()))
                .distinct()
                .toList();

        Map<String, Integer> deleted = new LinkedHashMap<>();
        if (cleanupScopes.contains(CLEANUP_SCOPE_MEMBERS)) {
            deleted.put("memberTagCount", deleteByStoreIds("member_tag", storeDbIds));
            deleted.put("memberProfileCount", deleteByStoreIds("member_profile", storeDbIds));
        }
        if (cleanupScopes.contains(CLEANUP_SCOPE_ORDERS)) {
            deleted.put("logisticsRecordCount", deleteLogisticsByStoreIds(storeDbIds));
            deleted.put("fulfillmentTaskCount", deleteByStoreIds("fulfillment_task", storeDbIds));
            deleted.put("orderItemCount", deleteOrderItemsByStoreIds(storeDbIds));
            deleted.put("orderCount", deleteByStoreIds("order_main", storeDbIds));
        }
        if (cleanupScopes.contains(CLEANUP_SCOPE_INVENTORY)) {
            deleted.put("replenishmentTaskCount", deleteByStoreIds("replenishment_task", storeDbIds));
            deleted.put("inventorySnapshotCount", deleteByStoreIds("inventory_snapshot", storeDbIds));
        }
        if (cleanupScopes.contains(CLEANUP_SCOPE_PRODUCTS)) {
            deleted.put("productMappingCount", deleteByStoreIds("product_source_mapping", storeDbIds));
            deleted.put("supplierCount", deleteByStoreIds("supplier", storeDbIds));
            deleted.put("productCount", deleteByStoreIds("product", storeDbIds));
            deleted.put("productDraftCount", deleteByStoreIds("product_draft", storeDbIds));
            deleted.put("candidateProductCount", deleteByStoreIds("candidate_product", storeDbIds));
        }
        if (cleanupCoversAllOperationalScopes(cleanupScopes)) {
            deleted.put("channelAccountCount", deleteByOrganizationIds("channel_account", organizationDbIds));
            deleted.put("storeCount", deleteStoresByTenantId(tenantId));
        }
        return deleted;
    }

    private List<String> normalizeCleanupScopes(List<String> cleanupScopes) {
        if (cleanupScopes == null || cleanupScopes.isEmpty()) {
            return DEFAULT_CLEANUP_SCOPES;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String cleanupScope : cleanupScopes) {
            if (!DEFAULT_CLEANUP_SCOPES.contains(cleanupScope)) {
                throw new BusinessException("1008", "unsupported cleanup scope", HttpStatus.BAD_REQUEST);
            }
            normalized.add(cleanupScope);
        }
        if (normalized.isEmpty()) {
            throw new BusinessException("1008", "cleanup scopes must not be empty", HttpStatus.BAD_REQUEST);
        }
        return List.copyOf(normalized);
    }

    private boolean cleanupCoversAllOperationalScopes(Set<String> cleanupScopes) {
        return cleanupScopes.size() == DEFAULT_CLEANUP_SCOPES.size()
                && cleanupScopes.containsAll(DEFAULT_CLEANUP_SCOPES);
    }

    private int countByStoreIds(String tableName, List<Long> storeDbIds) {
        if (storeDbIds.isEmpty()) {
            return 0;
        }
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE store_id IN (" + placeholders(storeDbIds.size()) + ")",
                Integer.class,
                storeDbIds.toArray()
        );
        return value == null ? 0 : value;
    }

    private int countByOrganizationIds(String tableName, List<Long> organizationDbIds) {
        if (organizationDbIds.isEmpty()) {
            return 0;
        }
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE organization_id IN (" + placeholders(organizationDbIds.size()) + ")",
                Integer.class,
                organizationDbIds.toArray()
        );
        return value == null ? 0 : value;
    }

    private int countOrderItemsByStoreIds(List<Long> storeDbIds) {
        if (storeDbIds.isEmpty()) {
            return 0;
        }
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_item WHERE order_id IN (SELECT id FROM order_main WHERE store_id IN (" + placeholders(storeDbIds.size()) + "))",
                Integer.class,
                storeDbIds.toArray()
        );
        return value == null ? 0 : value;
    }

    private int countLogisticsByStoreIds(List<Long> storeDbIds) {
        if (storeDbIds.isEmpty()) {
            return 0;
        }
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logistics_record WHERE fulfillment_task_id IN (SELECT id FROM fulfillment_task WHERE store_id IN (" + placeholders(storeDbIds.size()) + "))",
                Integer.class,
                storeDbIds.toArray()
        );
        return value == null ? 0 : value;
    }

    private int deleteByStoreIds(String tableName, List<Long> storeDbIds) {
        if (storeDbIds.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update(
                "DELETE FROM " + tableName + " WHERE store_id IN (" + placeholders(storeDbIds.size()) + ")",
                storeDbIds.toArray()
        );
    }

    private int deleteByOrganizationIds(String tableName, List<Long> organizationDbIds) {
        if (organizationDbIds.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update(
                "DELETE FROM " + tableName + " WHERE organization_id IN (" + placeholders(organizationDbIds.size()) + ")",
                organizationDbIds.toArray()
        );
    }

    private int deleteOrderItemsByStoreIds(List<Long> storeDbIds) {
        if (storeDbIds.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update(
                "DELETE FROM order_item WHERE order_id IN (SELECT id FROM order_main WHERE store_id IN (" + placeholders(storeDbIds.size()) + "))",
                storeDbIds.toArray()
        );
    }

    private int deleteLogisticsByStoreIds(List<Long> storeDbIds) {
        if (storeDbIds.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update(
                "DELETE FROM logistics_record WHERE fulfillment_task_id IN (SELECT id FROM fulfillment_task WHERE store_id IN (" + placeholders(storeDbIds.size()) + "))",
                storeDbIds.toArray()
        );
    }

    private int deleteStoresByTenantId(String tenantId) {
        return jdbcTemplate.update("DELETE FROM store WHERE tenant_id = ?", JdbcIdCodec.parseTenantId(tenantId));
    }

    private String placeholders(int size) {
        return String.join(", ", Collections.nCopies(size, "?"));
    }

    private ComplianceDocument requireComplianceDocument(String documentCode) {
        ComplianceDocument document = complianceDocuments.get(documentCode);
        if (document == null) {
            throw new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND);
        }
        return document;
    }

    private String actionTypeForDocument(String documentCode) {
        return switch (documentCode) {
            case "privacy-policy" -> "ACCEPT_PRIVACY_POLICY";
            case "user-agreement" -> "ACCEPT_USER_AGREEMENT";
            default -> throw new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND);
        };
    }

    private String currentOperatorId() {
        TenantContext context = TenantContextHolder.get();
        if (context == null || context.operatorId() == null || context.operatorId().isBlank()) {
            return "system";
        }
        return context.operatorId();
    }

    private Map<String, ComplianceAcceptanceRecord> latestComplianceAcceptancesByDocument(String tenantId) {
        Map<String, ComplianceAcceptanceRecord> latest = new LinkedHashMap<>();
        for (ComplianceAcceptanceRecord record : complianceAcceptanceRepository.findByTenantId(tenantId)) {
            latest.putIfAbsent(record.documentCode(), record);
        }
        return latest;
    }

    private AdminComplianceAcceptanceView toAdminComplianceAcceptanceView(TenantProfile profile, ComplianceDocument document) {
        ComplianceAcceptanceRecord latestAcceptance = latestComplianceAcceptancesByDocument(profile.tenantId())
                .get(document.documentCode());
        String acceptanceStatus = resolveAcceptanceStatus(document.version(), latestAcceptance);
        return new AdminComplianceAcceptanceView(
                profile.tenantId(),
                profile.tenantName(),
                profile.tenantStatus(),
                document.documentCode(),
                document.version(),
                latestAcceptance == null ? null : latestAcceptance.documentVersion(),
                latestAcceptance == null ? null : latestAcceptance.acceptedBy(),
                latestAcceptance == null ? null : latestAcceptance.acceptedAt(),
                acceptanceStatus,
                ACCEPTANCE_STATUS_PENDING_REACCEPTANCE.equals(acceptanceStatus)
        );
    }

    private String resolveAcceptanceStatus(String currentVersion, ComplianceAcceptanceRecord latestAcceptance) {
        if (latestAcceptance == null) {
            return ACCEPTANCE_STATUS_NEVER_ACCEPTED;
        }
        if (currentVersion.equals(latestAcceptance.documentVersion())) {
            return ACCEPTANCE_STATUS_ACCEPTED_CURRENT_VERSION;
        }
        return ACCEPTANCE_STATUS_PENDING_REACCEPTANCE;
    }

    private void validateAcceptanceStatusFilter(String acceptanceStatus) {
        if (acceptanceStatus == null || acceptanceStatus.isBlank()) {
            return;
        }
        if (ACCEPTANCE_STATUS_ACCEPTED_CURRENT_VERSION.equals(acceptanceStatus)
                || ACCEPTANCE_STATUS_PENDING_REACCEPTANCE.equals(acceptanceStatus)
                || ACCEPTANCE_STATUS_NEVER_ACCEPTED.equals(acceptanceStatus)) {
            return;
        }
        throw new BusinessException("1010", "不支持的协议确认状态筛选值", HttpStatus.BAD_REQUEST);
    }

    private String toJson(Map<String, Integer> value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize json", exception);
        }
    }

    private String toJson(List<String> value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize json", exception);
        }
    }

    private Map<String, Integer> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to parse json", exception);
        }
    }

    private List<String> fromJsonList(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_CLEANUP_SCOPES;
        }
        try {
            return normalizeCleanupScopes(objectMapper.readValue(value, new TypeReference<>() {
            }));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to parse json", exception);
        }
    }

    private record DataExportScope(
            String scopeCode,
            String scopeName,
            String contentType,
            String fileSuffix
    ) {
        private String defaultFileName(String tenantId) {
            return scopeCode + "-" + tenantId + fileSuffix;
        }
    }
}
