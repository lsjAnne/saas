package com.dianshang.platform.inventory;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.inventory.application.InventoryService;
import com.dianshang.platform.inventory.domain.repository.InventorySnapshotRepository;
import com.dianshang.platform.inventory.model.InventorySnapshot;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.product.application.ProductMappingService;
import com.dianshang.platform.product.application.ProductService;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.model.OrderMain;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.store.application.StoreChannelService;
import com.dianshang.platform.supplier.application.SupplierService;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupplyChainControllerTest {

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
    private SupplierService supplierService;

    @Autowired
    private ProductMappingService productMappingService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        inventoryService.clear();
        productMappingService.clear();
        productService.clear();
        supplierService.clear();
        storeChannelService.clear();
        orderRepository.deleteAll();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldManageProductMappingsInventoryAndReplenishmentTasks() throws Exception {
        SupplyChainFixture fixture = prepareSupplyChainFixture();

        MvcResult mappingCreateResult = mockMvc.perform(post("/api/product-mappings")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "supplierId": "%s",
                                  "mappingType": "primary"
                                }
                                """.formatted(fixture.storeId(), fixture.productId(), fixture.primarySupplierId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mappingType").value("primary"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        String mappingId = objectMapper.readTree(mappingCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("productMappingId")
                .asText();

        mockMvc.perform(get("/api/product-mappings")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/api/product-mappings/{id}/switch-supplier", mappingId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "%s"
                                }
                                """.formatted(fixture.backupSupplierId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.backupSupplierId()))
                .andExpect(jsonPath("$.data.active").value(true));

        InventorySnapshot snapshot = inventorySnapshotRepository.save(new InventorySnapshot(
                null,
                fixture.storeId(),
                fixture.productId(),
                "sku-9001",
                28,
                3,
                5,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));

        mockMvc.perform(get("/api/inventory-snapshots")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].inventorySnapshotId").value(snapshot.inventorySnapshotId()));

        mockMvc.perform(put("/api/inventory-snapshots/{id}/safety-stock", snapshot.inventorySnapshotId())
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "safetyStock": 12
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyStock").value(12));

        MvcResult replenishmentCreateResult = mockMvc.perform(post("/api/replenishment-tasks")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9001",
                                  "suggestedQty": 50,
                                  "reasonText": "库存低于安全库存，建议补货"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value("draft"))
                .andExpect(jsonPath("$.data.approvalStatus").value("not_required"))
                .andReturn();

        String replenishmentTaskId = objectMapper.readTree(replenishmentCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("replenishmentTaskId")
                .asText();

        mockMvc.perform(get("/api/replenishment-tasks")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/api/replenishment-tasks/{id}/submit-approval", replenishmentTaskId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value("pending_approval"))
                .andExpect(jsonPath("$.data.approvalStatus").value("pending"));
    }

    @Test
    void shouldManagePurchaseRequests() throws Exception {
        SupplyChainFixture fixture = prepareSupplyChainFixture();

        MvcResult purchaseRequestCreateResult = mockMvc.perform(post("/api/purchase-requests")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierId": "%s",
                                  "expectedDeliveryDate": "2026-06-10",
                                  "reasonText": "大促前补齐爆品备货",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-9001",
                                      "requestedQty": 48,
                                      "targetUnitPrice": 56.80
                                    }
                                  ]
                                }
                                """.formatted(fixture.storeId(), fixture.primarySupplierId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("draft"))
                .andExpect(jsonPath("$.data.approvalStatus").value("not_submitted"))
                .andExpect(jsonPath("$.data.totalRequestedQty").value(48))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andReturn();

        String purchaseRequestId = objectMapper.readTree(purchaseRequestCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("purchaseRequestId")
                .asText();

        mockMvc.perform(get("/api/purchase-requests")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].purchaseRequestId").value(purchaseRequestId));

        mockMvc.perform(get("/api/purchase-requests/{id}", purchaseRequestId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.items[0].productId").value(fixture.productId()));

        mockMvc.perform(post("/api/purchase-requests/{id}/submit", purchaseRequestId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("submitted"))
                .andExpect(jsonPath("$.data.approvalStatus").value("pending_review"));
    }

    @Test
    void shouldCreateApproveAndDispatchPurchaseOrders() throws Exception {
        SupplyChainFixture fixture = prepareSupplyChainFixture();
        MvcResult transferStoreResult = mockMvc.perform(post("/api/stores/connect")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "kuaishou",
                                  "platformShopId": "shop-302",
                                  "shopName": "供应链调拨分店",
                                  "profitThreshold": 16.00,
                                  "riskThreshold": 65.00
                                }
                                """.formatted(fixture.organizationId())))
                .andExpect(status().isOk())
                .andReturn();
        String targetStoreId = objectMapper.readTree(transferStoreResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();

        MvcResult purchaseRequestCreateResult = mockMvc.perform(post("/api/purchase-requests")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierId": "%s",
                                  "expectedDeliveryDate": "2026-06-12",
                                  "reasonText": "季度采购备货",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-9010",
                                      "requestedQty": 60,
                                      "targetUnitPrice": 45.50
                                    }
                                  ]
                                }
                                """.formatted(fixture.storeId(), fixture.primarySupplierId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();

        String purchaseRequestId = objectMapper.readTree(purchaseRequestCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("purchaseRequestId")
                .asText();

        mockMvc.perform(post("/api/purchase-requests/{id}/submit", purchaseRequestId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("submitted"));

        MvcResult purchaseOrderCreateResult = mockMvc.perform(post("/api/purchase-orders")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purchaseRequestId": "%s",
                                  "remark": "生成首批采购单"
                                }
                                """.formatted(purchaseRequestId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("draft"))
                .andExpect(jsonPath("$.data.approvalStatus").value("not_submitted"))
                .andExpect(jsonPath("$.data.dispatchStatus").value("not_dispatched"))
                .andReturn();

        String purchaseOrderId = objectMapper.readTree(purchaseOrderCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("purchaseOrderId")
                .asText();

        mockMvc.perform(get("/api/purchase-orders")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].purchaseOrderId").value(purchaseOrderId));

        mockMvc.perform(get("/api/purchase-orders/{id}", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchaseRequestId").value(purchaseRequestId))
                .andExpect(jsonPath("$.data.items[0].requestedQty").value(60));

        mockMvc.perform(post("/api/purchase-orders/{id}/submit-approval", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("pending_approval"))
                .andExpect(jsonPath("$.data.approvalStatus").value("pending"));

        MvcResult approvalListResult = mockMvc.perform(get("/api/approvals")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode approvals = objectMapper.readTree(approvalListResult.getResponse().getContentAsString()).path("data");
        String purchaseOrderApprovalId = findApprovalId(approvals, "purchase_order", purchaseOrderId);

        mockMvc.perform(post("/api/approvals/{id}/approve", purchaseOrderApprovalId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "采购单审批通过"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));

        mockMvc.perform(get("/api/purchase-orders/{id}", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("approved"))
                .andExpect(jsonPath("$.data.approvalStatus").value("approved"));

        mockMvc.perform(post("/api/purchase-orders/{id}/dispatch", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("dispatched"))
                .andExpect(jsonPath("$.data.dispatchStatus").value("dispatched"));

        mockMvc.perform(post("/api/purchase-orders/{id}/receive", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-9010",
                                      "receivedQty": 54,
                                      "batchNo": "BATCH-20260612-01",
                                      "productionDate": "2026-05-20",
                                      "expiryDate": "2027-05-19"
                                    }
                                  ],
                                  "remark": "首批采购收货入库"
                                }
                                """.formatted(fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundStatus").value("completed"))
                .andExpect(jsonPath("$.data.items[0].receivedQty").value(54));

        mockMvc.perform(get("/api/purchase-orders/{id}", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("received"))
                .andExpect(jsonPath("$.data.receivingStatus").value("completed"))
                .andExpect(jsonPath("$.data.inboundStatus").value("completed"))
                .andExpect(jsonPath("$.data.discrepancyStatus").value("reported"));

        mockMvc.perform(get("/api/purchase-receipts")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].purchaseOrderId").value(purchaseOrderId))
                .andExpect(jsonPath("$.data[0].items[0].receivedQty").value(54))
                .andExpect(jsonPath("$.data[0].items[0].batchNo").value("BATCH-20260612-01"));

        mockMvc.perform(get("/api/purchase-receipt-discrepancies")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].purchaseOrderId").value(purchaseOrderId))
                .andExpect(jsonPath("$.data[0].discrepancyType").value("short_receive"))
                .andExpect(jsonPath("$.data[0].discrepancyQty").value(6));

        mockMvc.perform(post("/api/purchase-orders/{id}/returns", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-9010",
                                      "batchNo": "BATCH-20260612-01",
                                      "returnQty": 4,
                                      "reasonText": "来货破损退回供应商"
                                    }
                                  ],
                                  "remark": "部分退货处理"
                                }
                                """.formatted(fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnStatus").value("returned"))
                .andExpect(jsonPath("$.data.items[0].returnQty").value(4));

        mockMvc.perform(get("/api/purchase-returns")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].purchaseOrderId").value(purchaseOrderId))
                .andExpect(jsonPath("$.data[0].items[0].returnQty").value(4));

        mockMvc.perform(get("/api/purchase-orders/{id}", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnStatus").value("partial_returned"));

        mockMvc.perform(get("/api/inventory-snapshots")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productId").value(fixture.productId()))
                .andExpect(jsonPath("$.data[0].skuId").value("sku-9010"))
                .andExpect(jsonPath("$.data[0].availableStock").value(50));

        mockMvc.perform(get("/api/inventory-ledgers")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productId").value(fixture.productId()))
                .andExpect(jsonPath("$.data[0].skuId").value("sku-9010"))
                .andExpect(jsonPath("$.data[0].availableStock").value(50))
                .andExpect(jsonPath("$.data[0].inboundTotal").value(54))
                .andExpect(jsonPath("$.data[0].outboundTotal").value(4))
                .andExpect(jsonPath("$.data[0].riskLevel").value("healthy"));

        mockMvc.perform(get("/api/inventory-transactions")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].direction").value("outbound"))
                .andExpect(jsonPath("$.data[0].transactionType").value("purchase_return"))
                .andExpect(jsonPath("$.data[0].quantity").value(4))
                .andExpect(jsonPath("$.data[1].direction").value("inbound"))
                .andExpect(jsonPath("$.data[1].transactionType").value("purchase_receipt"))
                .andExpect(jsonPath("$.data[1].quantity").value(54));

        MvcResult transferCreateResult = mockMvc.perform(post("/api/inventory-transfers")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceStoreId": "%s",
                                  "targetStoreId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9010",
                                  "batchNo": "BATCH-20260612-01",
                                  "transferQty": 12,
                                  "reasonText": "调拨补足分店库存"
                                }
                                """.formatted(fixture.storeId(), targetStoreId, fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferStatus").value("frozen"))
                .andExpect(jsonPath("$.data.batchNo").value("BATCH-20260612-01"))
                .andReturn();
        String transferId = objectMapper.readTree(transferCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("transferId")
                .asText();

        mockMvc.perform(get("/api/inventory-freezes")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].relatedId").value(transferId))
                .andExpect(jsonPath("$.data[0].freezeStatus").value("active"))
                .andExpect(jsonPath("$.data[0].freezeQty").value(12));

        mockMvc.perform(get("/api/inventory-snapshots")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].availableStock").value(38))
                .andExpect(jsonPath("$.data[0].reservedStock").value(12));

        mockMvc.perform(post("/api/inventory-transfers/{id}/complete", transferId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferStatus").value("completed"));

        mockMvc.perform(get("/api/inventory-transfers")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].targetStoreId").value(targetStoreId))
                .andExpect(jsonPath("$.data[0].transferStatus").value("completed"));

        mockMvc.perform(get("/api/inventory-freezes")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].freezeStatus").value("released"));

        MvcResult ledgerResult = mockMvc.perform(get("/api/inventory-ledgers")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ledgers = objectMapper.readTree(ledgerResult.getResponse().getContentAsString()).path("data");
        JsonNode sourceLedger = findNodeByField(ledgers, "storeId", fixture.storeId());
        JsonNode targetLedger = findNodeByField(ledgers, "storeId", targetStoreId);
        assertNotNull(sourceLedger);
        assertNotNull(targetLedger);
        assertEquals(38, sourceLedger.path("availableStock").asInt());
        assertEquals(0, sourceLedger.path("reservedStock").asInt());
        assertEquals(54, sourceLedger.path("inboundTotal").asInt());
        assertEquals(16, sourceLedger.path("outboundTotal").asInt());
        assertEquals(12, targetLedger.path("availableStock").asInt());
        assertEquals(12, targetLedger.path("inboundTotal").asInt());

        MvcResult batchResult = mockMvc.perform(get("/api/inventory-batches")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode batches = objectMapper.readTree(batchResult.getResponse().getContentAsString()).path("data");
        JsonNode sourceBatch = findNodeByField(batches, "sourceId", purchaseOrderId);
        JsonNode targetBatch = findNodeByField(batches, "sourceId", transferId);
        assertNotNull(sourceBatch);
        assertNotNull(targetBatch);
        assertEquals("BATCH-20260612-01", sourceBatch.path("batchNo").asText());
        assertEquals(38, sourceBatch.path("availableQty").asInt());
        assertEquals(0, sourceBatch.path("lockedQty").asInt());
        assertEquals("2027-05-19", sourceBatch.path("expiryDate").asText());
        assertEquals("inventory_transfer", targetBatch.path("sourceType").asText());
        assertEquals(12, targetBatch.path("availableQty").asInt());

        MvcResult costLotResult = mockMvc.perform(get("/api/inventory-cost-lots")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode costLots = objectMapper.readTree(costLotResult.getResponse().getContentAsString()).path("data");
        JsonNode sourceCostLot = findNodeByField(costLots, "sourceId", purchaseOrderId);
        JsonNode targetCostLot = findNodeByField(costLots, "sourceId", transferId);
        assertNotNull(sourceCostLot);
        assertNotNull(targetCostLot);
        assertEquals(38, sourceCostLot.path("remainingQty").asInt());
        assertEquals(0, sourceCostLot.path("remainingAmount").decimalValue().compareTo(new BigDecimal("1729.00")));
        assertEquals(0, sourceCostLot.path("unitCost").decimalValue().compareTo(new BigDecimal("45.50")));
        assertEquals(12, targetCostLot.path("remainingQty").asInt());
        assertEquals(0, targetCostLot.path("remainingAmount").decimalValue().compareTo(new BigDecimal("546.00")));

        mockMvc.perform(get("/api/payable-ledgers")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].bizType").value("purchase_return"))
                .andExpect(jsonPath("$.data[0].direction").value("payable_decrease"))
                .andExpect(jsonPath("$.data[0].entryAmount").value(182.0))
                .andExpect(jsonPath("$.data[1].bizType").value("purchase_receipt"))
                .andExpect(jsonPath("$.data[1].direction").value("payable_increase"))
                .andExpect(jsonPath("$.data[1].entryAmount").value(2457.0));

        mockMvc.perform(get("/api/supplier-reconciliations")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data[0].receiptAmount").value(2457.0))
                .andExpect(jsonPath("$.data[0].returnAmount").value(182.0))
                .andExpect(jsonPath("$.data[0].netPayableAmount").value(2275.0))
                .andExpect(jsonPath("$.data[0].pendingDiscrepancyCount").value(1))
                .andExpect(jsonPath("$.data[0].reconciliationStatus").value("exception"));

        mockMvc.perform(post("/api/purchase-expense-allocations")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purchaseOrderId": "%s",
                                  "expenseType": "freight",
                                  "allocationRule": "received_qty_average",
                                  "expenseAmount": 150.00,
                                  "remark": "采购运费分摊"
                                }
                                """.formatted(purchaseOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchaseOrderId").value(purchaseOrderId))
                .andExpect(jsonPath("$.data.expenseType").value("freight"))
                .andExpect(jsonPath("$.data.allocatedQty").value(50))
                .andExpect(jsonPath("$.data.unitAllocatedExpense").value(3.0));

        mockMvc.perform(get("/api/purchase-expense-allocations")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].expenseAmount").value(150.0));

        mockMvc.perform(get("/api/purchase-cost-collections")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].purchaseOrderId").value(purchaseOrderId))
                .andExpect(jsonPath("$.data[0].basePurchaseCost").value(2275.0))
                .andExpect(jsonPath("$.data[0].allocatedExpense").value(150.0))
                .andExpect(jsonPath("$.data[0].totalCollectedCost").value(2425.0))
                .andExpect(jsonPath("$.data[0].unitCollectedCost").value(48.5))
                .andExpect(jsonPath("$.data[0].netReceiptQty").value(50))
                .andExpect(jsonPath("$.data[0].costLotCount").value(1));

        OffsetDateTime financeOrderTime = OffsetDateTime.now().minusHours(2);
        OrderMain settlementOrder = orderRepository.save(new OrderMain(
                null,
                fixture.storeId(),
                "sc-order-1001",
                "paid",
                "pending",
                BigDecimal.valueOf(580),
                BigDecimal.valueOf(180),
                "供应链财务联动客户",
                "137****7788",
                "深圳市南山区联动路 8 号",
                financeOrderTime.plusDays(1),
                financeOrderTime
        ));
        LocalDate periodStart = financeOrderTime.toLocalDate().minusDays(1);
        LocalDate periodEnd = financeOrderTime.toLocalDate().plusDays(1);

        MvcResult financeBillResult = mockMvc.perform(post("/api/finance-bills/generate")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "billType": "supply_chain_settlement",
                                  "periodStart": "%s",
                                  "periodEnd": "%s"
                                }
                                """.formatted(fixture.storeId(), periodStart, periodEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billStatus").value("draft"))
                .andExpect(jsonPath("$.data.incomeAmount").value(580))
                .andReturn();
        String financeBillId = objectMapper.readTree(financeBillResult.getResponse().getContentAsString())
                .path("data")
                .path("financeBillId")
                .asText();

        mockMvc.perform(post("/api/finance-bills/{id}/reconcile", financeBillId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billStatus").value("reconciled"));

        MvcResult settlementResult = mockMvc.perform(post("/api/finance-bills/{id}/settle", financeBillId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settlementType": "bank_transfer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementStatus").value("settled"))
                .andReturn();
        String settlementRecordId = objectMapper.readTree(settlementResult.getResponse().getContentAsString())
                .path("data")
                .path("settlementRecordId")
                .asText();

        mockMvc.perform(post("/api/finance-vouchers/archive")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "supply_chain_settlement",
                                  "referenceId": "%s",
                                  "voucherType": "settlement_voucher",
                                  "voucherAmount": 180.00,
                                  "remark": "采购库存结算联动归档"
                                }
                                """.formatted(fixture.storeId(), settlementRecordId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archiveStatus").value("archived"));

        mockMvc.perform(post("/api/customer-payments")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "paymentChannel": "online",
                                  "paymentAmount": 580.00,
                                  "remark": "供应链联动回款"
                                }
                                """.formatted(settlementOrder.orderId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("collected"));

        mockMvc.perform(get("/api/profit-statements")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .param("storeId", fixture.storeId())
                        .param("periodStart", periodStart.toString())
                        .param("periodEnd", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.salesOrderCount").value(1))
                .andExpect(jsonPath("$.data.salesIncomeAmount").value(580.0))
                .andExpect(jsonPath("$.data.estimatedCostAmount").value(400.0))
                .andExpect(jsonPath("$.data.grossProfitAmount").value(180.0))
                .andExpect(jsonPath("$.data.allocatedExpenseAmount").value(150.0))
                .andExpect(jsonPath("$.data.netProfitAmount").value(30.0))
                .andExpect(jsonPath("$.data.profitMarginRate").value(5.17))
                .andExpect(jsonPath("$.data.targetProfitThreshold").value(18.0))
                .andExpect(jsonPath("$.data.thresholdStatus").value("below_threshold"))
                .andExpect(jsonPath("$.data.outstandingReceivableAmount").value(0.0))
                .andExpect(jsonPath("$.data.expenseBreakdown.length()").value(1))
                .andExpect(jsonPath("$.data.expenseBreakdown[0].expenseType").value("freight"))
                .andExpect(jsonPath("$.data.expenseBreakdown[0].expenseAmount").value(150.0))
                .andExpect(jsonPath("$.data.expenseBreakdown[0].allocationCount").value(1));

        mockMvc.perform(get("/api/store-profit-reports")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .param("periodStart", periodStart.toString())
                        .param("periodEnd", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data[0].salesIncomeAmount").value(580.0))
                .andExpect(jsonPath("$.data[0].netProfitAmount").value(30.0))
                .andExpect(jsonPath("$.data[0].thresholdStatus").value("below_threshold"));

        mockMvc.perform(get("/api/inventory-transactions")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].transactionType").value("inventory_transfer_out"))
                .andExpect(jsonPath("$.data[0].quantity").value(12))
                .andExpect(jsonPath("$.data[1].transactionType").value("inventory_transfer_in"))
                .andExpect(jsonPath("$.data[1].quantity").value(12))
                .andExpect(jsonPath("$.data[2].transactionType").value("purchase_return"))
                .andExpect(jsonPath("$.data[3].transactionType").value("purchase_receipt"));
    }

    @Test
    void shouldManageSupplierAdmissionScoreInquirySettlementRiskAndLinkage() throws Exception {
        SupplyChainFixture fixture = prepareSupplyChainFixture();

        mockMvc.perform(post("/api/suppliers/{id}/admission-review", fixture.primarySupplierId())
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qualificationDocs": ["business_license", "tax_certificate", "bank_account"],
                                  "decision": "approved",
                                  "remark": "首轮准入通过"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.admissionStatus").value("approved"))
                .andExpect(jsonPath("$.data.whitelistFlag").value(true))
                .andExpect(jsonPath("$.data.qualificationComplete").value(true));

        mockMvc.perform(post("/api/suppliers/{id}/scorecards", fixture.primarySupplierId())
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryScore": 92,
                                  "fulfillmentScore": 88,
                                  "qualityScore": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.ratingGrade").value("A"))
                .andExpect(jsonPath("$.data.compositeScore").value(90.0));

        MvcResult inquiryResult = mockMvc.perform(post("/api/supplier-inquiries")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "inquiryTitle": "双供应商询比价",
                                  "targetQty": 80,
                                  "quotes": [
                                    {
                                      "supplierId": "%s",
                                      "quotedUnitPrice": 46.50,
                                      "deliveryDays": 3
                                    },
                                    {
                                      "supplierId": "%s",
                                      "quotedUnitPrice": 48.00,
                                      "deliveryDays": 5
                                    }
                                  ]
                                }
                                """.formatted(
                                fixture.storeId(),
                                fixture.productId(),
                                fixture.primarySupplierId(),
                                fixture.backupSupplierId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedSupplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.quotes.length()").value(2))
                .andReturn();
        String inquiryId = objectMapper.readTree(inquiryResult.getResponse().getContentAsString())
                .path("data")
                .path("inquiryId")
                .asText();

        mockMvc.perform(post("/api/suppliers/{id}/delivery-appointments", fixture.primarySupplierId())
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "purchaseReference": "po-demo-1",
                                  "appointmentDate": "2026-06-15",
                                  "plannedQty": 80,
                                  "remark": "预约第一批送货"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.appointmentStatus").value("scheduled"))
                .andExpect(jsonPath("$.data.plannedQty").value(80));

        mockMvc.perform(post("/api/supplier-settlement-statements")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierId": "%s",
                                  "statementPeriod": "2026-06",
                                  "accountPeriodDays": 30,
                                  "payableAmount": 3720.00,
                                  "dueDate": "2026-07-05"
                                }
                                """.formatted(fixture.storeId(), fixture.primarySupplierId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.accountPeriodDays").value(30))
                .andExpect(jsonPath("$.data.settlementStatus").value("pending"));

        mockMvc.perform(get("/api/supplier-settlement-statements")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .param("storeId", fixture.storeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].statementPeriod").value("2026-06"));

        mockMvc.perform(post("/api/suppliers/{id}/risk-events", fixture.primarySupplierId())
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "riskType": "delivery_delay",
                                  "severity": "high",
                                  "remark": "交付波动预警"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.warningLevel").value("high"))
                .andExpect(jsonPath("$.data.fallbackSupplierId").value(fixture.backupSupplierId()));

        mockMvc.perform(get("/api/suppliers/{id}/srm-linkage", fixture.primarySupplierId())
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierId").value(fixture.primarySupplierId()))
                .andExpect(jsonPath("$.data.admissionStatus").value("approved"))
                .andExpect(jsonPath("$.data.ratingGrade").value("A"))
                .andExpect(jsonPath("$.data.inquiryCount").value(1))
                .andExpect(jsonPath("$.data.deliveryAppointmentCount").value(1))
                .andExpect(jsonPath("$.data.settlementStatementCount").value(1))
                .andExpect(jsonPath("$.data.riskEventCount").value(1))
                .andExpect(jsonPath("$.data.latestInquiryId").value(inquiryId))
                .andExpect(jsonPath("$.data.erpSyncStatus").value("ready"))
                .andExpect(jsonPath("$.data.wmsInboundStatus").value("pending"));
    }

    @Test
    void shouldManageWmsWarehouseExecutionWaveLockCountReverseAndLinkage() throws Exception {
        SupplyChainFixture fixture = prepareSupplyChainFixture();

        MvcResult purchaseRequestCreateResult = mockMvc.perform(post("/api/purchase-requests")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierId": "%s",
                                  "expectedDeliveryDate": "2026-06-18",
                                  "reasonText": "wms restock",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-9101",
                                      "requestedQty": 40,
                                      "targetUnitPrice": 45.80
                                    }
                                  ]
                                }
                                """.formatted(fixture.storeId(), fixture.primarySupplierId(), fixture.productId())))
                .andExpect(status().isOk())
                .andReturn();
        String purchaseRequestId = objectMapper.readTree(purchaseRequestCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("purchaseRequestId")
                .asText();

        mockMvc.perform(post("/api/purchase-requests/{id}/submit", purchaseRequestId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("submitted"));

        MvcResult purchaseOrderCreateResult = mockMvc.perform(post("/api/purchase-orders")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purchaseRequestId": "%s",
                                  "remark": "wms purchase order"
                                }
                                """.formatted(purchaseRequestId)))
                .andExpect(status().isOk())
                .andReturn();
        String purchaseOrderId = objectMapper.readTree(purchaseOrderCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("purchaseOrderId")
                .asText();

        mockMvc.perform(post("/api/purchase-orders/{id}/submit-approval", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk());

        MvcResult approvalListResult = mockMvc.perform(get("/api/approvals")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andReturn();
        String purchaseOrderApprovalId = findApprovalId(
                objectMapper.readTree(approvalListResult.getResponse().getContentAsString()).path("data"),
                "purchase_order",
                purchaseOrderId
        );

        mockMvc.perform(post("/api/approvals/{id}/approve", purchaseOrderApprovalId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "wms approval"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/purchase-orders/{id}/dispatch", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/purchase-orders/{id}/receive", purchaseOrderId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-9101",
                                      "receivedQty": 40,
                                      "batchNo": "WMS-BATCH-20260618-01",
                                      "productionDate": "2026-06-01",
                                      "expiryDate": "2027-06-01"
                                    }
                                  ],
                                  "remark": "wms inbound baseline"
                                }
                                """.formatted(fixture.productId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/wms/warehouses")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "warehouseName": "鍗庝笢涓讳粨",
                                  "zoneCode": "Z-A",
                                  "locationCode": "A-01-01",
                                  "temperatureZone": "ambient"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.warehouseStatus").value("active"))
                .andExpect(jsonPath("$.data.zoneCode").value("Z-A"))
                .andExpect(jsonPath("$.data.locationCode").value("A-01-01"));

        mockMvc.perform(post("/api/wms/inbound-tasks")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9101",
                                  "batchNo": "WMS-BATCH-20260618-01",
                                  "warehouseCode": "WH-1",
                                  "zoneCode": "Z-A",
                                  "locationCode": "A-01-01",
                                  "taskType": "putaway",
                                  "quantity": 40
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionStatus").value("completed"))
                .andExpect(jsonPath("$.data.putawayStatus").value("stored"))
                .andExpect(jsonPath("$.data.locationCode").value("A-01-01"));

        mockMvc.perform(post("/api/wms/waves")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "warehouseCode": "WH-1",
                                  "waveType": "standard",
                                  "strategyCode": "fifo",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-9101",
                                      "locationCode": "A-01-01",
                                      "plannedQty": 12
                                    }
                                  ]
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waveStatus").value("released"))
                .andExpect(jsonPath("$.data.pickStrategy").value("fifo"))
                .andExpect(jsonPath("$.data.allocatedQty").value(12));

        mockMvc.perform(post("/api/wms/locks")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9101",
                                  "batchNo": "WMS-BATCH-20260618-01",
                                  "warehouseCode": "WH-1",
                                  "locationCode": "A-01-01",
                                  "lockQty": 5,
                                  "reasonText": "damage_hold"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockStatus").value("locked"))
                .andExpect(jsonPath("$.data.lockQty").value(5))
                .andExpect(jsonPath("$.data.locationCode").value("A-01-01"));

        mockMvc.perform(post("/api/wms/cycle-count-tasks")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9101",
                                  "warehouseCode": "WH-1",
                                  "locationCode": "A-01-01",
                                  "systemQty": 35,
                                  "countedQty": 33,
                                  "varianceReason": "count_loss"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countStatus").value("completed"))
                .andExpect(jsonPath("$.data.adjustmentType").value("shortage"))
                .andExpect(jsonPath("$.data.varianceQty").value(2));

        mockMvc.perform(post("/api/wms/reverse-inbounds")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "skuId": "sku-9101",
                                  "warehouseCode": "WH-1",
                                  "locationCode": "A-01-01",
                                  "reverseType": "return_inbound",
                                  "quantity": 4,
                                  "remark": "after sale return inbound"
                                }
                                """.formatted(fixture.storeId(), fixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reverseStatus").value("completed"))
                .andExpect(jsonPath("$.data.inventoryStatus").value("returned"))
                .andExpect(jsonPath("$.data.quantity").value(4));

        mockMvc.perform(get("/api/wms/linkage")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .param("storeId", fixture.storeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.warehouseCount").value(1))
                .andExpect(jsonPath("$.data.activeWaveCount").value(1))
                .andExpect(jsonPath("$.data.lockedBatchCount").value(1))
                .andExpect(jsonPath("$.data.cycleCountTaskCount").value(1))
                .andExpect(jsonPath("$.data.reverseInboundCount").value(1))
                .andExpect(jsonPath("$.data.omsSyncStatus").value("ready"))
                .andExpect(jsonPath("$.data.erpSyncStatus").value("ready"))
                .andExpect(jsonPath("$.data.tmsHandoverStatus").value("pending"));
    }

    private SupplyChainFixture prepareSupplyChainFixture() throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "星辰供应链中心",
                                  "ownerName": "张三",
                                  "mobile": "13800000000"
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
                                  "platformShopId": "shop-301",
                                  "shopName": "供应链测试店",
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
                                  "sourceUrl": "https://source.example.com/p/3001",
                                  "title": "多功能收纳箱",
                                  "category": "家居收纳",
                                  "estimatedProfit": 32.50,
                                  "riskLevel": "low",
                                  "recommendationReason": "季节性稳定，适合多渠道经营",
                                  "aiSummary": "可做场景收纳类内容"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andReturn();
        String candidateProductId = objectMapper.readTree(candidateResult.getResponse().getContentAsString())
                .path("data")
                .path("candidateProductId")
                .asText();

        MvcResult primarySupplierResult = mockMvc.perform(post("/api/suppliers")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierPlatformType": "1688",
                                  "supplierPlatformId": "sp-3001",
                                  "supplierName": "华东主供货商",
                                  "sourceUrl": "https://supplier.example.com/3001",
                                  "priceScore": 90,
                                  "deliveryScore": 87,
                                  "stabilityScore": 88,
                                  "riskLevel": "low",
                                  "dropshipSupportFlag": true
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andReturn();
        String primarySupplierId = objectMapper.readTree(primarySupplierResult.getResponse().getContentAsString())
                .path("data")
                .path("supplierId")
                .asText();

        MvcResult backupSupplierResult = mockMvc.perform(post("/api/suppliers")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierPlatformType": "1688",
                                  "supplierPlatformId": "sp-3002",
                                  "supplierName": "华南备用供货商",
                                  "sourceUrl": "https://supplier.example.com/3002",
                                  "priceScore": 86,
                                  "deliveryScore": 85,
                                  "stabilityScore": 84,
                                  "riskLevel": "medium",
                                  "dropshipSupportFlag": true
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andReturn();
        String backupSupplierId = objectMapper.readTree(backupSupplierResult.getResponse().getContentAsString())
                .path("data")
                .path("supplierId")
                .asText();

        MvcResult draftResult = mockMvc.perform(post("/api/product-drafts/generate")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateProductId": "%s",
                                  "aiVersion": "ai-stage2-v2",
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
                                  "platformProductId": "dp-3001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();

        return new SupplyChainFixture(tenantId, organizationId, storeId, productId, primarySupplierId, backupSupplierId);
    }

    private String findApprovalId(JsonNode approvals, String relatedType, String relatedId) {
        for (JsonNode approval : approvals) {
            if (relatedType.equals(approval.path("relatedType").asText())
                    && relatedId.equals(approval.path("relatedId").asText())) {
                return approval.path("approvalId").asText();
            }
        }
        throw new IllegalStateException("approval not found for relatedType=" + relatedType + ", relatedId=" + relatedId);
    }

    private JsonNode findNodeByField(JsonNode array, String fieldName, String expectedValue) {
        for (JsonNode node : array) {
            if (expectedValue.equals(node.path(fieldName).asText())) {
                return node;
            }
        }
        return null;
    }
}

record SupplyChainFixture(String tenantId,
                          String organizationId,
                          String storeId,
                          String productId,
                          String primarySupplierId,
                          String backupSupplierId) {
}
