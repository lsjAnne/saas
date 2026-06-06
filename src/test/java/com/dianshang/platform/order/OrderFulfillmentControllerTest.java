package com.dianshang.platform.order;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.fulfillment.application.FulfillmentService;
import com.dianshang.platform.fulfillment.domain.repository.FulfillmentTaskRepository;
import com.dianshang.platform.fulfillment.model.FulfillmentTask;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.order.application.OrderService;
import com.dianshang.platform.product.application.ProductMappingService;
import com.dianshang.platform.product.application.ProductService;
import com.dianshang.platform.saas.application.SaasTenantService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFulfillmentControllerTest {

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
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @BeforeEach
    void setUp() {
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
    void shouldSyncOrdersAndManageFulfillmentTasks() throws Exception {
        OrderFixture fixture = prepareOrderFixture();

        MvcResult syncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-9001",
                                  "productId": "%s",
                                  "skuId": "sku-9001",
                                  "quantity": 2,
                                  "unitPrice": 49.90,
                                  "buyerName": "李四",
                                  "buyerPhoneMask": "139****0001",
                                  "shippingAddress": "上海市浦东新区测试路88号"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reused").value(false))
                .andExpect(jsonPath("$.data.taskId").value(org.hamcrest.Matchers.startsWith("order-sync-")))
                .andReturn();

        JsonNode syncData = objectMapper.readTree(syncResult.getResponse().getContentAsString()).path("data");
        String orderId = syncData.path("orderId").asText();
        String fulfillmentTaskId = syncData.path("fulfillmentTaskId").asText();

        mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-9001"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reused").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId));

        mockMvc.perform(get("/api/orders")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderId").value(orderId))
                .andExpect(jsonPath("$.data[0].orderStatus").value("pending_fulfillment"));

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.orderId").value(orderId))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(fixture.productId()));

        mockMvc.perform(get("/api/fulfillment-tasks")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("pending_confirm"));

        mockMvc.perform(get("/api/fulfillment-tasks/{id}", fulfillmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fulfillmentTaskId").value(fulfillmentTaskId))
                .andExpect(jsonPath("$.data.status").value("pending_confirm"));

        mockMvc.perform(post("/api/fulfillment-tasks/{id}/confirm", fulfillmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_execute"));

        FulfillmentTask failedTask = fulfillmentTaskRepository.findByFulfillmentTaskId(fulfillmentTaskId)
                .orElseThrow()
                .withStatus("failed", "platform timeout");
        fulfillmentTaskRepository.save(failedTask);

        mockMvc.perform(post("/api/fulfillment-tasks/{id}/retry", fulfillmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_execute"))
                .andExpect(jsonPath("$.data.retryCount").value(1));
    }

    @Test
    void shouldBuildOmsBusAuditSplitRouteReverseAndWorkbench() throws Exception {
        OrderFixture fixture = prepareOrderFixture();

        MvcResult primarySyncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-9101",
                                  "productId": "%s",
                                  "skuId": "sku-9101",
                                  "quantity": 3,
                                  "unitPrice": 89.90,
                                  "buyerName": "王五",
                                  "buyerPhoneMask": "139****3001",
                                  "shippingAddress": "上海市浦东新区张江路 188 号"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode primarySyncData = objectMapper.readTree(primarySyncResult.getResponse().getContentAsString()).path("data");
        String primaryOrderId = primarySyncData.path("orderId").asText();
        String primaryFulfillmentTaskId = primarySyncData.path("fulfillmentTaskId").asText();

        MvcResult mergeCandidateSyncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-9102",
                                  "productId": "%s",
                                  "skuId": "sku-9102",
                                  "quantity": 1,
                                  "unitPrice": 39.90,
                                  "buyerName": "王五",
                                  "buyerPhoneMask": "139****3001",
                                  "shippingAddress": "上海市浦东新区张江路 188 号"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        String mergeCandidateOrderId = objectMapper.readTree(mergeCandidateSyncResult.getResponse().getContentAsString())
                .path("data")
                .path("orderId")
                .asText();

        MvcResult riskySyncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-9199",
                                  "productId": "%s",
                                  "skuId": "sku-9199",
                                  "quantity": 1,
                                  "unitPrice": 5200.00,
                                  "buyerName": "高风险客户",
                                  "buyerPhoneMask": "138****9999",
                                  "shippingAddress": "待核"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        String riskyOrderId = objectMapper.readTree(riskySyncResult.getResponse().getContentAsString())
                .path("data")
                .path("orderId")
                .asText();

        mockMvc.perform(get("/api/orders/oms-standardized")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].sourcePlatform").value("douyin"))
                .andExpect(jsonPath("$.data[0].standardOrderStatus").value("pending_fulfillment"));

        mockMvc.perform(post("/api/orders/{id}/audit-review", riskyOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(riskyOrderId))
                .andExpect(jsonPath("$.data.riskLevel").value("high"))
                .andExpect(jsonPath("$.data.addressValid").value(false))
                .andExpect(jsonPath("$.data.riskTags", org.hamcrest.Matchers.hasItem("address_invalid")))
                .andExpect(jsonPath("$.data.riskTags", org.hamcrest.Matchers.hasItem("high_amount")));

        mockMvc.perform(post("/api/orders/{id}/split", primaryOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "拆成主商品与赠品单",
                                  "children": [
                                    {
                                      "childLabel": "main-package",
                                      "quantity": 2,
                                      "orderType": "normal"
                                    },
                                    {
                                      "childLabel": "gift-package",
                                      "quantity": 1,
                                      "orderType": "gift"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentOrderId").value(primaryOrderId))
                .andExpect(jsonPath("$.data.splitStatus").value("split_completed"))
                .andExpect(jsonPath("$.data.children.length()").value(2))
                .andExpect(jsonPath("$.data.children[1].orderType").value("gift"));

        mockMvc.perform(post("/api/orders/merge")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "orderIds": ["%s", "%s"],
                                  "mergedOrderType": "combined",
                                  "remark": "同客同址合单"
                                }
                                """.formatted(fixture.storeId(), primaryOrderId, mergeCandidateOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.orderIds.length()").value(2))
                .andExpect(jsonPath("$.data.mergedOrderType").value("combined"));

        mockMvc.perform(post("/api/orders/{id}/route-plan", primaryOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseCandidates": ["WH-SH-A", "WH-SH-B"],
                                  "shippingStrategy": "priority_delivery"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(primaryOrderId))
                .andExpect(jsonPath("$.data.priorityCode").value("high"))
                .andExpect(jsonPath("$.data.routeWarehouseCode").value("WH-SH-A"))
                .andExpect(jsonPath("$.data.shippingStrategy").value("priority_delivery"));

        mockMvc.perform(post("/api/orders/{id}/reverse-status", riskyOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reverseStatus": "refunding",
                                  "remark": "高风险订单发起退款"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(riskyOrderId))
                .andExpect(jsonPath("$.data.reverseStatus").value("refunding"))
                .andExpect(jsonPath("$.data.reverseCategory").value("refund"));

        FulfillmentTask manualRequiredTask = fulfillmentTaskRepository.findByFulfillmentTaskId(primaryFulfillmentTaskId)
                .orElseThrow()
                .withStatus("manual_required", "warehouse timeout");
        fulfillmentTaskRepository.save(manualRequiredTask);

        mockMvc.perform(post("/api/fulfillment-tasks/{id}/exception-replay", primaryFulfillmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fulfillmentTaskId").value(primaryFulfillmentTaskId))
                .andExpect(jsonPath("$.data.status").value("pending_execute"))
                .andExpect(jsonPath("$.data.replayCount").value(1));

        mockMvc.perform(get("/api/orders/{id}/orchestration-view", primaryOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(primaryOrderId))
                .andExpect(jsonPath("$.data.fulfillmentTaskId").value(primaryFulfillmentTaskId))
                .andExpect(jsonPath("$.data.splitChildCount").value(2))
                .andExpect(jsonPath("$.data.mergeOrderCount").value(2))
                .andExpect(jsonPath("$.data.routeWarehouseCode").value("WH-SH-A"))
                .andExpect(jsonPath("$.data.financeSyncStatus").value("pending"))
                .andExpect(jsonPath("$.data.wmsSyncStatus").value("ready"))
                .andExpect(jsonPath("$.data.tmsSyncStatus").value("pending"));

        mockMvc.perform(get("/api/orders/oms-workbench")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.standardizedOrderCount").value(3))
                .andExpect(jsonPath("$.data.riskOrderCount").value(1))
                .andExpect(jsonPath("$.data.splitOrderCount").value(1))
                .andExpect(jsonPath("$.data.mergedOrderGroupCount").value(1))
                .andExpect(jsonPath("$.data.manualReplayCount").value(1))
                .andExpect(jsonPath("$.data.reverseOrderCount").value(1));
    }

    @Test
    void shouldManageTmsCarrierShipmentTrackingSettlementPodReverseAndTower() throws Exception {
        OrderFixture fixture = prepareOrderFixture();

        MvcResult syncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "dy-order-tms-9201",
                                  "productId": "%s",
                                  "skuId": "sku-9201",
                                  "quantity": 2,
                                  "unitPrice": 76.90,
                                  "buyerName": "buyer-li",
                                  "buyerPhoneMask": "137****2001",
                                  "shippingAddress": "Hangzhou Yuhang Tongxie Road 66"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode syncData = objectMapper.readTree(syncResult.getResponse().getContentAsString()).path("data");
        String orderId = syncData.path("orderId").asText();
        String fulfillmentTaskId = syncData.path("fulfillmentTaskId").asText();

        MvcResult carrierResult = mockMvc.perform(post("/api/tms/carriers")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "carrierName": "SF Express Premium",
                                  "carrierCode": "SF-EXP",
                                  "channelType": "express",
                                  "serviceScope": "national"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.carrierStatus").value("active"))
                .andReturn();
        String carrierId = objectMapper.readTree(carrierResult.getResponse().getContentAsString())
                .path("data")
                .path("carrierId")
                .asText();

        MvcResult shipmentResult = mockMvc.perform(post("/api/tms/shipments")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fulfillmentTaskId": "%s",
                                  "carrierId": "%s",
                                  "shippingMode": "express",
                                  "freightAmount": 18.50,
                                  "originCity": "涓婃捣",
                                  "destinationCity": "鏉窞"
                                }
                                """.formatted(fulfillmentTaskId, carrierId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fulfillmentTaskId").value(fulfillmentTaskId))
                .andExpect(jsonPath("$.data.shipmentStatus").value("in_transit"))
                .andExpect(jsonPath("$.data.labelStatus").value("generated"))
                .andReturn();
        String shipmentId = objectMapper.readTree(shipmentResult.getResponse().getContentAsString())
                .path("data")
                .path("shipmentId")
                .asText();

        mockMvc.perform(post("/api/tms/shipments/{id}/tracking-events", shipmentId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingStatus": "in_transit",
                                  "locationText": "Hangzhou Sorting Center",
                                  "remark": "route collected"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentId").value(shipmentId))
                .andExpect(jsonPath("$.data.latestTrackingStatus").value("in_transit"))
                .andExpect(jsonPath("$.data.eventCount").value(1));

        mockMvc.perform(post("/api/tms/freight-settlements")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shipmentId": "%s",
                                  "settleMode": "monthly",
                                  "costType": "freight",
                                  "billableWeight": 2.40,
                                  "freightAmount": 18.50
                                }
                                """.formatted(shipmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentId").value(shipmentId))
                .andExpect(jsonPath("$.data.settlementStatus").value("pending"))
                .andExpect(jsonPath("$.data.costType").value("freight"));

        mockMvc.perform(post("/api/tms/shipments/{id}/sign-off", shipmentId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signStatus": "delivered",
                                  "proofType": "electronic_pod",
                                  "remark": "customer signed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentId").value(shipmentId))
                .andExpect(jsonPath("$.data.signStatus").value("delivered"))
                .andExpect(jsonPath("$.data.podStatus").value("archived"));

        mockMvc.perform(post("/api/tms/reverse-logistics")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "shipmentId": "%s",
                                  "carrierId": "%s",
                                  "reverseType": "return_pickup",
                                  "remark": "after sale reverse pickup"
                                }
                                """.formatted(orderId, shipmentId, carrierId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.reverseType").value("return_pickup"))
                .andExpect(jsonPath("$.data.reverseStatus").value("initiated"));

        mockMvc.perform(get("/api/tms/control-tower")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.carrierCount").value(1))
                .andExpect(jsonPath("$.data.shipmentCount").value(1))
                .andExpect(jsonPath("$.data.signedShipmentCount").value(1))
                .andExpect(jsonPath("$.data.reverseLogisticsCount").value(1))
                .andExpect(jsonPath("$.data.latestTrackingStatus").value("in_transit"))
                .andExpect(jsonPath("$.data.settlementPendingCount").value(1))
                .andExpect(jsonPath("$.data.podArchiveCount").value(1));
    }

    private OrderFixture prepareOrderFixture() throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "订单履约测试中心",
                                  "ownerName": "王五",
                                  "mobile": "13900000000"
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
                                  "platformShopId": "shop-order-301",
                                  "shopName": "订单测试店",
                                  "profitThreshold": 18.00,
                                  "riskThreshold": 70.00
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
                                  "sourceUrl": "https://source.example.com/p/order-3001",
                                  "title": "多功能收纳架",
                                  "category": "家居",
                                  "estimatedProfit": 21.50,
                                  "riskLevel": "low",
                                  "recommendationReason": "可用于订单履约主链路验证",
                                  "aiSummary": "系统生成订单测试商品"
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
                                  "aiVersion": "ai-stage3-v1",
                                  "suggestedPrice": 99.90
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
                                  "platformProductId": "dp-order-3001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();

        return new OrderFixture(tenantId, storeId, productId);
    }
}

record OrderFixture(String tenantId, String storeId, String productId) {
}
