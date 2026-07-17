package backend.saas.controller;

import backend.audit.application.AuditLogService;
import backend.auth.security.AuthPermissionCodes;
import backend.auth.application.AuthService;
import backend.auth.dto.LoginResponse;
import backend.common.api.ApiResponse;
import backend.inventory.domain.repository.InventorySnapshotRepository;
import backend.inventory.domain.repository.ReplenishmentTaskRepository;
import backend.inventory.model.InventorySnapshot;
import backend.inventory.model.ReplenishmentTask;
import backend.member.domain.repository.MemberProfileRepository;
import backend.member.domain.repository.MemberTagRepository;
import backend.member.model.MemberProfile;
import backend.member.model.MemberTag;
import backend.organization.application.OrganizationService;
import backend.order.domain.repository.OrderItemRepository;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderItem;
import backend.order.model.OrderMain;
import backend.product.domain.repository.ProductRepository;
import backend.product.model.Product;
import backend.saas.application.SaasTenantService;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import com.fasterxml.jackson.core.type.TypeReference;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class TenantLifecycleControllerTest {

    private static final String OWNER_MOBILE = "13800000000";
    private static final String BOOTSTRAP_PASSWORD = "123456";

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
    private AuthService authService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Autowired
    private ReplenishmentTaskRepository replenishmentTaskRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private MemberTagRepository memberTagRepository;

    @BeforeEach
    void setUp() {
        memberTagRepository.deleteAll();
        memberProfileRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        replenishmentTaskRepository.deleteAll();
        inventorySnapshotRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
        authService.clearSensitiveOperationConfirmations();
    }

    @Test
    void shouldRegisterTenantAndExposeSubscriptionAndQuota() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "鏄熻景鐢靛晢",
                                  "ownerName": "寮犱笁",
                                  "mobile": "13800000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.tenantStatus").value("trial"))
                .andExpect(jsonPath("$.data.tenantCode").value("tenant_1001"))
                .andReturn();

        JsonNode tenantData = objectMapper.readTree(registerResult.getResponse().getContentAsString()).path("data");
        String tenantId = tenantData.path("tenantId").asText();
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/tenants/{id}/subscription", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("trial"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("trialing"))
                .andExpect(jsonPath("$.data.seatCount").value(1));

        mockMvc.perform(get("/api/tenants/{id}/quotas", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data[0].quotaCode").value("store_count"));
    }

    @Test
    void shouldSubscribeRenewChangePlanAndPurchaseSeats() throws Exception {
        String tenantId = registerTenantAndReturnId();
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        mockMvc.perform(post("/api/tenants/{id}/subscription/subscribe", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "basic",
                                  "seatCount": 2,
                                  "autoRenew": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("basic"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("active"))
                .andExpect(jsonPath("$.data.seatCount").value(2))
                .andExpect(jsonPath("$.data.autoRenew").value(true));

        mockMvc.perform(post("/api/tenants/{id}/subscription/renew", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "months": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("basic"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("active"));

        mockMvc.perform(post("/api/tenants/{id}/subscription/upgrade", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "pro"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("pro"));

        mockMvc.perform(post("/api/tenants/{id}/seats/purchase", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "seatCount": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seatCount").value(5));

        mockMvc.perform(post("/api/tenants/{id}/subscription/downgrade", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "basic"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("basic"));

        mockMvc.perform(get("/api/tenants/{id}/quotas", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].quotaCode").value("seat_count"))
                .andExpect(jsonPath("$.data[1].quotaLimit").value(5));

        String billingOrderId = objectMapper.readTree(mockMvc.perform(get("/api/tenants/{id}/billing-orders", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].paymentStatus").value("paid"))
                .andReturn().getResponse().getContentAsString())
                .path("data")
                .get(0)
                .path("billingOrderId")
                .asText();

        mockMvc.perform(post("/api/tenants/{id}/invoice-requests", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billingOrderId": "%s",
                                  "invoiceTitle": "鏄熻景鐢靛晢鏈夐檺鍏徃",
                                  "invoiceTaxNo": "91310000TEST0001"
                                }
                                """.formatted(billingOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingOrderId").value(billingOrderId))
                .andExpect(jsonPath("$.data.invoiceStatus").value("pending"));
    }

    @Test
    void shouldManageLifecycleAndComplianceAcceptances() throws Exception {
        String tenantId = registerTenantAndReturnId();
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/compliance/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data.version").value("2026.06"));

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
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.06"))
                .andExpect(jsonPath("$.data[1].documentCode").value("user-agreement"))
                .andExpect(jsonPath("$.data[1].version").value("2026.06"));

        mockMvc.perform(get("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[1].documentCode").value("user-agreement"));

        mockMvc.perform(post("/api/tenants/{id}/suspend", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("suspended"));

        mockMvc.perform(post("/api/tenants/{id}/resume", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("trial"));

        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1015"))
                .andExpect(jsonPath("$.message").value("sensitive operation confirmation required"));

        confirmSensitivePermission(ownerToken, AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE);

        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("offboarding_requested"));
    }

    @Test
    void shouldAllowTenantUserToReadAndUpdateExternalIntegrationPreferences() throws Exception {
        RegisteredTenant tenant = registerTenant("tenant-external-preferences", OWNER_MOBILE);
        String ownerToken = login(tenant.mobile(), BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/tenant/external-integrations/preferences")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenant.tenantId()))
                .andExpect(jsonPath("$.data.options.length()").value(6))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='erp')].selected", org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='routing')].selected", org.hamcrest.Matchers.hasItem(true)));

        mockMvc.perform(put("/api/tenant/external-integrations/preferences")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectedSystemCodes": ["erp", "messaging"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenant.tenantId()))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='erp')].selected", org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='messaging')].selected", org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='wms')].selected", org.hamcrest.Matchers.hasItem(false)))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='bi')].selected", org.hamcrest.Matchers.hasItem(false)))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='routing')].selected", org.hamcrest.Matchers.hasItem(false)));

        mockMvc.perform(get("/api/tenant/system/observability-overview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiredExternalSystemCount").value(2))
                .andExpect(jsonPath("$.data.readyExternalSystemCount").value(0))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.ready").value(false))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.configuredCount").value(0))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.reachableCount").value(0));
    }

    @Test
    void shouldUpdateExternalIntegrationPreferencesWithoutDroppingOtherFeatureFlags() throws Exception {
        RegisteredTenant tenant = registerTenant("tenant-external-preferences-merge", "13800000066");
        String ownerToken = login(tenant.mobile(), BOOTSTRAP_PASSWORD);

        saasTenantService.updateFeatureToggles(tenant.tenantId(), Map.of(
                "billing_auto_charge_authorized", false,
                "ai_copilot", true
        ));

        mockMvc.perform(put("/api/tenant/external-integrations/preferences")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectedSystemCodes": ["erp"]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tenant/external-integrations/preferences")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureFlags.billing_auto_charge_authorized").value(false))
                .andExpect(jsonPath("$.data.featureFlags.ai_copilot").value(true))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='erp')].selected", org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$.data.options[?(@.systemCode=='wms')].selected", org.hamcrest.Matchers.hasItem(false)));
    }

    @Test
    void shouldCreateListAndDownloadTenantDataExports() throws Exception {
        String tenantA = registerTenantAndReturnId("tenant-export-a", "13800000011");
        String tenantB = registerTenantAndReturnId("tenant-export-b", "13800000012");
        String ownerTokenA = login("13800000011", BOOTSTRAP_PASSWORD);
        String ownerTokenB = login("13800000012", BOOTSTRAP_PASSWORD);

        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1015"))
                .andExpect(jsonPath("$.message").value("sensitive operation confirmation required"));

        confirmSensitivePermission(ownerTokenA, AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE);

        String exportTaskId = objectMapper.readTree(mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeCode").value("tenant_profile"))
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.requestedBy").value("13800000011"))
                .andReturn().getResponse().getContentAsString())
                .path("data")
                .path("exportTaskId")
                .asText();

        confirmSensitivePermission(ownerTokenB, AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE);

        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].exportTaskId").value(exportTaskId))
                .andExpect(jsonPath("$.data[0].scopeCode").value("tenant_profile"));

        mockMvc.perform(get("/api/tenant/data-exports/{taskId}/download", exportTaskId)
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"tenant_profile-" + tenantA + ".json\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(tenantA)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(tenantB))));
    }

    @Test
    void shouldExportOperationalBundlesWithinTenantScope() throws Exception {
        RegisteredTenant tenantA = registerTenant("tenant-export-scope-a", "13800000021");
        RegisteredTenant tenantB = registerTenant("tenant-export-scope-b", "13800000022");
        TenantBusinessFixture fixtureA = seedOperationalData(tenantA, 101);
        seedOperationalData(tenantB, 201);

        String ownerTokenA = login(tenantA.mobile(), BOOTSTRAP_PASSWORD);

        String ordersTaskId = createExportTask(ownerTokenA, "orders_bundle");
        String ordersContent = downloadExportContent(ownerTokenA, ordersTaskId);
        JsonNode ordersBundle = objectMapper.readTree(ordersContent);
        assertEquals(1, ordersBundle.path("stores").size());
        assertEquals(tenantA.tenantId(), ordersBundle.path("stores").get(0).path("tenantId").asText());
        assertEquals(1, ordersBundle.path("orders").size());
        assertEquals(fixtureA.platformOrderId(), ordersBundle.path("orders").get(0).path("platformOrderId").asText());
        assertEquals(1, ordersBundle.path("orderItems").path(fixtureA.orderId()).size());
        assertEquals(fixtureA.productId(), ordersBundle.path("orderItems").path(fixtureA.orderId()).get(0).path("productId").asText());
        assertFalse(ordersContent.contains("platform-order-201"));

        String productTaskId = createExportTask(ownerTokenA, "product_catalog");
        String productContent = downloadExportContent(ownerTokenA, productTaskId);
        JsonNode productBundle = objectMapper.readTree(productContent);
        assertEquals(1, productBundle.path("stores").size());
        assertEquals(1, productBundle.path("products").size());
        assertEquals(fixtureA.productTitle(), productBundle.path("products").get(0).path("title").asText());
        assertFalse(productContent.contains("product-title-201"));
    }

    @Test
    void shouldPlanAndExecuteCleanupWithoutAffectingOtherTenantData() throws Exception {
        RegisteredTenant tenantA = registerTenant("tenant-cleanup-a", "13800000031");
        RegisteredTenant tenantB = registerTenant("tenant-cleanup-b", "13800000032");
        TenantBusinessFixture fixtureA = seedOperationalData(tenantA, 301);
        TenantBusinessFixture fixtureB = seedOperationalData(tenantB, 401);

        String ownerTokenA = login(tenantA.mobile(), BOOTSTRAP_PASSWORD);
        String ownerTokenB = login(tenantB.mobile(), BOOTSTRAP_PASSWORD);
        InvitedTenantAdmin reviewer = inviteTenantAdminAndLogin(tenantA, ownerTokenA, "reviewer-admin", "13800000033");
        InvitedTenantAdmin executor = inviteTenantAdminAndLogin(tenantA, ownerTokenA, "executor-admin", "13800000034");

        confirmSensitivePermission(ownerTokenA, AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE);

        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("offboarding_requested"));

        String cleanupTaskId = objectMapper.readTree(mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/plan", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "merchant_offboarding"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("planned"))
                .andExpect(jsonPath("$.data.impactSummary.storeCount").value(1))
                .andExpect(jsonPath("$.data.impactSummary.productCount").value(1))
                .andExpect(jsonPath("$.data.impactSummary.orderCount").value(1))
                .andExpect(jsonPath("$.data.impactSummary.memberProfileCount").value(1))
                .andReturn().getResponse().getContentAsString())
                .path("data")
                .path("cleanupTaskId")
                .asText();

        mockMvc.perform(get("/api/tenants/{id}/cleanup-tasks", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].cleanupTaskId").value(cleanupTaskId))
                .andExpect(jsonPath("$.data[0].status").value("planned"));

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/{taskId}/execute", tenantA.tenantId(), cleanupTaskId)
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1008"))
                .andExpect(jsonPath("$.message").value("cleanup task must be reviewed before execution"));

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/{taskId}/review", tenantA.tenantId(), cleanupTaskId)
                        .header("Authorization", "Bearer " + reviewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("reviewed"))
                .andExpect(jsonPath("$.data.requestedBy").value(tenantA.mobile()))
                .andExpect(jsonPath("$.data.reviewedBy").value(reviewer.userId()));

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/plan", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "duplicate_cleanup_plan"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1012"));

        mockMvc.perform(get("/api/tenants/{id}/cleanup-tasks", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("reviewed"))
                .andExpect(jsonPath("$.data[0].reviewedBy").value(reviewer.userId()));

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/{taskId}/execute", tenantA.tenantId(), cleanupTaskId)
                        .header("Authorization", "Bearer " + reviewer.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"))
                .andExpect(jsonPath("$.message").value("cleanup execution requires a different operator"));

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/{taskId}/execute", tenantA.tenantId(), cleanupTaskId)
                        .header("Authorization", "Bearer " + executor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.requestedBy").value(tenantA.mobile()))
                .andExpect(jsonPath("$.data.reviewedBy").value(reviewer.userId()))
                .andExpect(jsonPath("$.data.executedBy").value(executor.userId()))
                .andExpect(jsonPath("$.data.resultSummary.storeCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.productCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.orderCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.orderItemCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.memberProfileCount").value(1));

        mockMvc.perform(get("/api/tenants/{id}/cleanup-tasks", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("completed"))
                .andExpect(jsonPath("$.data[0].reviewedBy").value(reviewer.userId()))
                .andExpect(jsonPath("$.data[0].executedBy").value(executor.userId()));

        String cleanedOrderTaskId = createExportTask(ownerTokenA, "orders_bundle");
        String cleanedOrderContent = downloadExportContent(ownerTokenA, cleanedOrderTaskId);
        JsonNode cleanedOrderBundle = objectMapper.readTree(cleanedOrderContent);
        assertEquals(0, cleanedOrderBundle.path("stores").size());
        assertEquals(0, cleanedOrderBundle.path("orders").size());
        assertEquals(0, cleanedOrderBundle.path("orderItems").size());
        assertFalse(cleanedOrderContent.contains(fixtureA.platformOrderId()));

        String cleanedProductTaskId = createExportTask(ownerTokenA, "product_catalog");
        String cleanedProductContent = downloadExportContent(ownerTokenA, cleanedProductTaskId);
        JsonNode cleanedProductBundle = objectMapper.readTree(cleanedProductContent);
        assertEquals(0, cleanedProductBundle.path("stores").size());
        assertEquals(0, cleanedProductBundle.path("products").size());

        String tenantBTaskId = createExportTask(ownerTokenB, "orders_bundle");
        String tenantBContent = downloadExportContent(ownerTokenB, tenantBTaskId);
        JsonNode tenantBBundle = objectMapper.readTree(tenantBContent);
        assertEquals(1, tenantBBundle.path("stores").size());
        assertEquals(1, tenantBBundle.path("orders").size());
        assertEquals(fixtureB.platformOrderId(), tenantBBundle.path("orders").get(0).path("platformOrderId").asText());
        assertFalse(tenantBContent.contains(fixtureA.platformOrderId()));
    }

    @Test
    void shouldExecuteScopedCleanupWithoutDeletingUnselectedOperationalData() throws Exception {
        RegisteredTenant tenantA = registerTenant("tenant-cleanup-scope-a", "13800000041");
        RegisteredTenant tenantB = registerTenant("tenant-cleanup-scope-b", "13800000042");
        TenantBusinessFixture fixtureA = seedOperationalData(tenantA, 501);
        TenantBusinessFixture fixtureB = seedOperationalData(tenantB, 601);

        String ownerTokenA = login(tenantA.mobile(), BOOTSTRAP_PASSWORD);
        String ownerTokenB = login(tenantB.mobile(), BOOTSTRAP_PASSWORD);
        InvitedTenantAdmin reviewer = inviteTenantAdminAndLogin(tenantA, ownerTokenA, "scope-reviewer", "13800000043");
        InvitedTenantAdmin executor = inviteTenantAdminAndLogin(tenantA, ownerTokenA, "scope-executor", "13800000044");

        confirmSensitivePermission(ownerTokenA, AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE);

        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("offboarding_requested"));

        String cleanupTaskId = objectMapper.readTree(mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/plan", tenantA.tenantId())
                        .header("Authorization", "Bearer " + ownerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "scoped_cleanup",
                                  "cleanupScopes": ["orders", "members"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("planned"))
                .andExpect(jsonPath("$.data.cleanupScopes[0]").value("orders"))
                .andExpect(jsonPath("$.data.cleanupScopes[1]").value("members"))
                .andExpect(jsonPath("$.data.impactSummary.orderCount").value(1))
                .andExpect(jsonPath("$.data.impactSummary.orderItemCount").value(1))
                .andExpect(jsonPath("$.data.impactSummary.memberProfileCount").value(1))
                .andExpect(jsonPath("$.data.impactSummary.memberTagCount").value(1))
                .andExpect(jsonPath("$.data.impactSummary.productCount").doesNotExist())
                .andExpect(jsonPath("$.data.impactSummary.inventorySnapshotCount").doesNotExist())
                .andReturn().getResponse().getContentAsString())
                .path("data")
                .path("cleanupTaskId")
                .asText();

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/{taskId}/review", tenantA.tenantId(), cleanupTaskId)
                        .header("Authorization", "Bearer " + reviewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("reviewed"));

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/{taskId}/execute", tenantA.tenantId(), cleanupTaskId)
                        .header("Authorization", "Bearer " + executor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.cleanupScopes[0]").value("orders"))
                .andExpect(jsonPath("$.data.cleanupScopes[1]").value("members"))
                .andExpect(jsonPath("$.data.resultSummary.orderCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.orderItemCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.memberProfileCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.memberTagCount").value(1))
                .andExpect(jsonPath("$.data.resultSummary.productCount").doesNotExist())
                .andExpect(jsonPath("$.data.resultSummary.inventorySnapshotCount").doesNotExist());

        JsonNode cleanedOrderBundle = objectMapper.readTree(downloadExportContent(ownerTokenA, createExportTask(ownerTokenA, "orders_bundle")));
        assertEquals(0, cleanedOrderBundle.path("orders").size());
        assertEquals(0, cleanedOrderBundle.path("orderItems").size());

        JsonNode cleanedMemberBundle = objectMapper.readTree(downloadExportContent(ownerTokenA, createExportTask(ownerTokenA, "member_bundle")));
        assertEquals(0, cleanedMemberBundle.path("memberProfiles").size());
        assertEquals(0, cleanedMemberBundle.path("memberTags").size());

        JsonNode productBundle = objectMapper.readTree(downloadExportContent(ownerTokenA, createExportTask(ownerTokenA, "product_catalog")));
        assertEquals(1, productBundle.path("stores").size());
        assertEquals(1, productBundle.path("products").size());
        assertEquals(fixtureA.productTitle(), productBundle.path("products").get(0).path("title").asText());

        JsonNode inventoryBundle = objectMapper.readTree(downloadExportContent(ownerTokenA, createExportTask(ownerTokenA, "inventory_bundle")));
        assertEquals(1, inventoryBundle.path("stores").size());
        assertEquals(1, inventoryBundle.path("inventorySnapshots").size());
        assertEquals(1, inventoryBundle.path("replenishmentTasks").size());

        JsonNode tenantBBundle = objectMapper.readTree(downloadExportContent(ownerTokenB, createExportTask(ownerTokenB, "orders_bundle")));
        assertEquals(1, tenantBBundle.path("stores").size());
        assertEquals(1, tenantBBundle.path("orders").size());
        assertEquals(fixtureB.platformOrderId(), tenantBBundle.path("orders").get(0).path("platformOrderId").asText());
    }

    private String registerTenantAndReturnId() throws Exception {
        return registerTenantAndReturnId("tenant-alpha", OWNER_MOBILE);
    }

    private String registerTenantAndReturnId(String tenantName, String mobile) throws Exception {
        return registerTenant(tenantName, mobile).tenantId();
    }

    private RegisteredTenant registerTenant(String tenantName, String mobile) throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-alpha",
                                  "mobile": "%s"
                                }
                                """.formatted(tenantName, mobile)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(registerResult.getResponse().getContentAsString()).path("data");
        return new RegisteredTenant(
                data.path("tenantId").asText(),
                data.path("defaultOrganizationId").asText(),
                mobile
        );
    }

    private String login(String username, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(
                content,
                new TypeReference<>() {
                }
        );
        return response.data().token();
    }

    private String createExportTask(String ownerToken, String scopeCode) throws Exception {
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE);
        return objectMapper.readTree(mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "%s",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """.formatted(scopeCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeCode").value(scopeCode))
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andReturn().getResponse().getContentAsString())
                .path("data")
                .path("exportTaskId")
                .asText();
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

    private String downloadExportContent(String ownerToken, String taskId) throws Exception {
        return mockMvc.perform(get("/api/tenant/data-exports/{taskId}/download", taskId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private InvitedTenantAdmin inviteTenantAdminAndLogin(RegisteredTenant tenant,
                                                         String ownerToken,
                                                         String userName,
                                                         String mobile) throws Exception {
        String userId = "user-" + mobile;
        mockMvc.perform(post("/api/organizations/{id}/members/invite", tenant.defaultOrganizationId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-%s",
                                  "userName": "%s",
                                  "mobile": "%s",
                                  "roleCode": "admin"
                                }
                                """.formatted(mobile, userName, mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("admin"));
        return new InvitedTenantAdmin(userId, mobile, login(mobile, BOOTSTRAP_PASSWORD));
    }

    private TenantBusinessFixture seedOperationalData(RegisteredTenant tenant, int suffix) {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T10:00:00+08:00").plusMinutes(suffix);
        String skuId = "sku-" + suffix;
        String platformOrderId = "platform-order-" + suffix;
        String productTitle = "product-title-" + suffix;

        Store store = storeRepository.save(new Store(
                null,
                tenant.tenantId(),
                tenant.defaultOrganizationId(),
                tenant.mobile(),
                "douyin",
                "shop-" + suffix,
                "shop-name-" + suffix,
                "authorized",
                new BigDecimal("18.80"),
                new BigDecimal("8.80"),
                Map.of("warehouseCode", "wh-" + suffix),
                createdAt
        ));
        Product product = productRepository.save(new Product(
                null,
                store.storeId(),
                "platform-product-" + suffix,
                "draft-" + suffix,
                productTitle,
                "published",
                95,
                createdAt.plusHours(1),
                createdAt
        ));
        inventorySnapshotRepository.save(new InventorySnapshot(
                null,
                store.storeId(),
                product.productId(),
                skuId,
                50,
                5,
                10,
                createdAt.plusHours(2),
                createdAt
        ));
        replenishmentTaskRepository.save(new ReplenishmentTask(
                null,
                store.storeId(),
                product.productId(),
                skuId,
                20,
                "pending",
                "pending",
                "auto-generated",
                createdAt.plusHours(3)
        ));
        OrderMain order = orderRepository.save(new OrderMain(
                null,
                store.storeId(),
                platformOrderId,
                "paid",
                "ready_to_ship",
                new BigDecimal("199.00"),
                new BigDecimal("39.00"),
                "buyer-" + suffix,
                "138****" + String.format("%04d", suffix),
                "Shanghai Road " + suffix,
                createdAt.plusDays(1),
                createdAt.plusHours(4)
        ));
        orderItemRepository.save(new OrderItem(
                null,
                order.orderId(),
                product.productId(),
                skuId,
                2,
                new BigDecimal("99.50"),
                createdAt.plusHours(4)
        ));
        MemberProfile memberProfile = memberProfileRepository.save(new MemberProfile(
                null,
                store.storeId(),
                "customer-" + suffix,
                "member-" + suffix,
                "vip",
                3,
                new BigDecimal("299.00"),
                createdAt.plusHours(5),
                createdAt
        ));
        memberTagRepository.save(new MemberTag(
                null,
                store.storeId(),
                memberProfile.memberId(),
                "tag-" + suffix,
                "VIP-" + suffix,
                "manual",
                createdAt.plusHours(5)
        ));

        return new TenantBusinessFixture(store.storeId(), product.productId(), order.orderId(), platformOrderId, productTitle);
    }

    private record RegisteredTenant(
            String tenantId,
            String defaultOrganizationId,
            String mobile
    ) {
    }

    private record InvitedTenantAdmin(
            String userId,
            String mobile,
            String token
    ) {
    }

    private record TenantBusinessFixture(
            String storeId,
            String productId,
            String orderId,
            String platformOrderId,
            String productTitle
    ) {
    }
}

