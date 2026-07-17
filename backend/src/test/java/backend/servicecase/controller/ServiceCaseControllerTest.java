package backend.servicecase.controller;

import backend.audit.application.AuditLogService;
import backend.fulfillment.application.FulfillmentService;
import backend.order.application.OrderService;
import backend.organization.application.OrganizationService;
import backend.product.application.ProductMappingService;
import backend.product.application.ProductService;
import backend.saas.application.SaasTenantService;
import backend.servicecase.application.ServiceCaseService;
import backend.store.application.StoreChannelService;
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
class ServiceCaseControllerTest {

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
    private StoreChannelService storeChannelService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMappingService productMappingService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FulfillmentService fulfillmentService;

    @Autowired
    private ServiceCaseService serviceCaseService;

    @BeforeEach
    void setUp() {
        serviceCaseService.clear();
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
    void shouldManageTicketsAndAfterSales() throws Exception {
        ServiceCaseFixture fixture = prepareFixture();

        MvcResult syncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-service-1",
                                  "productId": "%s",
                                  "quantity": 1,
                                  "unitPrice": 69.90
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();

        String orderId = objectMapper.readTree(syncResult.getResponse().getContentAsString())
                .path("data")
                .path("orderId")
                .asText();

        String ticketId = serviceCaseService.createTicketSeed(
                        fixture.tenantId(),
                        fixture.storeId(),
                        orderId,
                        "customer-1001",
                        false)
                .ticketId();

        mockMvc.perform(get("/api/tickets")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ticketId").value(ticketId));

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketStatus").value("open"));

        mockMvc.perform(post("/api/tickets/{id}/reply-suggestion", ticketId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketStatus").value("processing"))
                .andExpect(jsonPath("$.data.aiReplySuggestion").isNotEmpty());

        MvcResult afterSaleCreateResult = mockMvc.perform(post("/api/after-sales")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "afterSaleType": "refund",
                                  "reasonText": "买家反馈尺寸不合适",
                                  "evidenceBlob": "{\\\"images\\\":[\\\"e1.png\\\"]}"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("created"))
                .andReturn();

        String afterSaleId = objectMapper.readTree(afterSaleCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("afterSaleId")
                .asText();

        mockMvc.perform(get("/api/after-sales/{id}", afterSaleId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.afterSaleType").value("refund"));

        mockMvc.perform(post("/api/after-sales/{id}/submit-approval", afterSaleId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("reviewing"));

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.orderStatus").value("after_sale"));
    }

    private ServiceCaseFixture prepareFixture() throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "瀹㈡湇鍞悗娴嬭瘯涓績",
                                  "ownerName": "鍛ㄤ竷",
                                  "mobile": "13600000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tenantData = objectMapper.readTree(tenantResult.getResponse().getContentAsString()).path("data");
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();

        MvcResult storeResult = mockMvc.perform(post("/api/stores/connect")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "shop-service-501",
                                  "shopName": "客服测试店",
                                  "profitThreshold": 22.00,
                                  "riskThreshold": 68.00
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andReturn();
        String storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();

        MvcResult candidateResult = mockMvc.perform(post("/api/candidate-products")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "sourceType": "1688",
                                  "sourceUrl": "https://source.example.com/p/service-5001",
                                  "title": "便携榨汁杯",
                                  "category": "鍘ㄦ埧鐢靛櫒",
                                  "estimatedProfit": 18.00,
                                  "riskLevel": "low",
                                  "recommendationReason": "閫傚悎瀹㈡湇鍞悗閾捐矾鑱旇皟",
                                  "aiSummary": "绯荤粺鑷姩鐢熸垚鍞悗娴嬭瘯鍟嗗搧"
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
                                  "aiVersion": "ai-stage4-v1",
                                  "suggestedPrice": 79.90
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
                                  "platformProductId": "dp-service-5001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();

        return new ServiceCaseFixture(tenantId, storeId, productId);
    }
}

record ServiceCaseFixture(String tenantId, String storeId, String productId) {
}

