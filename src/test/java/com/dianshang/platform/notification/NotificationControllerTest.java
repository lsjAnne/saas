package com.dianshang.platform.notification;

import com.dianshang.platform.approval.application.ApprovalService;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.fulfillment.application.FulfillmentService;
import com.dianshang.platform.inventory.application.InventoryService;
import com.dianshang.platform.notification.application.NotificationService;
import com.dianshang.platform.order.application.OrderService;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.product.application.ProductMappingService;
import com.dianshang.platform.product.application.ProductService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.servicecase.application.ServiceCaseService;
import com.dianshang.platform.store.application.StoreChannelService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

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
    void shouldSeedAndUpdateNotificationTemplates() throws Exception {
        NotificationFixture fixture = prepareFixture();

        MvcResult templateListResult = mockMvc.perform(get("/api/notification-templates")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].templateCode").value("approval_approved"))
                .andReturn();

        JsonNode templates = objectMapper.readTree(templateListResult.getResponse().getContentAsString()).path("data");
        String manualTemplateId = findTemplateId(templates, "manual_notice");

        mockMvc.perform(put("/api/notification-templates/{id}", manualTemplateId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "手工消息模板",
                                  "titleTemplate": "手工消息标题",
                                  "contentTemplate": "手工消息内容",
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationTemplateId").value(manualTemplateId))
                .andExpect(jsonPath("$.data.templateName").value("手工消息模板"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void shouldSendManualNotificationAndRetryFailedTask() throws Exception {
        NotificationFixture fixture = prepareFixture();

        mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "site_message",
                                  "templateCode": "manual_notice",
                                  "targetReceiver": "ops-user",
                                  "payloadJson": "{\\\"bizType\\\":\\\"manual\\\"}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateCode").value("manual_notice"))
                .andExpect(jsonPath("$.data.targetReceiver").value("ops-user"))
                .andExpect(jsonPath("$.data.sendStatus").value("sent"));

        MvcResult failedResult = mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "site_message",
                                  "templateCode": "manual_notice",
                                  "targetReceiver": "ops-fail-user",
                                  "payloadJson": "{\\\"forceFail\\\":true}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sendStatus").value("failed"))
                .andExpect(jsonPath("$.data.retryCount").value(0))
                .andReturn();

        String notificationId = objectMapper.readTree(failedResult.getResponse().getContentAsString())
                .path("data")
                .path("notificationTaskId")
                .asText();

        mockMvc.perform(post("/api/notifications/{id}/retry", notificationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationTaskId").value(notificationId))
                .andExpect(jsonPath("$.data.sendStatus").value("sent"))
                .andExpect(jsonPath("$.data.retryCount").value(1));
    }

    @Test
    void shouldSupportSmsEmailAndFeishuChannels() throws Exception {
        NotificationFixture fixture = prepareFixture();

        mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "sms",
                                  "templateCode": "manual_notice_sms",
                                  "targetReceiver": "13800138000",
                                  "payloadJson": "{\\\"bizType\\\":\\\"sms\\\"}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifyType").value("sms"))
                .andExpect(jsonPath("$.data.sendStatus").value("sent"));

        mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "email",
                                  "templateCode": "manual_notice_email",
                                  "targetReceiver": "ops@example.com",
                                  "payloadJson": "{\\\"bizType\\\":\\\"email\\\"}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifyType").value("email"))
                .andExpect(jsonPath("$.data.sendStatus").value("sent"));

        mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "feishu_bot",
                                  "templateCode": "manual_notice_feishu",
                                  "targetReceiver": "hook:https://open.feishu.cn/open-apis/bot/v2/hook/mock",
                                  "payloadJson": "{\\\"bizType\\\":\\\"feishu\\\"}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifyType").value("feishu_bot"))
                .andExpect(jsonPath("$.data.sendStatus").value("sent"));
    }

    @Test
    void shouldRejectInvalidReceiverOrMismatchedChannel() throws Exception {
        NotificationFixture fixture = prepareFixture();

        mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "sms",
                                  "templateCode": "manual_notice_sms",
                                  "targetReceiver": "bad-mobile",
                                  "payloadJson": "{\\\"bizType\\\":\\\"sms\\\"}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sms targetReceiver must be a valid mobile number"));

        mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "email",
                                  "templateCode": "manual_notice_sms",
                                  "targetReceiver": "ops@example.com",
                                  "payloadJson": "{\\\"bizType\\\":\\\"mismatch\\\"}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("notifyType does not match template channel"));
    }

    @Test
    void shouldBatchSendWithPriorityAndScheduleRunDue() throws Exception {
        NotificationFixture fixture = prepareFixture();
        OffsetDateTime scheduledAt = OffsetDateTime.now().plusMinutes(5);

        mockMvc.perform(post("/api/notifications/batch-send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "email",
                                  "templateCode": "manual_notice_email",
                                  "targetReceivers": ["ops1@example.com", "ops2@example.com"],
                                  "payloadJson": "{\\\"bizType\\\":\\\"batch\\\"}",
                                  "priority": "urgent",
                                  "scheduledAt": "%s",
                                  "batchId": "batch-20260531-01"
                                }
                                """.formatted(scheduledAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sendStatus").value("scheduled"))
                .andExpect(jsonPath("$.data[0].priority").value("urgent"))
                .andExpect(jsonPath("$.data[0].batchId").value("batch-20260531-01"));

        mockMvc.perform(post("/api/notifications/run-due")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldMoveFailedNotificationToDeadLetterAfterRetryLimit() throws Exception {
        NotificationFixture fixture = prepareFixture();
        OffsetDateTime scheduledAt = OffsetDateTime.now().plusSeconds(1);

        MvcResult result = mockMvc.perform(post("/api/notifications/send")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifyType": "email",
                                  "templateCode": "manual_notice_email",
                                  "targetReceiver": "fail@example.com",
                                  "payloadJson": "{\\\"forceFail\\\":true}",
                                  "priority": "high",
                                  "scheduledAt": "%s"
                                }
                                """.formatted(scheduledAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sendStatus").value("scheduled"))
                .andReturn();

        String notificationId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("notificationTaskId")
                .asText();

        Thread.sleep(1200L);

        mockMvc.perform(post("/api/notifications/run-due")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sendStatus").value("failed"))
                .andExpect(jsonPath("$.data[0].retryCount").value(1));

        mockMvc.perform(post("/api/notifications/run-due")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notificationTaskId").value(notificationId))
                .andExpect(jsonPath("$.data[0].sendStatus").value("failed"))
                .andExpect(jsonPath("$.data[0].retryCount").value(2));

        mockMvc.perform(post("/api/notifications/run-due")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notificationTaskId").value(notificationId))
                .andExpect(jsonPath("$.data[0].sendStatus").value("dead_letter"))
                .andExpect(jsonPath("$.data[0].retryCount").value(3))
                .andExpect(jsonPath("$.data[0].deadLetterReason").value("retry limit exceeded"));
    }

    @Test
    void shouldCreateNotificationsFromApprovalLifecycle() throws Exception {
        NotificationFixture fixture = prepareFixture();
        String replenishmentTaskId = createReplenishmentTask(fixture);

        MvcResult approvalResult = mockMvc.perform(post("/api/approvals")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalType": "manual_replenishment_review",
                                  "relatedType": "replenishment_task",
                                  "relatedId": "%s",
                                  "currentHandlerId": "ops-manager",
                                  "remark": "manual approval creation"
                                }
                                """.formatted(replenishmentTaskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andReturn();

        String approvalId = objectMapper.readTree(approvalResult.getResponse().getContentAsString())
                .path("data")
                .path("approvalId")
                .asText();

        mockMvc.perform(post("/api/approvals/{id}/approve", approvalId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "approved by tenant-admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));

        MvcResult notificationsResult = mockMvc.perform(get("/api/notifications")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        JsonNode notifications = objectMapper.readTree(notificationsResult.getResponse().getContentAsString()).path("data");
        assertNotificationExists(notifications, "approval_pending", "ops-manager");
        assertNotificationExists(notifications, "approval_approved", "tenant-admin");
    }

    private NotificationFixture prepareFixture() throws Exception {
        JsonNode tenantData = registerTenant("notification-center-test");
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String storeId = connectStore(tenantId, organizationId, "shop-notification-" + organizationId);
        String productId = publishProduct(tenantId, storeId);
        String orderId = syncOrder(tenantId, storeId, productId);
        return new NotificationFixture(tenantId, organizationId, storeId, productId, orderId);
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "notification-owner",
                                  "mobile": "13800000001"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String connectStore(String tenantId, String organizationId, String platformShopId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stores/connect")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "%s",
                                  "shopName": "notification-store",
                                  "profitThreshold": 18.50,
                                  "riskThreshold": 72.00
                                }
                                """.formatted(organizationId, platformShopId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();
    }

    private String publishProduct(String tenantId, String storeId) throws Exception {
        MvcResult candidateResult = mockMvc.perform(post("/api/candidate-products")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "sourceType": "1688",
                                  "sourceUrl": "https://source.example.com/p/notification-001",
                                  "title": "notification-product",
                                  "category": "home",
                                  "estimatedProfit": 28.80,
                                  "riskLevel": "medium",
                                  "recommendationReason": "notification linkage test",
                                  "aiSummary": "notification linkage summary"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andReturn();
        String candidateProductId = objectMapper.readTree(candidateResult.getResponse().getContentAsString())
                .path("data")
                .path("candidateProductId")
                .asText();

        MvcResult draftResult = mockMvc.perform(post("/api/product-drafts/generate")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateProductId": "%s",
                                  "aiVersion": "ai-notification-v1",
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
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformProductId": "platform-notification-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();
    }

    private String syncOrder(String tenantId, String storeId, String productId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "notification-order-001",
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

    private String createReplenishmentTask(NotificationFixture fixture) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/replenishment-tasks")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9003",
                                  "suggestedQty": 18,
                                  "reasonText": "notification approval linkage"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("replenishmentTaskId")
                .asText();
    }

    private String findTemplateId(JsonNode templates, String templateCode) {
        for (JsonNode template : templates) {
            if (templateCode.equals(template.path("templateCode").asText())) {
                return template.path("notificationTemplateId").asText();
            }
        }
        throw new IllegalStateException("notification template not found for " + templateCode);
    }

    private void assertNotificationExists(JsonNode notifications, String templateCode, String targetReceiver) {
        for (JsonNode notification : notifications) {
            if (templateCode.equals(notification.path("templateCode").asText())
                    && targetReceiver.equals(notification.path("targetReceiver").asText())) {
                return;
            }
        }
        throw new IllegalStateException("notification not found for template " + templateCode + " and target " + targetReceiver);
    }
}

record NotificationFixture(
        String tenantId,
        String organizationId,
        String storeId,
        String productId,
        String orderId
) {
}
