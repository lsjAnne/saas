package com.dianshang.platform.dashboard;

import com.dianshang.platform.approval.application.ApprovalService;
import com.dianshang.platform.approval.domain.repository.ApprovalInstanceRepository;
import com.dianshang.platform.approval.model.ApprovalInstance;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.campaign.application.CampaignService;
import com.dianshang.platform.campaign.domain.repository.CampaignActivityRepository;
import com.dianshang.platform.campaign.domain.repository.CouponTemplateRepository;
import com.dianshang.platform.campaign.model.CampaignActivity;
import com.dianshang.platform.campaign.model.CouponTemplate;
import com.dianshang.platform.dashboard.application.DashboardService;
import com.dianshang.platform.exceptioncenter.application.ExceptionService;
import com.dianshang.platform.exceptioncenter.domain.repository.ExceptionTaskRepository;
import com.dianshang.platform.exceptioncenter.model.ExceptionTask;
import com.dianshang.platform.finance.application.FinanceService;
import com.dianshang.platform.fulfillment.domain.repository.FulfillmentTaskRepository;
import com.dianshang.platform.fulfillment.model.FulfillmentTask;
import com.dianshang.platform.inventory.application.InventoryService;
import com.dianshang.platform.inventory.domain.repository.InventorySnapshotRepository;
import com.dianshang.platform.inventory.model.InventorySnapshot;
import com.dianshang.platform.member.application.MemberService;
import com.dianshang.platform.notification.application.NotificationService;
import com.dianshang.platform.notification.domain.repository.NotificationTaskRepository;
import com.dianshang.platform.notification.model.NotificationTask;
import com.dianshang.platform.order.application.OrderService;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.model.OrderMain;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.servicecase.application.ServiceCaseService;
import com.dianshang.platform.store.application.StoreChannelService;
import com.dianshang.platform.supplier.domain.repository.SupplierRepository;
import com.dianshang.platform.supplier.model.Supplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.integrations.external.bi.provider=superset",
        "app.integrations.external.bi.endpoint=https://superset.example.com/api/v1",
        "app.integrations.external.bi.dashboard-count=6",
        "app.integrations.external.bi.dataset-count=18",
        "app.integrations.external.bi.embed-enabled=true",
        "app.integrations.external.systems.bi.endpoint=https://superset.example.com/api/v1",
        "app.integrations.external.systems.bi.credential-configured=true"
})
@AutoConfigureMockMvc
class MemberFinanceDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StoreChannelService storeChannelService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private ServiceCaseService serviceCaseService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Autowired
    private ExceptionTaskRepository exceptionTaskRepository;

    @Autowired
    private ApprovalInstanceRepository approvalInstanceRepository;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @Autowired
    private CampaignActivityRepository campaignActivityRepository;

    @Autowired
    private CouponTemplateRepository couponTemplateRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @BeforeEach
    void setUp() {
        dashboardService.clear();
        financeService.clear();
        memberService.clear();
        campaignService.clear();
        notificationService.clear();
        approvalService.clear();
        exceptionService.clear();
        supplierRepository.deleteAll();
        fulfillmentTaskRepository.deleteAll();
        inventoryService.clear();
        orderService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldManageMemberTagsAndExportGroups() throws Exception {
        TenantStoreFixture fixture = prepareTenantStore("member-center");
        seedOrders(fixture.storeId());

        MvcResult listResult = mockMvc.perform(withBearerToken(get("/api/members"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].levelCode").value("svip"))
                .andReturn();
        String memberId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data")
                .path("list")
                .path(0)
                .path("memberId")
                .asText();

        mockMvc.perform(withBearerToken(post("/api/members/{id}/tags", memberId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tagCode": "vip_reactivation",
                                  "tagName": "高价值召回",
                                  "sourceType": "manual"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberTagId").value(Matchers.startsWith("member-tag-")))
                .andExpect(jsonPath("$.data.tagCode").value("vip_reactivation"));

        mockMvc.perform(withBearerToken(get("/api/member-tags"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tagCode").value("vip_reactivation"))
                .andExpect(jsonPath("$.data[0].memberCount").value(1));

        MvcResult exportResult = mockMvc.perform(withBearerToken(post("/api/member-groups/export"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "tagCode": "vip_reactivation"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMembers").value(1))
                .andReturn();
        String memberTagId = mockMvc.perform(withBearerToken(get("/api/members/{id}", memberId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.memberId").value(memberId))
                .andExpect(jsonPath("$.data.member.tags.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String tagId = objectMapper.readTree(memberTagId)
                .path("data")
                .path("member")
                .path("tags")
                .path(0)
                .path("memberTagId")
                .asText();
        JsonNode exportData = objectMapper.readTree(exportResult.getResponse().getContentAsString()).path("data");
        org.assertj.core.api.Assertions.assertThat(exportData.path("memberIds").get(0).asText()).isEqualTo(memberId);

        mockMvc.perform(withBearerToken(delete("/api/members/{id}/tags/{tagId}", memberId, tagId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberTagId").value(tagId));

        mockMvc.perform(withBearerToken(get("/api/member-tags"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldBuildCrmProfilesSegmentsSlaAndLinkageViews() throws Exception {
        TenantStoreFixture fixture = prepareTenantStore("crm-center");
        OrderSeedResult orders = seedOrders(fixture.storeId());

        MvcResult memberListResult = mockMvc.perform(withBearerToken(get("/api/members"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn();
        JsonNode memberList = objectMapper.readTree(memberListResult.getResponse().getContentAsString())
                .path("data")
                .path("list");
        String highValueMemberId = memberList.path(0).path("memberId").asText();
        String highValueCustomerId = memberList.path(0).path("customerId").asText();

        mockMvc.perform(withBearerToken(post("/api/members/{id}/crm-profile", highValueMemberId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "赵二重点客户",
                                  "primaryContactName": "李经理",
                                  "primaryContactMobile": "13700000001",
                                  "wechatId": "crm-zhao-001",
                                  "sourceChannel": "douyin_live",
                                  "sourceDetail": "618直播专场",
                                  "customerTier": "strategic"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(highValueMemberId))
                .andExpect(jsonPath("$.data.primaryContactName").value("李经理"))
                .andExpect(jsonPath("$.data.sourceChannel").value("douyin_live"))
                .andExpect(jsonPath("$.data.customerTier").value("strategic"));

        MvcResult segmentRuleResult = mockMvc.perform(withBearerToken(post("/api/member-segment-rules"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "ruleName": "高价值沉默客召回",
                                  "lifecycleStage": "dormant",
                                  "minTotalPaidAmount": 5000,
                                  "tagCode": "winback_high_value",
                                  "tagName": "高价值召回"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleId").value(Matchers.startsWith("member-segment-rule-")))
                .andReturn();
        String ruleId = objectMapper.readTree(segmentRuleResult.getResponse().getContentAsString())
                .path("data")
                .path("ruleId")
                .asText();

        mockMvc.perform(withBearerToken(post("/api/member-segment-rules/{id}/execute", ruleId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleId").value(ruleId))
                .andExpect(jsonPath("$.data.matchedMemberCount").value(1))
                .andExpect(jsonPath("$.data.matchedMemberIds[0]").value(highValueMemberId));

        mockMvc.perform(withBearerToken(get("/api/member-tags"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tagCode").value("winback_high_value"))
                .andExpect(jsonPath("$.data[0].memberCount").value(1));

        String ticketId = serviceCaseService.createTicketSeed(
                        fixture.tenantId(),
                        fixture.storeId(),
                        orders.largeOrderId(),
                        highValueCustomerId,
                        true)
                .ticketId();

        mockMvc.perform(withBearerToken(post("/api/tickets/{id}/reply-suggestion", ticketId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketId").value(ticketId))
                .andExpect(jsonPath("$.data.ticketStatus").value("processing"));

        mockMvc.perform(withBearerToken(post("/api/tickets/{id}/satisfaction", ticketId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score": 5,
                                  "comment": "响应及时，问题已处理"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketId").value(ticketId))
                .andExpect(jsonPath("$.data.score").value(5));

        mockMvc.perform(withBearerToken(get("/api/tickets/sla-overview"), fixture.token())
                        .param("storeId", fixture.storeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTicketCount").value(1))
                .andExpect(jsonPath("$.data.withinSlaCount").value(1))
                .andExpect(jsonPath("$.data.satisfactionCount").value(1))
                .andExpect(jsonPath("$.data.averageSatisfactionScore").value(5.0));

        MvcResult afterSaleResult = mockMvc.perform(withBearerToken(post("/api/after-sales"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "afterSaleType": "refund",
                                  "reasonText": "高价值客户申请售后",
                                  "evidenceBlob": "{\\\"images\\\":[\\\"crm-proof-1.png\\\"]}"
                                }
                                """.formatted(orders.largeOrderId())))
                .andExpect(status().isOk())
                .andReturn();
        String afterSaleId = objectMapper.readTree(afterSaleResult.getResponse().getContentAsString())
                .path("data")
                .path("afterSaleId")
                .asText();

        mockMvc.perform(withBearerToken(post("/api/after-sales/{id}/submit-approval", afterSaleId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.afterSaleId").value(afterSaleId))
                .andExpect(jsonPath("$.data.status").value("reviewing"));

        CampaignActivity recallCampaign = campaignActivityRepository.save(new CampaignActivity(
                null,
                fixture.storeId(),
                "full_reduction",
                "沉默客召回计划",
                "published",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(7),
                List.of("crm-product-1"),
                Map.of("thresholdAmount", 199, "discountAmount", 30),
                null,
                true,
                OffsetDateTime.now()
        ));

        mockMvc.perform(withBearerToken(post("/api/member-touch-tasks"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "memberId": "%s",
                                  "taskType": "recall",
                                  "triggerType": "segment_rule",
                                  "campaignId": "%s",
                                  "channel": "site_message",
                                  "scheduledAt": "%s",
                                  "remark": "高价值沉默客自动召回"
                                }
                                """.formatted(
                                fixture.storeId(),
                                highValueMemberId,
                                recallCampaign.campaignId(),
                                OffsetDateTime.now().plusDays(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(highValueMemberId))
                .andExpect(jsonPath("$.data.taskType").value("recall"))
                .andExpect(jsonPath("$.data.status").value("planned"));

        mockMvc.perform(withBearerToken(get("/api/member-touch-tasks"), fixture.token())
                        .param("memberId", highValueMemberId)
                        .param("taskType", "recall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].campaignId").value(recallCampaign.campaignId()));

        mockMvc.perform(withBearerToken(get("/api/member-crm-analysis"), fixture.token())
                        .param("storeId", fixture.storeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMembers").value(2))
                .andExpect(jsonPath("$.data.highValueMembers").value(1))
                .andExpect(jsonPath("$.data.silentMembers").value(1))
                .andExpect(jsonPath("$.data.churnWarningMembers").value(1))
                .andExpect(jsonPath("$.data.strategicMembers").value(1))
                .andExpect(jsonPath("$.data.autoTaggedMembers").value(1))
                .andExpect(jsonPath("$.data.activeRecallTaskCount").value(1));

        mockMvc.perform(withBearerToken(get("/api/members/{id}/crm-linkage", highValueMemberId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(highValueMemberId))
                .andExpect(jsonPath("$.data.customerTier").value("strategic"))
                .andExpect(jsonPath("$.data.sourceChannel").value("douyin_live"))
                .andExpect(jsonPath("$.data.relatedOrderCount").value(1))
                .andExpect(jsonPath("$.data.relatedAfterSaleCount").value(1))
                .andExpect(jsonPath("$.data.relatedTicketCount").value(1))
                .andExpect(jsonPath("$.data.relatedCampaignCount").value(1))
                .andExpect(jsonPath("$.data.activeTouchTaskCount").value(1))
                .andExpect(jsonPath("$.data.churnWarningLevel").value("high"));
    }

    @Test
    void shouldGenerateReconcileAndSettleFinanceBill() throws Exception {
        TenantStoreFixture fixture = prepareTenantStore("finance-center");
        OrderSeedResult orders = seedOrders(fixture.storeId());

        LocalDate periodStart = OffsetDateTime.now().toLocalDate().minusDays(150);
        LocalDate periodEnd = OffsetDateTime.now().toLocalDate();

        mockMvc.perform(withBearerToken(get("/api/receivable-ledgers"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        MvcResult paymentResult = mockMvc.perform(withBearerToken(post("/api/customer-payments"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "paymentChannel": "bank_transfer",
                                  "paymentAmount": 200.00,
                                  "remark": "客户线下补款"
                                }
                                """.formatted(orders.primaryOrderId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentRecordId").value(Matchers.startsWith("customer-payment-")))
                .andExpect(jsonPath("$.data.paymentStatus").value("collected"))
                .andReturn();
        String paymentRecordId = objectMapper.readTree(paymentResult.getResponse().getContentAsString())
                .path("data")
                .path("paymentRecordId")
                .asText();

        mockMvc.perform(withBearerToken(get("/api/customer-payments"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].paymentRecordId").value(paymentRecordId))
                .andExpect(jsonPath("$.data[0].paymentAmount").value(200.0));

        MvcResult receivableResult = mockMvc.perform(withBearerToken(get("/api/receivable-ledgers"), fixture.token()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode receivableLedgers = objectMapper.readTree(receivableResult.getResponse().getContentAsString()).path("data");
        JsonNode primaryReceivable = findNodeByField(receivableLedgers, "orderId", orders.primaryOrderId());
        org.assertj.core.api.Assertions.assertThat(primaryReceivable).isNotNull();
        org.assertj.core.api.Assertions.assertThat(primaryReceivable.path("collectedAmount").decimalValue())
                .isEqualByComparingTo("200.00");
        org.assertj.core.api.Assertions.assertThat(primaryReceivable.path("outstandingAmount").decimalValue())
                .isEqualByComparingTo("0");
        org.assertj.core.api.Assertions.assertThat(primaryReceivable.path("receivableStatus").asText())
                .isEqualTo("collected");

        MvcResult generateResult = mockMvc.perform(withBearerToken(post("/api/finance-bills/generate"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "billType": "monthly",
                                  "periodStart": "%s",
                                  "periodEnd": "%s"
                                }
                                """.formatted(fixture.storeId(), periodStart, periodEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.financeBillId").value(Matchers.startsWith("finance-bill-")))
                .andExpect(jsonPath("$.data.billStatus").value("draft"))
                .andExpect(jsonPath("$.data.incomeAmount").value(6500))
                .andReturn();
        String financeBillId = objectMapper.readTree(generateResult.getResponse().getContentAsString())
                .path("data")
                .path("financeBillId")
                .asText();

        mockMvc.perform(withBearerToken(get("/api/finance-bills"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].financeBillId").value(financeBillId));

        mockMvc.perform(withBearerToken(post("/api/finance-bills/{id}/reconcile", financeBillId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.financeBillId").value(financeBillId))
                .andExpect(jsonPath("$.data.billStatus").value("reconciled"))
                .andExpect(jsonPath("$.data.discrepancyCount").value(0));

        mockMvc.perform(withBearerToken(post("/api/finance-bills/{id}/settle", financeBillId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settlementType": "bank_transfer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementRecordId").value(Matchers.startsWith("settlement-record-")))
                .andExpect(jsonPath("$.data.settlementStatus").value("settled"))
                .andReturn();

        MvcResult settlementResult = mockMvc.perform(withBearerToken(post("/api/finance-bills/{id}/settle", financeBillId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settlementType": "bank_transfer"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String settlementRecordId = objectMapper.readTree(settlementResult.getResponse().getContentAsString())
                .path("data")
                .path("settlementRecordId")
                .asText();

        mockMvc.perform(withBearerToken(post("/api/finance-vouchers/archive"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "finance_settlement",
                                  "referenceId": "%s",
                                  "voucherType": "settlement_voucher",
                                  "voucherAmount": 990.00,
                                  "remark": "账单结算归档"
                                }
                                """.formatted(fixture.storeId(), settlementRecordId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucherId").value(Matchers.startsWith("finance-voucher-")))
                .andExpect(jsonPath("$.data.archiveStatus").value("archived"));

        mockMvc.perform(withBearerToken(post("/api/finance-vouchers/archive"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "customer_payment",
                                  "referenceId": "%s",
                                  "voucherType": "receipt_voucher",
                                  "voucherAmount": 200.00,
                                  "remark": "客户回款归档"
                                }
                                """.formatted(fixture.storeId(), paymentRecordId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archiveStatus").value("archived"));

        mockMvc.perform(withBearerToken(get("/api/finance-vouchers"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].voucherType").value("receipt_voucher"))
                .andExpect(jsonPath("$.data[1].voucherType").value("settlement_voucher"));

        mockMvc.perform(withBearerToken(post("/api/finance-period-closings/close"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "periodStart": "%s",
                                  "periodEnd": "%s",
                                  "remark": "月度账期结转"
                                }
                                """.formatted(fixture.storeId(), periodStart, periodEnd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7423"))
                .andExpect(jsonPath("$.message").value("finance closing checks must pass before period closing"));

        mockMvc.perform(withBearerToken(get("/api/finance-period-closings"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(withBearerToken(get("/api/finance-bills/{id}", financeBillId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bill.financeBillId").value(financeBillId))
                .andExpect(jsonPath("$.data.bill.billStatus").value("settled"))
                .andExpect(jsonPath("$.data.settlements.length()").value(1))
                .andExpect(jsonPath("$.data.splitStatus").value("completed"))
                .andExpect(jsonPath("$.data.invoiceCheckStatus").value("matched"));

        MvcResult issuedInvoiceResult = mockMvc.perform(withBearerToken(post("/api/finance-invoices/issue"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "finance_bill",
                                  "referenceId": "%s",
                                  "invoiceTitle": "星辰电商有限公司",
                                  "invoiceTaxNo": "91310000TEST0001",
                                  "invoiceAmount": 990.00,
                                  "remark": "月度账单开票"
                                }
                                """.formatted(fixture.storeId(), financeBillId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceId").value(Matchers.startsWith("finance-invoice-")))
                .andExpect(jsonPath("$.data.invoiceStatus").value("issued"))
                .andExpect(jsonPath("$.data.redFlush").value(false))
                .andReturn();
        String issuedInvoiceId = objectMapper.readTree(issuedInvoiceResult.getResponse().getContentAsString())
                .path("data")
                .path("invoiceId")
                .asText();

        mockMvc.perform(withBearerToken(post("/api/finance-invoices/{id}/archive", issuedInvoiceId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "纸票归档"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceStatus").value("issued"))
                .andExpect(jsonPath("$.data.archiveStatus").value("archived"));

        MvcResult redFlushInvoiceResult = mockMvc.perform(withBearerToken(post("/api/finance-invoices/{id}/red-flush", issuedInvoiceId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "开票信息错误红冲"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceId").value(Matchers.startsWith("finance-invoice-")))
                .andExpect(jsonPath("$.data.sourceInvoiceId").value(issuedInvoiceId))
                .andExpect(jsonPath("$.data.invoiceStatus").value("red_flushed"))
                .andExpect(jsonPath("$.data.redFlush").value(true))
                .andExpect(jsonPath("$.data.invoiceAmount").value(-990.0))
                .andReturn();
        String redFlushInvoiceId = objectMapper.readTree(redFlushInvoiceResult.getResponse().getContentAsString())
                .path("data")
                .path("invoiceId")
                .asText();

        mockMvc.perform(withBearerToken(post("/api/finance-invoices/{id}/archive", redFlushInvoiceId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "红冲票归档"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceStatus").value("red_flushed"))
                .andExpect(jsonPath("$.data.archiveStatus").value("archived"));

        MvcResult voidInvoiceResult = mockMvc.perform(withBearerToken(post("/api/finance-invoices/issue"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "finance_bill",
                                  "referenceId": "%s",
                                  "invoiceTitle": "星辰电商有限公司",
                                  "invoiceTaxNo": "91310000TEST0001",
                                  "invoiceAmount": 200.00,
                                  "remark": "补开发票"
                                }
                                """.formatted(fixture.storeId(), financeBillId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceStatus").value("issued"))
                .andReturn();
        String voidInvoiceId = objectMapper.readTree(voidInvoiceResult.getResponse().getContentAsString())
                .path("data")
                .path("invoiceId")
                .asText();

        mockMvc.perform(withBearerToken(post("/api/finance-invoices/{id}/void", voidInvoiceId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "客户取消补开"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceStatus").value("voided"));

        MvcResult invoiceListResult = mockMvc.perform(withBearerToken(get("/api/finance-invoices"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andReturn();
        JsonNode invoices = objectMapper.readTree(invoiceListResult.getResponse().getContentAsString()).path("data");
        JsonNode voidedInvoice = findNodeByField(invoices, "invoiceId", voidInvoiceId);
        JsonNode archivedInvoice = findNodeByField(invoices, "invoiceId", issuedInvoiceId);
        JsonNode redFlushInvoice = findNodeByField(invoices, "invoiceId", redFlushInvoiceId);
        org.assertj.core.api.Assertions.assertThat(voidedInvoice).isNotNull();
        org.assertj.core.api.Assertions.assertThat(archivedInvoice).isNotNull();
        org.assertj.core.api.Assertions.assertThat(redFlushInvoice).isNotNull();
        org.assertj.core.api.Assertions.assertThat(voidedInvoice.path("invoiceStatus").asText()).isEqualTo("voided");
        org.assertj.core.api.Assertions.assertThat(archivedInvoice.path("archiveStatus").asText()).isEqualTo("archived");
        org.assertj.core.api.Assertions.assertThat(redFlushInvoice.path("invoiceStatus").asText()).isEqualTo("red_flushed");
        org.assertj.core.api.Assertions.assertThat(redFlushInvoice.path("archiveStatus").asText()).isEqualTo("archived");

        mockMvc.perform(withBearerToken(get("/api/finance-bills/{id}", financeBillId), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceCheckStatus").value("red_flushed"));
    }

    @Test
    void shouldBuildFinanceGeneralLedgerAndClosingChecks() throws Exception {
        TenantStoreFixture fixture = prepareTenantStore("finance-ledger-center");
        OrderSeedResult orders = seedOrders(fixture.storeId());

        LocalDate periodStart = OffsetDateTime.now().toLocalDate().minusDays(150);
        LocalDate periodEnd = OffsetDateTime.now().toLocalDate();

        String primaryPaymentRecordId = objectMapper.readTree(mockMvc.perform(withBearerToken(post("/api/customer-payments"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "paymentChannel": "bank_transfer",
                                  "paymentAmount": 200.00,
                                  "remark": "首单回款"
                                }
                                """.formatted(orders.primaryOrderId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("paymentRecordId").asText();

        String financeBillId = objectMapper.readTree(mockMvc.perform(withBearerToken(post("/api/finance-bills/generate"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "billType": "monthly",
                                  "periodStart": "%s",
                                  "periodEnd": "%s"
                                }
                                """.formatted(fixture.storeId(), periodStart, periodEnd)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("financeBillId").asText();

        mockMvc.perform(withBearerToken(post("/api/finance-bills/{id}/reconcile", financeBillId), fixture.token()))
                .andExpect(status().isOk());

        String settlementRecordId = objectMapper.readTree(mockMvc.perform(withBearerToken(post("/api/finance-bills/{id}/settle", financeBillId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settlementType": "bank_transfer"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("settlementRecordId").asText();

        mockMvc.perform(withBearerToken(post("/api/finance-vouchers/archive"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "finance_settlement",
                                  "referenceId": "%s",
                                  "voucherType": "settlement_voucher",
                                  "voucherAmount": 990.00,
                                  "remark": "账单结算凭证归档"
                                }
                                """.formatted(fixture.storeId(), settlementRecordId)))
                .andExpect(status().isOk());

        String invoiceId = objectMapper.readTree(mockMvc.perform(withBearerToken(post("/api/finance-invoices/issue"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "finance_bill",
                                  "referenceId": "%s",
                                  "invoiceTitle": "星辰电商有限公司",
                                  "invoiceTaxNo": "91310000TEST0001",
                                  "invoiceAmount": 990.00,
                                  "remark": "月度账单开票"
                                }
                                """.formatted(fixture.storeId(), financeBillId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("invoiceId").asText();

        mockMvc.perform(withBearerToken(post("/api/finance-invoices/{id}/archive", invoiceId), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "发票归档"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(withBearerToken(get("/api/finance-general-ledgers"), fixture.token())
                        .param("periodStart", periodStart.toString())
                        .param("periodEnd", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data[0].financeBillCount").value(1))
                .andExpect(jsonPath("$.data[0].settledFinanceBillCount").value(1))
                .andExpect(jsonPath("$.data[0].salesIncomeAmount").value(6500.0))
                .andExpect(jsonPath("$.data[0].receivableCollectedAmount").value(200.0))
                .andExpect(jsonPath("$.data[0].outstandingReceivableAmount").value(6300.0))
                .andExpect(jsonPath("$.data[0].settlementAmount").value(990.0))
                .andExpect(jsonPath("$.data[0].archivedVoucherAmount").value(990.0))
                .andExpect(jsonPath("$.data[0].issuedInvoiceAmount").value(990.0))
                .andExpect(jsonPath("$.data[0].pendingVoucherCount").value(1))
                .andExpect(jsonPath("$.data[0].pendingInvoiceCount").value(0))
                .andExpect(jsonPath("$.data[0].closingStatus").value("pending"));

        MvcResult blockingCheckResult = mockMvc.perform(withBearerToken(get("/api/finance-closing-checks"), fixture.token())
                        .param("storeId", fixture.storeId())
                        .param("periodStart", periodStart.toString())
                        .param("periodEnd", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readyToClose").value(false))
                .andExpect(jsonPath("$.data.blockingIssueCount").value(2))
                .andReturn();
        JsonNode blockingItems = objectMapper.readTree(blockingCheckResult.getResponse().getContentAsString())
                .path("data")
                .path("checkItems");
        org.assertj.core.api.Assertions.assertThat(findNodeByField(blockingItems, "checkCode", "receivable_collection")
                .path("checkStatus").asText()).isEqualTo("blocking");
        org.assertj.core.api.Assertions.assertThat(findNodeByField(blockingItems, "checkCode", "voucher_archiving")
                .path("checkStatus").asText()).isEqualTo("blocking");

        mockMvc.perform(withBearerToken(post("/api/finance-period-closings/close"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "periodStart": "%s",
                                  "periodEnd": "%s",
                                  "remark": "检查未通过时禁止月结"
                                }
                                """.formatted(fixture.storeId(), periodStart, periodEnd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7423"))
                .andExpect(jsonPath("$.message").value("finance closing checks must pass before period closing"));

        mockMvc.perform(withBearerToken(post("/api/finance-vouchers/archive"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "customer_payment",
                                  "referenceId": "%s",
                                  "voucherType": "receipt_voucher",
                                  "voucherAmount": 200.00,
                                  "remark": "首单回款凭证归档"
                                }
                                """.formatted(fixture.storeId(), primaryPaymentRecordId)))
                .andExpect(status().isOk());

        String secondPaymentRecordId = objectMapper.readTree(mockMvc.perform(withBearerToken(post("/api/customer-payments"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "paymentChannel": "online",
                                  "paymentAmount": 100.00,
                                  "remark": "第二单回款"
                                }
                                """.formatted(orders.secondaryOrderId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("paymentRecordId").asText();

        String thirdPaymentRecordId = objectMapper.readTree(mockMvc.perform(withBearerToken(post("/api/customer-payments"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "paymentChannel": "online",
                                  "paymentAmount": 6200.00,
                                  "remark": "大单回款"
                                }
                                """.formatted(orders.largeOrderId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("paymentRecordId").asText();

        mockMvc.perform(withBearerToken(post("/api/finance-vouchers/archive"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "customer_payment",
                                  "referenceId": "%s",
                                  "voucherType": "receipt_voucher",
                                  "voucherAmount": 100.00,
                                  "remark": "第二单回款凭证归档"
                                }
                                """.formatted(fixture.storeId(), secondPaymentRecordId)))
                .andExpect(status().isOk());

        mockMvc.perform(withBearerToken(post("/api/finance-vouchers/archive"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "referenceType": "customer_payment",
                                  "referenceId": "%s",
                                  "voucherType": "receipt_voucher",
                                  "voucherAmount": 6200.00,
                                  "remark": "大单回款凭证归档"
                                }
                                """.formatted(fixture.storeId(), thirdPaymentRecordId)))
                .andExpect(status().isOk());

        mockMvc.perform(withBearerToken(get("/api/finance-general-ledgers"), fixture.token())
                        .param("periodStart", periodStart.toString())
                        .param("periodEnd", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].receivableCollectedAmount").value(6500.0))
                .andExpect(jsonPath("$.data[0].outstandingReceivableAmount").value(0.0))
                .andExpect(jsonPath("$.data[0].archivedVoucherAmount").value(7490.0))
                .andExpect(jsonPath("$.data[0].pendingVoucherCount").value(0))
                .andExpect(jsonPath("$.data[0].closingStatus").value("ready"));

        mockMvc.perform(withBearerToken(get("/api/finance-closing-checks"), fixture.token())
                        .param("storeId", fixture.storeId())
                        .param("periodStart", periodStart.toString())
                        .param("periodEnd", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readyToClose").value(true))
                .andExpect(jsonPath("$.data.blockingIssueCount").value(0))
                .andExpect(jsonPath("$.data.checkItems[0].checkStatus").value("passed"));

        mockMvc.perform(withBearerToken(post("/api/finance-period-closings/close"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "periodStart": "%s",
                                  "periodEnd": "%s",
                                  "remark": "ERP 月结自动化验收"
                                }
                                """.formatted(fixture.storeId(), periodStart, periodEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closingRecordId").value(Matchers.startsWith("finance-period-closing-")))
                .andExpect(jsonPath("$.data.closingStatus").value("closed"))
                .andExpect(jsonPath("$.data.linkedFinanceBillCount").value(1))
                .andExpect(jsonPath("$.data.archivedVoucherCount").value(4));

        mockMvc.perform(withBearerToken(get("/api/finance-period-closings"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].closingStatus").value("closed"))
                .andExpect(jsonPath("$.data[0].linkedFinanceBillCount").value(1))
                .andExpect(jsonPath("$.data[0].archivedVoucherCount").value(4));
    }

    @Test
    void shouldManageErpMasterDataDictionariesAndAccountMappings() throws Exception {
        TenantStoreFixture fixture = prepareTenantStore("erp-config-center");

        mockMvc.perform(withBearerToken(post("/api/erp-master-data-dictionaries"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictionaryType": "voucher_type",
                                  "dictionaryCode": "settlement_voucher",
                                  "dictionaryName": "结算凭证",
                                  "erpCode": "ERP_VOUCHER_SETTLEMENT",
                                  "erpName": "ERP结算凭证",
                                  "enabled": true,
                                  "remark": "首次配置"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dictionaryEntryId").value(Matchers.startsWith("erp-dictionary-")))
                .andExpect(jsonPath("$.data.dictionaryType").value("voucher_type"))
                .andExpect(jsonPath("$.data.dictionaryCode").value("settlement_voucher"))
                .andExpect(jsonPath("$.data.erpCode").value("ERP_VOUCHER_SETTLEMENT"));

        mockMvc.perform(withBearerToken(post("/api/erp-master-data-dictionaries"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictionaryType": "voucher_type",
                                  "dictionaryCode": "receipt_voucher",
                                  "dictionaryName": "回款凭证",
                                  "erpCode": "ERP_VOUCHER_RECEIPT",
                                  "erpName": "ERP回款凭证",
                                  "enabled": true,
                                  "remark": "回款映射"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(withBearerToken(get("/api/erp-master-data-dictionaries"), fixture.token())
                        .param("dictionaryType", "voucher_type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dictionaryCode").value("receipt_voucher"))
                .andExpect(jsonPath("$.data[1].dictionaryCode").value("settlement_voucher"));

        mockMvc.perform(withBearerToken(post("/api/erp-master-data-dictionaries"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictionaryType": "voucher_type",
                                  "dictionaryCode": "settlement_voucher",
                                  "dictionaryName": "结算凭证",
                                  "erpCode": "ERP_VOUCHER_SETTLEMENT",
                                  "erpName": "ERP结算凭证-更新",
                                  "enabled": true,
                                  "remark": "更新名称"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.erpName").value("ERP结算凭证-更新"));

        mockMvc.perform(withBearerToken(post("/api/erp-account-mappings"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "mappingCategory": "voucher_subject",
                                  "businessType": "voucher_type",
                                  "businessCode": "settlement_voucher",
                                  "subjectCode": "600101",
                                  "subjectName": "主营业务收入",
                                  "direction": "credit",
                                  "enabled": true,
                                  "remark": "结算科目"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mappingId").value(Matchers.startsWith("erp-account-mapping-")))
                .andExpect(jsonPath("$.data.subjectCode").value("600101"));

        mockMvc.perform(withBearerToken(post("/api/erp-account-mappings"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "mappingCategory": "voucher_subject",
                                  "businessType": "voucher_type",
                                  "businessCode": "receipt_voucher",
                                  "subjectCode": "112201",
                                  "subjectName": "应收账款",
                                  "direction": "debit",
                                  "enabled": true,
                                  "remark": "回款科目"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk());

        mockMvc.perform(withBearerToken(get("/api/erp-account-mappings"), fixture.token())
                        .param("mappingCategory", "voucher_subject")
                        .param("storeId", fixture.storeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].businessCode").value("receipt_voucher"))
                .andExpect(jsonPath("$.data[0].subjectCode").value("112201"))
                .andExpect(jsonPath("$.data[1].businessCode").value("settlement_voucher"))
                .andExpect(jsonPath("$.data[1].subjectCode").value("600101"));

        mockMvc.perform(withBearerToken(post("/api/erp-account-mappings"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "mappingCategory": "voucher_subject",
                                  "businessType": "voucher_type",
                                  "businessCode": "settlement_voucher",
                                  "subjectCode": "600199",
                                  "subjectName": "主营业务收入-调整",
                                  "direction": "credit",
                                  "enabled": true,
                                  "remark": "结算科目更新"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjectCode").value("600199"))
                .andExpect(jsonPath("$.data.subjectName").value("主营业务收入-调整"));
    }

    @Test
    void shouldBuildDashboardSummaryRiskCampaignAndMemberAnalysis() throws Exception {
        TenantStoreFixture fixture = prepareTenantStore("dashboard-center");
        OrderSeedResult orders = seedOrders(fixture.storeId());
        seedDashboardArtifacts(fixture.tenantId(), fixture.storeId(), orders.primaryOrderId());

        mockMvc.perform(withBearerToken(get("/api/dashboard/summary"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todaySalesAmount").value(300))
                .andExpect(jsonPath("$.data.todayOrderCount").value(2))
                .andExpect(jsonPath("$.data.grossProfit").value(90))
                .andExpect(jsonPath("$.data.exceptionCount").value(1))
                .andExpect(jsonPath("$.data.pendingConfirmCount").value(1))
                .andExpect(jsonPath("$.data.lowStockCount").value(1))
                .andExpect(jsonPath("$.data.topSuggestion").value("建议优先处理低库存与补货提醒"));

        mockMvc.perform(withBearerToken(get("/api/dashboard/trends"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[6].orderCount").value(2));

        mockMvc.perform(withBearerToken(get("/api/dashboard/risks"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[*].riskCode", Matchers.containsInAnyOrder(
                        "exception_backlog",
                        "low_stock",
                        "approval_pending",
                        "notification_dead_letter"
                )));

        mockMvc.perform(withBearerToken(get("/api/dashboard/campaign-analysis"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCampaignCount").value(2))
                .andExpect(jsonPath("$.data.publishedCampaignCount").value(1))
                .andExpect(jsonPath("$.data.pendingApprovalCount").value(1))
                .andExpect(jsonPath("$.data.totalCouponTemplateCount").value(1));

        mockMvc.perform(withBearerToken(get("/api/dashboard/member-analysis"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMembers").value(2))
                .andExpect(jsonPath("$.data.vipMembers").value(1))
                .andExpect(jsonPath("$.data.dormantMembers").value(1))
                .andExpect(jsonPath("$.data.repurchaseRate").value(0.5));
    }

    @Test
    void shouldBuildBiDomainMetricsLayersCockpitExportsAndDataQualityViews() throws Exception {
        TenantStoreFixture fixture = prepareTenantStore("bi-center");
        OrderSeedResult orders = seedOrders(fixture.storeId());
        seedDashboardArtifacts(fixture.tenantId(), fixture.storeId(), orders.primaryOrderId());
        seedBiArtifacts(fixture.tenantId(), fixture.storeId(), orders);

        mockMvc.perform(withBearerToken(post("/api/bi/metric-dictionaries"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metricCode": "avg_order_ticket",
                                  "metricName": "客单价",
                                  "metricCategory": "overview",
                                  "metricFormula": "salesIncomeAmount / salesOrderCount",
                                  "metricDescription": "最近30天销售额除以订单数",
                                  "ownerDomain": "order",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricCode").value("avg_order_ticket"))
                .andExpect(jsonPath("$.data.ownerDomain").value("order"));

        MvcResult domainsResult = mockMvc.perform(withBearerToken(get("/api/bi/theme-domains"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andReturn();
        JsonNode domains = objectMapper.readTree(domainsResult.getResponse().getContentAsString()).path("data");
        org.assertj.core.api.Assertions.assertThat(findNodeByField(domains, "domainCode", "order").path("objectCount").asInt()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(findNodeByField(domains, "domainCode", "supplier").path("objectCount").asInt()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(findNodeByField(domains, "domainCode", "service").path("objectCount").asInt()).isEqualTo(1);

        mockMvc.perform(withBearerToken(get("/api/bi/metric-dictionaries"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].metricCode", Matchers.hasItem("avg_order_ticket")))
                .andExpect(jsonPath("$.data[*].metricCode", Matchers.hasItem("sales_income_amount")));

        mockMvc.perform(withBearerToken(get("/api/bi/layers"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[*].layerCode", Matchers.contains("ods", "dwd", "dws", "ads")));

        mockMvc.perform(withBearerToken(get("/api/bi/overview"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesIncomeAmount").value(300))
                .andExpect(jsonPath("$.data.netProfitAmount").value(90))
                .andExpect(jsonPath("$.data.repurchaseRate").value(0.5))
                .andExpect(jsonPath("$.data.activeCampaignCount").value(1))
                .andExpect(jsonPath("$.data.activeMemberCount").value(1));

        mockMvc.perform(withBearerToken(get("/api/bi/operations-analysis"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingFulfillmentCount").value(1))
                .andExpect(jsonPath("$.data.overdueFulfillmentCount").value(0))
                .andExpect(jsonPath("$.data.supplierCount").value(2))
                .andExpect(jsonPath("$.data.highRiskSupplierCount").value(1))
                .andExpect(jsonPath("$.data.ticketCount").value(1))
                .andExpect(jsonPath("$.data.averageSatisfactionScore").value(4.0));

        mockMvc.perform(withBearerToken(get("/api/bi/marketing-analysis"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCampaignCount").value(2))
                .andExpect(jsonPath("$.data.publishedCampaignCount").value(1))
                .andExpect(jsonPath("$.data.marketingRoiRate").value(4.5))
                .andExpect(jsonPath("$.data.campaignConversionRate").value(0.5))
                .andExpect(jsonPath("$.data.couponBindCampaignCount").value(2));

        mockMvc.perform(withBearerToken(post("/api/bi/export-tasks"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportCode": "bi_cockpit_daily",
                                  "storeId": "%s",
                                  "exportFormat": "xlsx",
                                  "remark": "导出日报驾驶舱"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1015"))
                .andExpect(jsonPath("$.message").value("sensitive operation confirmation required"));

        confirmSensitivePermission(fixture.token(), AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE);

        mockMvc.perform(withBearerToken(post("/api/bi/export-tasks"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportCode": "bi_cockpit_daily",
                                  "storeId": "%s",
                                  "exportFormat": "xlsx",
                                  "remark": "导出日报驾驶舱"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportCode").value("bi_cockpit_daily"))
                .andExpect(jsonPath("$.data.exportStatus").value("pending"));

        mockMvc.perform(withBearerToken(get("/api/bi/export-tasks"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].exportFormat").value("xlsx"));

        mockMvc.perform(withBearerToken(get("/api/bi/cockpit"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alertCount").value(4))
                .andExpect(jsonPath("$.data.recommendedReports.length()").value(3));

        mockMvc.perform(withBearerToken(post("/api/bi/subscriptions"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subscriptionType": "daily_report",
                                  "reportCode": "bi_cockpit_daily",
                                  "scheduleType": "daily",
                                  "channel": "site_message",
                                  "recipient": "tenant-admin",
                                  "enabled": true,
                                  "remark": "每日推送驾驶舱"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionType").value("daily_report"))
                .andExpect(jsonPath("$.data.channel").value("site_message"));

        mockMvc.perform(withBearerToken(get("/api/bi/subscriptions"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reportCode").value("bi_cockpit_daily"));

        mockMvc.perform(withBearerToken(get("/api/bi/data-quality-checks"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[*].checkCode", Matchers.hasItems(
                        "inventory_below_safety",
                        "pending_approval",
                        "notification_dead_letter",
                        "dormant_member"
                )));

        mockMvc.perform(withBearerToken(post("/api/bi/data-repair-tasks"), fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checkCode": "notification_dead_letter",
                                  "repairStrategy": "reschedule_delivery",
                                  "remark": "重新编排死信通知"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkCode").value("notification_dead_letter"))
                .andExpect(jsonPath("$.data.affectedCount").value(1))
                .andExpect(jsonPath("$.data.repairStatus").value("completed"));

        mockMvc.perform(withBearerToken(get("/api/bi/data-quality-checks"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.checkCode=='notification_dead_letter')].affectedCount").value(Matchers.contains(0)));

        mockMvc.perform(withBearerToken(get("/api/bi/external-platform-overview"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("superset"))
                .andExpect(jsonPath("$.data.overviewStatus").value("ready"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("superset.example.com"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value("https://superset.example.com/***"))
                .andExpect(jsonPath("$.data.dashboardCount").value(6))
                .andExpect(jsonPath("$.data.datasetCount").value(18))
                .andExpect(jsonPath("$.data.embedEnabled").value(true))
                .andExpect(jsonPath("$.data.linkedThemeDomains.length()").value(6))
                .andExpect(jsonPath("$.data.linkedThemeDomains", Matchers.hasItems(
                        "order",
                        "finance",
                        "member"
                )));

        mockMvc.perform(withBearerToken(get("/api/bi/delivery-checklist"), fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallStatus").value("ready_for_integration"))
                .andExpect(jsonPath("$.data.checkItems.length()").value(7))
                .andExpect(jsonPath("$.data.checkItems[?(@.itemCode=='external_platform_ready')].itemStatus").value(Matchers.contains("completed")));
    }

    private TenantStoreFixture prepareTenantStore(String tenantName) throws Exception {
        String ownerMobile = "13800000000";
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-a",
                                  "mobile": "13800000000"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tenantData = objectMapper.readTree(tenantResult.getResponse().getContentAsString()).path("data");
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String token = login(ownerMobile);

        MvcResult storeResult = mockMvc.perform(withBearerToken(post("/api/stores/connect"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "%s",
                                  "platformType": "douyin",
                                  "platformShopId": "%s-shop",
                                  "shopName": "dashboard-shop",
                                  "profitThreshold": 18.50,
                                  "riskThreshold": 72.00
                                }
                                """.formatted(organizationId, ownerMobile, tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        String storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();
        return new TenantStoreFixture(tenantId, organizationId, storeId, ownerMobile, token);
    }

    private OrderSeedResult seedOrders(String storeId) {
        OffsetDateTime now = OffsetDateTime.now()
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        OrderMain primary = orderRepository.save(new OrderMain(
                null,
                storeId,
                "platform-order-1001",
                "paid",
                "pending",
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(60),
                "王一",
                "138****1001",
                "上海市浦东新区测试路 1 号",
                now.plusDays(1),
                now.minusHours(2)
        ));
        OrderMain secondary = orderRepository.save(new OrderMain(
                null,
                storeId,
                "platform-order-1002",
                "paid",
                "pending",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(30),
                "王一",
                "138****1001",
                "上海市浦东新区测试路 1 号",
                now.plusDays(1),
                now.minusHours(1)
        ));
        OrderMain large = orderRepository.save(new OrderMain(
                null,
                storeId,
                "platform-order-1003",
                "paid",
                "pending",
                BigDecimal.valueOf(6200),
                BigDecimal.valueOf(900),
                "赵二",
                "139****2002",
                "杭州市滨江区测试路 2 号",
                now.minusDays(119),
                now.minusDays(120)
        ));
        return new OrderSeedResult(primary.orderId(), secondary.orderId(), large.orderId());
    }

    private void seedDashboardArtifacts(String tenantId, String storeId, String orderId) {
        OffsetDateTime now = OffsetDateTime.now();
        inventorySnapshotRepository.save(new InventorySnapshot(
                null,
                storeId,
                "product-9001",
                "sku-9001",
                5,
                0,
                10,
                now,
                now
        ));
        exceptionTaskRepository.save(new ExceptionTask(
                null,
                storeId,
                "order",
                orderId,
                "delivery_timeout",
                "high",
                "open",
                "请人工处理配送超时异常",
                "tenant-admin",
                now
        ));
        fulfillmentTaskRepository.save(new FulfillmentTask(
                null,
                storeId,
                orderId,
                "fulfillment-" + orderId,
                "pending_confirm",
                0,
                now.plusHours(2),
                null,
                now
        ));
        CouponTemplate couponTemplate = couponTemplateRepository.save(new CouponTemplate(
                null,
                storeId,
                "新客立减",
                "amount",
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(99),
                "enabled",
                now
        ));
        CampaignActivity publishedCampaign = campaignActivityRepository.save(new CampaignActivity(
                null,
                storeId,
                "full_reduction",
                "高转化老客场",
                "published",
                now.minusDays(1),
                now.plusDays(3),
                List.of("product-9001"),
                Map.of("thresholdAmount", 99, "discountAmount", 20),
                couponTemplate.couponTemplateId(),
                true,
                now
        ));
        CampaignActivity pendingCampaign = campaignActivityRepository.save(new CampaignActivity(
                null,
                storeId,
                "new_customer_coupon",
                "新客拉新场",
                "pending_approval",
                now,
                now.plusDays(5),
                List.of("product-9001"),
                Map.of("limitPerUser", 1),
                couponTemplate.couponTemplateId(),
                true,
                now
        ));
        approvalInstanceRepository.save(new ApprovalInstance(
                null,
                tenantId,
                "campaign_publish",
                "campaign_activity",
                pendingCampaign.campaignId(),
                "pending",
                "tenant-admin",
                "待审核",
                null,
                now
        ));
        notificationTaskRepository.save(new NotificationTask(
                null,
                tenantId,
                "site_message",
                "approval_pending",
                "tenant-admin",
                "dead_letter",
                3,
                "{\"forceFail\":true}",
                "high",
                now.minusMinutes(5),
                null,
                "retry limit exceeded",
                now
        ));
        org.assertj.core.api.Assertions.assertThat(publishedCampaign.campaignId()).startsWith("campaign-");
    }

    private void seedBiArtifacts(String tenantId, String storeId, OrderSeedResult orders) {
        OffsetDateTime now = OffsetDateTime.now();
        supplierRepository.save(new Supplier(
                null,
                storeId,
                "1688",
                "supplier-platform-001",
                "稳定供应商A",
                "https://supplier.example.com/a",
                88,
                92,
                90,
                "low",
                true,
                true,
                false,
                false,
                now.minusDays(2)
        ));
        supplierRepository.save(new Supplier(
                null,
                storeId,
                "1688",
                "supplier-platform-002",
                "高风险供应商B",
                "https://supplier.example.com/b",
                60,
                58,
                55,
                "high",
                false,
                false,
                true,
                true,
                now.minusDays(1)
        ));
        String ticketId = serviceCaseService.createTicketSeed(
                        tenantId,
                        storeId,
                        orders.primaryOrderId(),
                        "customer-bi-001",
                        true)
                .ticketId();
        serviceCaseService.generateReplySuggestion(tenantId, ticketId);
        serviceCaseService.saveTicketSatisfaction(
                tenantId,
                ticketId,
                new ServiceCaseService.SaveTicketSatisfactionRequest(4, "处理及时")
        );
    }

    private String login(String mobile) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456"
                                }
                                """.formatted(mobile)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }

    private void confirmSensitivePermission(String token, String permissionCode) throws Exception {
        mockMvc.perform(withBearerToken(post("/api/auth/sensitive-operation-confirmations"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "123456",
                                  "permissionCode": "%s"
                                }
                                """.formatted(permissionCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(permissionCode))
                .andExpect(jsonPath("$.data.confirmed").value(true));
    }

    private MockHttpServletRequestBuilder withBearerToken(MockHttpServletRequestBuilder requestBuilder, String token) {
        return requestBuilder
                .header("Authorization", "Bearer " + token);
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

record TenantStoreFixture(String tenantId, String organizationId, String storeId, String ownerMobile, String token) {
}

record OrderSeedResult(String primaryOrderId, String secondaryOrderId, String largeOrderId) {
}
