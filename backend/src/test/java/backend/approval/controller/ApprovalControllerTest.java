package backend.approval.controller;

import backend.approval.application.ApprovalService;
import backend.audit.application.AuditLogService;
import backend.auth.dto.LoginResponse;
import backend.common.api.ApiResponse;
import backend.fulfillment.application.FulfillmentService;
import backend.inventory.application.InventoryService;
import backend.notification.application.NotificationService;
import backend.order.application.OrderService;
import backend.organization.application.OrganizationService;
import backend.product.application.ProductMappingService;
import backend.product.application.ProductService;
import backend.saas.application.SaasTenantService;
import backend.servicecase.application.ServiceCaseService;
import backend.store.application.StoreChannelService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class ApprovalControllerTest {

    private static final String OWNER_MOBILE = "13900000000";
    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ServiceCaseService serviceCaseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private FulfillmentService fulfillmentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductMappingService productMappingService;

    @Autowired
    private ProductService productService;

    @Autowired
    private StoreChannelService storeChannelService;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService.clear();
        approvalService.clear();
        serviceCaseService.clear();
        inventoryService.clear();
        fulfillmentService.clear();
        orderService.clear();
        productMappingService.clear();
        productService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldCreateProcessAndTransferApprovals() throws Exception {
        ApprovalFixture fixture = prepareFixture();

        MvcResult replenishmentCreateResult = mockMvc.perform(post("/api/replenishment-tasks")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9001",
                                  "suggestedQty": 36,
                                  "reasonText": "inventory below threshold"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        String replenishmentTaskId = objectMapper.readTree(replenishmentCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("replenishmentTaskId")
                .asText();

        mockMvc.perform(post("/api/replenishment-tasks/{id}/submit-approval", replenishmentTaskId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value("pending_approval"))
                .andExpect(jsonPath("$.data.approvalStatus").value("pending"));

        MvcResult afterSaleCreateResult = mockMvc.perform(post("/api/after-sales")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "afterSaleType": "refund",
                                  "reasonText": "size is not suitable",
                                  "evidenceBlob": "{\\\"images\\\":[\\\"after-sale-1.png\\\"]}"
                                }
                                """.formatted(fixture.orderId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("created"))
                .andReturn();
        String afterSaleId = objectMapper.readTree(afterSaleCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("afterSaleId")
                .asText();

        mockMvc.perform(post("/api/after-sales/{id}/submit-approval", afterSaleId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("reviewing"));

        MvcResult listResult = mockMvc.perform(get("/api/approvals")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        JsonNode approvals = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("data");
        String replenishmentApprovalId = findApprovalId(approvals, "replenishment_task", replenishmentTaskId);
        String afterSaleApprovalId = findApprovalId(approvals, "after_sale_record", afterSaleId);

        mockMvc.perform(post("/api/approvals/{id}/approve", replenishmentApprovalId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "remark": "replenishment approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.currentHandlerId").value(OWNER_MOBILE));

        mockMvc.perform(get("/api/replenishment-tasks")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].taskStatus").value("approved"))
                .andExpect(jsonPath("$.data[0].approvalStatus").value("approved"));

        mockMvc.perform(post("/api/approvals/{id}/reject", afterSaleApprovalId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "evidence is insufficient"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));

        mockMvc.perform(get("/api/after-sales/{id}", afterSaleId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));

        mockMvc.perform(get("/api/orders/{id}", fixture.orderId())
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.orderStatus").value("pending_fulfillment"));

        MvcResult secondTaskCreateResult = mockMvc.perform(post("/api/replenishment-tasks")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9002",
                                  "suggestedQty": 24,
                                  "reasonText": "manual restock review"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        String secondTaskId = objectMapper.readTree(secondTaskCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("replenishmentTaskId")
                .asText();

        MvcResult manualApprovalCreateResult = mockMvc.perform(post("/api/approvals")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalType": "manual_replenishment_review",
                                  "relatedType": "replenishment_task",
                                  "relatedId": "%s",
                                  "currentHandlerId": "operations-manager",
                                  "remark": "manual approval created"
                                }
                                """.formatted(secondTaskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.currentHandlerId").value("operations-manager"))
                .andReturn();
        String manualApprovalId = objectMapper.readTree(manualApprovalCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("approvalId")
                .asText();

        mockMvc.perform(post("/api/approvals/{id}/transfer", manualApprovalId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentHandlerId": "finance-manager",
                                  "remark": "transfer to finance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.currentHandlerId").value("finance-manager"));

        mockMvc.perform(get("/api/approvals/{id}", manualApprovalId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalId").value(manualApprovalId))
                .andExpect(jsonPath("$.data.currentHandlerId").value("finance-manager"));
    }

    @Test
    void shouldCreateApprovalTemplatesAndProcessGovernanceApprovalRequests() throws Exception {
        ApprovalFixture fixture = prepareFixture();

        mockMvc.perform(post("/api/approval-templates")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "contract-default",
                                  "templateName": "鍚堝悓瀹℃壒妯℃澘",
                                  "approvalType": "contract",
                                  "enabled": true,
                                  "stages": [
                                    {
                                      "stageCode": "legal_review",
                                      "handlerId": "legal-manager",
                                      "stageOrder": 1
                                    },
                                    {
                                      "stageCode": "finance_review",
                                      "handlerId": "finance-manager",
                                      "stageOrder": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateCode").value("contract-default"))
                .andExpect(jsonPath("$.data.approvalType").value("contract"))
                .andExpect(jsonPath("$.data.stages.length()").value(2))
                .andExpect(jsonPath("$.data.stages[0].stageCode").value("legal_review"));

        mockMvc.perform(post("/api/approval-templates")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "payment-default",
                                  "templateName": "浠樻瀹℃壒妯℃澘",
                                  "approvalType": "payment",
                                  "enabled": true,
                                  "stages": [
                                    {
                                      "stageCode": "finance_pay_review",
                                      "handlerId": "finance-director",
                                      "stageOrder": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/approval-templates")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "expense-default",
                                  "templateName": "璐圭敤瀹℃壒妯℃澘",
                                  "approvalType": "expense",
                                  "enabled": true,
                                  "stages": [
                                    {
                                      "stageCode": "ops_expense_review",
                                      "handlerId": "ops-manager",
                                      "stageOrder": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/approval-templates")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        MvcResult contractRequestResult = mockMvc.perform(post("/api/governance-approval-requests")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalType": "contract",
                                  "templateCode": "contract-default",
                                  "documentNo": "CONTRACT-001",
                                  "subject": "年度代运营合同",
                                  "amount": 30000.00,
                                  "counterparty": "渚涘簲鍟咥",
                                  "remark": "骞村害妗嗘灦鍚堝悓瀹℃壒"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalType").value("contract"))
                .andExpect(jsonPath("$.data.requestStatus").value("pending"))
                .andExpect(jsonPath("$.data.currentStageCode").value("legal_review"))
                .andExpect(jsonPath("$.data.currentHandlerId").value("legal-manager"))
                .andReturn();
        String contractRequestId = objectMapper.readTree(contractRequestResult.getResponse().getContentAsString())
                .path("data")
                .path("requestId")
                .asText();

        MvcResult paymentRequestResult = mockMvc.perform(post("/api/governance-approval-requests")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalType": "payment",
                                  "templateCode": "payment-default",
                                  "documentNo": "PAYMENT-001",
                                  "subject": "供应商付款申请",
                                  "amount": 12800.00,
                                  "counterparty": "渚涘簲鍟咮",
                                  "remark": "璐︽湡浠樻瀹℃壒"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalType").value("payment"))
                .andExpect(jsonPath("$.data.currentStageCode").value("finance_pay_review"))
                .andReturn();
        String paymentRequestId = objectMapper.readTree(paymentRequestResult.getResponse().getContentAsString())
                .path("data")
                .path("requestId")
                .asText();

        mockMvc.perform(post("/api/governance-approval-requests")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalType": "expense",
                                  "templateCode": "expense-default",
                                  "documentNo": "EXPENSE-001",
                                  "subject": "鐩存挱涓撻」璐圭敤鐢宠",
                                  "amount": 5600.00,
                                  "counterparty": "鏈嶅姟鍟咰",
                                  "remark": "鐩存挱杩愯惀璐圭敤瀹℃壒"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalType").value("expense"))
                .andExpect(jsonPath("$.data.currentStageCode").value("ops_expense_review"));

        MvcResult governanceListResult = mockMvc.perform(get("/api/governance-approval-requests")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andReturn();

        JsonNode approvals = objectMapper.readTree(mockMvc.perform(get("/api/approvals")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data");
        String contractApprovalId = findApprovalId(approvals, "governance_request", contractRequestId);
        String paymentApprovalId = findApprovalId(approvals, "governance_request", paymentRequestId);

        mockMvc.perform(post("/api/approvals/{id}/approve", contractApprovalId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "娉曞姟宸查€氳繃"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.currentHandlerId").value("finance-manager"));

        mockMvc.perform(get("/api/governance-approval-requests/{id}", contractRequestId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("pending"))
                .andExpect(jsonPath("$.data.currentStageCode").value("finance_review"))
                .andExpect(jsonPath("$.data.currentHandlerId").value("finance-manager"));

        mockMvc.perform(post("/api/approvals/{id}/approve", contractApprovalId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "璐㈠姟宸查€氳繃"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));

        mockMvc.perform(get("/api/governance-approval-requests/{id}", contractRequestId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("approved"));

        mockMvc.perform(post("/api/approvals/{id}/reject", paymentApprovalId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "浠樻渚濇嵁涓嶈冻"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));

        mockMvc.perform(get("/api/governance-approval-requests/{id}", paymentRequestId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("rejected"));
    }

    private ApprovalFixture prepareFixture() throws Exception {
        JsonNode tenantData = registerTenant();
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        String storeId = connectStore(organizationId, ownerToken);
        String productId = publishProduct(storeId, ownerToken);
        String orderId = syncOrder(storeId, productId, ownerToken);
        return new ApprovalFixture(tenantId, storeId, productId, orderId, ownerToken);
    }

    private JsonNode registerTenant() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "approval-center",
                                  "ownerName": "approval-owner",
                                  "mobile": "13900000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String connectStore(String organizationId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "shop-approval-001",
                                  "shopName": "approval-store",
                                  "profitThreshold": 20.00,
                                  "riskThreshold": 75.00
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();
    }

    private String publishProduct(String storeId, String token) throws Exception {
        MvcResult candidateResult = mockMvc.perform(post("/api/candidate-products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "sourceType": "1688",
                                  "sourceUrl": "https://source.example.com/p/approval-001",
                                  "title": "approval-product",
                                  "category": "storage",
                                  "estimatedProfit": 28.80,
                                  "riskLevel": "medium",
                                  "recommendationReason": "used for approval workflow test",
                                  "aiSummary": "approval workflow product"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andReturn();
        String candidateProductId = objectMapper.readTree(candidateResult.getResponse().getContentAsString())
                .path("data")
                .path("candidateProductId")
                .asText();

        MvcResult draftResult = mockMvc.perform(post("/api/product-drafts/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateProductId": "%s",
                                  "aiVersion": "ai-approval-v1",
                                  "suggestedPrice": 129.90
                                }
                                """.formatted(candidateProductId)))
                .andExpect(status().isOk())
                .andReturn();
        String draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString())
                .path("data")
                .path("productDraftId")
                .asText();

        MvcResult productResult = mockMvc.perform(post("/api/product-drafts/{id}/publish", draftId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformProductId": "platform-approval-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();
    }

    private String syncOrder(String storeId, String productId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "approval-order-001",
                                  "productId": "%s",
                                  "quantity": 1,
                                  "unitPrice": 79.90
                                }
                                """.formatted(storeId, productId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("orderId")
                .asText();
    }

    private String findApprovalId(JsonNode approvals, String relatedType, String relatedId) {
        for (JsonNode approval : approvals) {
            if (relatedType.equals(approval.path("relatedType").asText())
                    && relatedId.equals(approval.path("relatedId").asText())) {
                return approval.path("approvalId").asText();
            }
        }
        throw new IllegalStateException("approval not found for related type " + relatedType + " and related id " + relatedId);
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
}

record ApprovalFixture(String tenantId, String storeId, String productId, String orderId, String ownerToken) {
}

