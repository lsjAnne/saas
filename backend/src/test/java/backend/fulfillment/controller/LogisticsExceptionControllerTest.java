package backend.fulfillment.controller;

import backend.audit.application.AuditLogService;
import backend.exceptioncenter.application.ExceptionService;
import backend.exceptioncenter.domain.repository.ExceptionTaskRepository;
import backend.exceptioncenter.model.ExceptionTask;
import backend.fulfillment.application.FulfillmentService;
import backend.order.application.OrderService;
import backend.organization.application.OrganizationService;
import backend.product.application.ProductMappingService;
import backend.product.application.ProductService;
import backend.saas.application.SaasTenantService;
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

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogisticsExceptionControllerTest {

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
    private ExceptionService exceptionService;

    @Autowired
    private ExceptionTaskRepository exceptionTaskRepository;

    @BeforeEach
    void setUp() {
        exceptionService.clear();
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
    void shouldHandleLogisticsCallbackAndExceptionCenter() throws Exception {
        LogisticsFixture fixture = prepareFulfillmentFixture();

        MvcResult syncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-logistics-1",
                                  "productId": "%s",
                                  "quantity": 1,
                                  "unitPrice": 89.90
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode syncData = objectMapper.readTree(syncResult.getResponse().getContentAsString()).path("data");
        String fulfillmentTaskId = syncData.path("fulfillmentTaskId").asText();

        mockMvc.perform(post("/api/fulfillment-tasks/{id}/confirm", fulfillmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_execute"));

        mockMvc.perform(post("/api/fulfillment-tasks/{id}/logistics-records", fulfillmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingNumber": "SF1234567890",
                                  "logisticsCompany": "椤轰赴",
                                  "logisticsStatus": "exception"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingNumber").value("SF1234567890"))
                .andExpect(jsonPath("$.data.logisticsStatus").value("exception"));

        mockMvc.perform(get("/api/fulfillment-tasks/{id}/logistics-records", fulfillmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        MvcResult exceptionsResult = mockMvc.perform(get("/api/exceptions")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].exceptionType").value("logistics_exception"))
                .andReturn();

        String autoExceptionId = objectMapper.readTree(exceptionsResult.getResponse().getContentAsString())
                .path("data")
                .get(0)
                .path("exceptionTaskId")
                .asText();

        mockMvc.perform(get("/api/exceptions/{id}", autoExceptionId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("new"));

        mockMvc.perform(post("/api/exceptions/{id}/escalate", autoExceptionId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "ops-lead",
                                  "remark": "鍗囩骇鍒板饱绾﹁礋璐ｄ汉"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("escalated"));

        mockMvc.perform(post("/api/exceptions/{id}/process", autoExceptionId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "manual_sync_logistics",
                                  "operatorId": "ops-lead",
                                  "remark": "test-logistics-resolution"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("resolved"));

        ExceptionTask ignoredSeed = exceptionTaskRepository.save(new ExceptionTask(
                null,
                fixture.storeId(),
                "order_main",
                "order-9999",
                "address_exception",
                "P2",
                "todo",
                "鍦板潃淇℃伅缂哄け",
                null,
                OffsetDateTime.now()
        ));

        mockMvc.perform(post("/api/exceptions/{id}/ignore", ignoredSeed.exceptionTaskId())
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "tenant-admin",
                                  "remark": "娴嬭瘯鍗曪紝鏃犻渶澶勭悊"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ignored"));
    }

    private LogisticsFixture prepareFulfillmentFixture() throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "鐗╂祦寮傚父娴嬭瘯涓績",
                                  "ownerName": "璧靛叚",
                                  "mobile": "13700000000"
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
                                  "platformShopId": "shop-logistics-401",
                                  "shopName": "物流测试店",
                                  "profitThreshold": 20.00,
                                  "riskThreshold": 75.00
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
                                  "sourceUrl": "https://source.example.com/p/logistics-4001",
                                  "title": "户外折叠桌",
                                  "category": "鎴峰鐢ㄥ搧",
                                  "estimatedProfit": 25.00,
                                  "riskLevel": "low",
                                  "recommendationReason": "閫傚悎鍋氱墿娴佷笌寮傚父鑱旇皟",
                                  "aiSummary": "绯荤粺鑷姩鐢熸垚鐗╂祦娴嬭瘯鍟嗗搧"
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
                                  "aiVersion": "ai-stage3-v2",
                                  "suggestedPrice": 149.90
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
                                  "platformProductId": "dp-logistics-4001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();

        return new LogisticsFixture(tenantId, storeId, productId);
    }
}

record LogisticsFixture(String tenantId, String storeId, String productId) {
}

