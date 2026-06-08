package com.dianshang.platform.admin;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.application.AuthService;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.saas.domain.repository.BillingOrderRepository;
import com.dianshang.platform.saas.domain.repository.TenantProfileRepository;
import com.dianshang.platform.saas.domain.repository.TenantSubscriptionRepository;
import com.dianshang.platform.saas.model.TenantProfile;
import com.dianshang.platform.saas.model.TenantSubscription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.release.automation-evidence.suite-count=3",
        "app.release.automation-evidence.environment=server-local",
        "app.release.automation-evidence.executed-at=2026-06-08T09:59:30+08:00",
        "app.release.automation-evidence.summary=auth/openplatform/compliance regression suites passed"
})
@AutoConfigureMockMvc
class AdminTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Autowired
    private TenantProfileRepository tenantProfileRepository;

    @Autowired
    private BillingOrderRepository billingOrderRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
        authService.clearSensitiveOperationConfirmations();
    }

    @Test
    void shouldStartTrialSuspendAndResumeTenant() throws Exception {
        String tenantId = registerTenant();

        mockMvc.perform(post("/api/tenants/{id}/start-trial", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("trial"));

        mockMvc.perform(post("/api/admin/tenants/{id}/suspend", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("suspended"));

        mockMvc.perform(post("/api/admin/tenants/{id}/resume", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("trial"));

        mockMvc.perform(put("/api/admin/tenants/{id}/feature-toggles", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureFlags": {
                                    "ai_copilot": true,
                                    "live_module": false
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureFlags.ai_copilot").value(true))
                .andExpect(jsonPath("$.data.featureFlags.live_module").value(false));
    }

    @Test
    void shouldReconcileSubscriptionAutomationForAutoRenewAndExpiredSuspend() throws Exception {
        String autoRenewTenantId = registerTenant("tenant-auto-renew", "13800000001");
        String expiredSuspendTenantId = registerTenant("tenant-expired-suspend", "13800000002");

        subscribeTenant(autoRenewTenantId, "13800000001", true);
        subscribeTenant(expiredSuspendTenantId, "13800000002", false);

        expireSubscription(autoRenewTenantId, "active");
        expireSubscription(expiredSuspendTenantId, "active");

        mockMvc.perform(post("/api/admin/tenants/subscription-automation/reconcile")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoRenewedCount").value(1))
                .andExpect(jsonPath("$.data.autoSuspendedCount").value(1))
                .andExpect(jsonPath("$.data.autoRenewedTenantIds[0]").value(autoRenewTenantId))
                .andExpect(jsonPath("$.data.autoSuspendedTenantIds[0]").value(expiredSuspendTenantId))
                .andExpect(jsonPath("$.data.executedAt").isString());

        TenantSubscription renewedSubscription = tenantSubscriptionRepository.findByTenantId(autoRenewTenantId).orElseThrow();
        TenantProfile renewedProfile = tenantProfileRepository.findByTenantId(autoRenewTenantId).orElseThrow();
        assertEquals("active", renewedSubscription.subscriptionStatus());
        assertEquals("active", renewedProfile.tenantStatus());
        assertEquals(2, billingOrderRepository.findByTenantId(autoRenewTenantId).size());
        assertEquals("auto_renew", billingOrderRepository.findByTenantId(autoRenewTenantId).get(0).orderType());

        TenantSubscription expiredSubscription = tenantSubscriptionRepository.findByTenantId(expiredSuspendTenantId).orElseThrow();
        TenantProfile expiredProfile = tenantProfileRepository.findByTenantId(expiredSuspendTenantId).orElseThrow();
        assertEquals("expired", expiredSubscription.subscriptionStatus());
        assertEquals("expired_suspended", expiredProfile.tenantStatus());
        assertEquals(1, billingOrderRepository.findByTenantId(expiredSuspendTenantId).size());
    }

    @Test
    void shouldCreatePendingAutoRenewBillAndRecoverAfterSettlement() throws Exception {
        String tenantId = registerTenant("tenant-auto-renew-pending", "13800000005");
        subscribeTenant(tenantId, "13800000005", true);

        mockMvc.perform(put("/api/admin/tenants/{id}/feature-toggles", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureFlags": {
                                    "billing_auto_charge_authorized": false
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureFlags.billing_auto_charge_authorized").value(false));

        expireSubscription(tenantId, "active");

        mockMvc.perform(post("/api/admin/tenants/subscription-automation/reconcile")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoRenewedCount").value(0))
                .andExpect(jsonPath("$.data.autoSuspendedCount").value(0))
                .andExpect(jsonPath("$.data.autoChargeFailedCount").value(1))
                .andExpect(jsonPath("$.data.autoChargeFailedTenantIds[0]").value(tenantId));

        TenantSubscription pendingSubscription = tenantSubscriptionRepository.findByTenantId(tenantId).orElseThrow();
        TenantProfile pendingProfile = tenantProfileRepository.findByTenantId(tenantId).orElseThrow();
        assertEquals("past_due", pendingSubscription.subscriptionStatus());
        assertEquals("payment_overdue", pendingProfile.tenantStatus());

        String pendingBillingOrderId = billingOrderRepository.findByTenantId(tenantId).get(0).billingOrderId();
        assertEquals("auto_renew", billingOrderRepository.findByTenantId(tenantId).get(0).orderType());
        assertEquals("pending", billingOrderRepository.findByTenantId(tenantId).get(0).paymentStatus());

        String ownerToken = login("13800000005");
        mockMvc.perform(post("/api/tenants/{id}/billing-orders/{billingOrderId}/settle", tenantId, pendingBillingOrderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingOrderId").value(pendingBillingOrderId))
                .andExpect(jsonPath("$.data.paymentStatus").value("paid"));

        TenantSubscription recoveredSubscription = tenantSubscriptionRepository.findByTenantId(tenantId).orElseThrow();
        TenantProfile recoveredProfile = tenantProfileRepository.findByTenantId(tenantId).orElseThrow();
        assertEquals("active", recoveredSubscription.subscriptionStatus());
        assertEquals("active", recoveredProfile.tenantStatus());

        mockMvc.perform(post("/api/tenants/{id}/invoice-requests", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billingOrderId": "%s",
                                  "invoiceTitle": "自动续费补偿账单",
                                  "invoiceTaxNo": "91310000AUTO0005"
                                }
                                """.formatted(pendingBillingOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingOrderId").value(pendingBillingOrderId))
                .andExpect(jsonPath("$.data.invoiceStatus").value("pending"));
    }

    @Test
    void shouldExposeReleaseReadinessChecklistWithBlockingReasonsAndEvidence() throws Exception {
        String tenantId = registerTenant("tenant-release-readiness", "13800000006");
        String ownerToken = login("13800000006");

        subscribeTenant(tenantId, "13800000006", true);
        confirmSensitivePermission(ownerToken, "tenant.data.export.manage");

        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk());

        confirmSensitivePermission(ownerToken, "tenant.lifecycle.manage");
        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/plan", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "release_readiness_sampling"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.conclusion").value("reject_release"))
                .andExpect(jsonPath("$.data.checklistItems.length()").value(4))
                .andExpect(jsonPath("$.data.checklistItems[0].itemCode").value("config_hardening"))
                .andExpect(jsonPath("$.data.checklistItems[0].status").value("blocked"))
                .andExpect(jsonPath("$.data.checklistItems[0].detail").value(org.hamcrest.Matchers.containsString("explicit auth secret enforcement is disabled")))
                .andExpect(jsonPath("$.data.checklistItems[0].detail").value(org.hamcrest.Matchers.containsString("legacy header context fallback is still enabled")))
                .andExpect(jsonPath("$.data.checklistItems[1].itemCode").value("critical_state_evidence"))
                .andExpect(jsonPath("$.data.checklistItems[1].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[1].evidenceCount").value(4))
                .andExpect(jsonPath("$.data.checklistItems[2].itemCode").value("audit_traceability"))
                .andExpect(jsonPath("$.data.checklistItems[2].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[3].itemCode").value("automation_regression_evidence"))
                .andExpect(jsonPath("$.data.checklistItems[3].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[3].evidenceCount").value(3))
                .andExpect(jsonPath("$.data.checklistItems[3].detail").value(org.hamcrest.Matchers.containsString("server-local")))
                .andExpect(jsonPath("$.data.checklistItems[3].detail").value(org.hamcrest.Matchers.containsString("2026-06-08T09:59:30+08:00")))
                .andExpect(jsonPath("$.data.checklistItems[3].detail").value(org.hamcrest.Matchers.containsString("auth/openplatform/compliance regression suites passed")))
                .andExpect(jsonPath("$.data.blockingReasons[0]").value(org.hamcrest.Matchers.containsString("default auth secret")))
                .andExpect(jsonPath("$.data.blockingReasons[1]").value(org.hamcrest.Matchers.containsString("bootstrap password")))
                .andExpect(jsonPath("$.data.blockingReasons[2]").value(org.hamcrest.Matchers.containsString("explicit auth secret enforcement is disabled")))
                .andExpect(jsonPath("$.data.blockingReasons[3]").value(org.hamcrest.Matchers.containsString("legacy header context fallback is still enabled")))
                .andExpect(jsonPath("$.data.evidenceSummary.exportTaskCount").value(1))
                .andExpect(jsonPath("$.data.evidenceSummary.cleanupTaskCount").value(1))
                .andExpect(jsonPath("$.data.evidenceSummary.complianceAcceptanceCount").value(1))
                .andExpect(jsonPath("$.data.evidenceSummary.billingOrderCount").value(1));
    }

    @Test
    void shouldPublishComplianceDocumentAndTrackPendingReacceptance() throws Exception {
        String tenantId = registerTenant("tenant-compliance-a", "13800000003");
        String ownerToken = login("13800000003");

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.06"));

        mockMvc.perform(get("/api/admin/tenants/compliance/acceptances")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .param("documentCode", "privacy-policy")
                        .param("acceptanceStatus", "accepted_current_version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].currentVersion").value("2026.06"))
                .andExpect(jsonPath("$.data[0].latestAcceptedVersion").value("2026.06"))
                .andExpect(jsonPath("$.data[0].acceptanceStatus").value("accepted_current_version"));

        mockMvc.perform(post("/api/admin/tenants/compliance/documents/privacy-policy/publish")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "2026.07",
                                  "title": "隐私政策",
                                  "content": "已发布新的隐私政策版本，需租户重新确认。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data.version").value("2026.07"));

        mockMvc.perform(get("/api/compliance/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("2026.07"));

        mockMvc.perform(get("/api/admin/tenants/compliance/documents")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.07"))
                .andExpect(jsonPath("$.data[0].pendingReacceptanceTenantCount").value(1));

        mockMvc.perform(get("/api/admin/tenants/compliance/acceptances")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .param("documentCode", "privacy-policy")
                        .param("acceptanceStatus", "pending_reacceptance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data[0].currentVersion").value("2026.07"))
                .andExpect(jsonPath("$.data[0].latestAcceptedVersion").value("2026.06"))
                .andExpect(jsonPath("$.data[0].acceptanceStatus").value("pending_reacceptance"));

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.07"));

        mockMvc.perform(get("/api/admin/tenants/compliance/acceptances")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .param("documentCode", "privacy-policy")
                        .param("tenantId", tenantId)
                        .param("acceptanceStatus", "accepted_current_version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data[0].currentVersion").value("2026.07"))
                .andExpect(jsonPath("$.data[0].latestAcceptedVersion").value("2026.07"))
                .andExpect(jsonPath("$.data[0].acceptanceStatus").value("accepted_current_version"));
    }

    @Test
    void shouldBlockSensitiveOperationsUntilLatestComplianceDocumentsAreReaccepted() throws Exception {
        String tenantId = registerTenant("tenant-compliance-block", "13800000004");
        String ownerToken = login("13800000004");

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy", "user-agreement"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].version").value("2026.06"))
                .andExpect(jsonPath("$.data[1].version").value("2026.06"));

        mockMvc.perform(post("/api/admin/tenants/compliance/documents/privacy-policy/publish")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "2026.08",
                                  "title": "隐私政策",
                                  "content": "新的隐私政策版本已经发布。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("2026.08"));

        confirmSensitivePermission(ownerToken, "tenant.data.export.manage");
        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1014"))
                .andExpect(jsonPath("$.data.permissionCode").value("tenant.data.export.manage"))
                .andExpect(jsonPath("$.data.pendingDocumentCodes[0]").value("privacy-policy"));

        confirmSensitivePermission(ownerToken, "tenant.lifecycle.manage");
        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1014"))
                .andExpect(jsonPath("$.data.permissionCode").value("tenant.lifecycle.manage"))
                .andExpect(jsonPath("$.data.pendingDocumentCodes[0]").value("privacy-policy"));

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.08"));

        confirmSensitivePermission(ownerToken, "tenant.data.export.manage");
        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeCode").value("tenant_profile"))
                .andExpect(jsonPath("$.data.status").value("completed"));

        confirmSensitivePermission(ownerToken, "tenant.lifecycle.manage");
        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("offboarding_requested"));
    }

    private String registerTenant() throws Exception {
        return registerTenant("星迹电商", "13800000000");
    }

    private String registerTenant(String tenantName, String mobile) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "张三",
                                  "mobile": "%s"
                                }
                                """.formatted(tenantName, mobile)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("tenantId").asText();
    }

    private void subscribeTenant(String tenantId, String mobile, boolean autoRenew) throws Exception {
        String ownerToken = login(mobile);
        mockMvc.perform(post("/api/tenants/{id}/subscription/subscribe", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "basic",
                                  "seatCount": 2,
                                  "autoRenew": %s
                                }
                                """.formatted(autoRenew)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("basic"))
                .andExpect(jsonPath("$.data.autoRenew").value(autoRenew));
    }

    private void expireSubscription(String tenantId, String tenantStatus) {
        TenantSubscription current = tenantSubscriptionRepository.findByTenantId(tenantId).orElseThrow();
        tenantSubscriptionRepository.save(new TenantSubscription(
                current.subscriptionId(),
                current.tenantId(),
                current.planCode(),
                current.planName(),
                current.subscriptionStatus(),
                current.startedAt(),
                OffsetDateTime.now().minusDays(2),
                current.seatCount(),
                current.autoRenew()
        ));
        TenantProfile profile = tenantProfileRepository.findByTenantId(tenantId).orElseThrow();
        tenantProfileRepository.save(profile.withStatusAndTrialEndAt(tenantStatus, current.expiredAt()));
    }

    private String login(String mobile) throws Exception {
        JsonNode root = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456"
                                }
                                """.formatted(mobile)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        return root.path("data").path("token").asText();
    }

    private void confirmSensitivePermission(String token, String permissionCode) throws Exception {
        mockMvc.perform(post("/api/auth/sensitive-operation-confirmations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "123456",
                                  "permissionCode": "%s"
                                }
                                """.formatted(permissionCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(permissionCode))
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.expiresAt").isString());
    }
}
