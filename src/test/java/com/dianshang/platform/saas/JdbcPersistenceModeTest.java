package com.dianshang.platform.saas;

import com.dianshang.platform.auth.domain.repository.AuthRolePermissionRepository;
import com.dianshang.platform.auth.domain.repository.AuthUserRepository;
import com.dianshang.platform.auth.infrastructure.persistence.JdbcAuthRolePermissionRepository;
import com.dianshang.platform.auth.infrastructure.persistence.JdbcAuthUserRepository;
import com.dianshang.platform.audit.domain.repository.AuditLogRepository;
import com.dianshang.platform.audit.infrastructure.persistence.JdbcAuditLogRepository;
import com.dianshang.platform.organization.domain.repository.OrganizationMemberRepository;
import com.dianshang.platform.organization.domain.repository.OrganizationRepository;
import com.dianshang.platform.organization.infrastructure.persistence.JdbcOrganizationMemberRepository;
import com.dianshang.platform.organization.infrastructure.persistence.JdbcOrganizationRepository;
import com.dianshang.platform.saas.domain.repository.BillingOrderRepository;
import com.dianshang.platform.saas.domain.repository.InvoiceRequestRepository;
import com.dianshang.platform.saas.domain.repository.TenantProfileRepository;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcTenantProfileRepository;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcBillingOrderRepository;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcInvoiceRequestRepository;
import com.dianshang.platform.support.domain.repository.SupportSessionRepository;
import com.dianshang.platform.support.infrastructure.persistence.JdbcSupportSessionRepository;
import com.dianshang.platform.store.domain.repository.ChannelAccountRepository;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.infrastructure.persistence.JdbcChannelAccountRepository;
import com.dianshang.platform.store.infrastructure.persistence.JdbcStoreRepository;
import com.dianshang.platform.product.domain.repository.CandidateProductRepository;
import com.dianshang.platform.product.domain.repository.ProductDraftRepository;
import com.dianshang.platform.product.domain.repository.ProductRepository;
import com.dianshang.platform.product.domain.repository.ProductSourceMappingRepository;
import com.dianshang.platform.product.infrastructure.persistence.JdbcCandidateProductRepository;
import com.dianshang.platform.product.infrastructure.persistence.JdbcProductDraftRepository;
import com.dianshang.platform.product.infrastructure.persistence.JdbcProductRepository;
import com.dianshang.platform.product.infrastructure.persistence.JdbcProductSourceMappingRepository;
import com.dianshang.platform.supplier.domain.repository.SupplierRepository;
import com.dianshang.platform.supplier.infrastructure.persistence.JdbcSupplierRepository;
import com.dianshang.platform.inventory.domain.repository.InventorySnapshotRepository;
import com.dianshang.platform.inventory.domain.repository.ReplenishmentTaskRepository;
import com.dianshang.platform.inventory.infrastructure.persistence.JdbcInventorySnapshotRepository;
import com.dianshang.platform.inventory.infrastructure.persistence.JdbcReplenishmentTaskRepository;
import com.dianshang.platform.order.domain.repository.OrderItemRepository;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.infrastructure.persistence.JdbcOrderItemRepository;
import com.dianshang.platform.order.infrastructure.persistence.JdbcOrderRepository;
import com.dianshang.platform.fulfillment.domain.repository.FulfillmentTaskRepository;
import com.dianshang.platform.fulfillment.infrastructure.persistence.JdbcFulfillmentTaskRepository;
import com.dianshang.platform.fulfillment.domain.repository.LogisticsRecordRepository;
import com.dianshang.platform.fulfillment.infrastructure.persistence.JdbcLogisticsRecordRepository;
import com.dianshang.platform.exceptioncenter.domain.repository.ExceptionTaskRepository;
import com.dianshang.platform.exceptioncenter.infrastructure.persistence.JdbcExceptionTaskRepository;
import com.dianshang.platform.servicecase.domain.repository.AfterSaleRecordRepository;
import com.dianshang.platform.servicecase.domain.repository.CustomerServiceTicketRepository;
import com.dianshang.platform.servicecase.infrastructure.persistence.JdbcAfterSaleRecordRepository;
import com.dianshang.platform.servicecase.infrastructure.persistence.JdbcCustomerServiceTicketRepository;
import com.dianshang.platform.qa.domain.repository.CustomerConversationRepository;
import com.dianshang.platform.qa.domain.repository.ConversationMessageRepository;
import com.dianshang.platform.qa.domain.repository.FaqKnowledgeRepository;
import com.dianshang.platform.qa.infrastructure.persistence.JdbcConversationMessageRepository;
import com.dianshang.platform.qa.infrastructure.persistence.JdbcCustomerConversationRepository;
import com.dianshang.platform.qa.infrastructure.persistence.JdbcFaqKnowledgeRepository;
import com.dianshang.platform.live.domain.repository.LivePlanRepository;
import com.dianshang.platform.live.domain.repository.LiveProductItemRepository;
import com.dianshang.platform.live.domain.repository.LiveScriptRepository;
import com.dianshang.platform.live.domain.repository.LiveSessionRepository;
import com.dianshang.platform.live.infrastructure.persistence.JdbcLivePlanRepository;
import com.dianshang.platform.live.infrastructure.persistence.JdbcLiveProductItemRepository;
import com.dianshang.platform.live.infrastructure.persistence.JdbcLiveScriptRepository;
import com.dianshang.platform.live.infrastructure.persistence.JdbcLiveSessionRepository;
import com.dianshang.platform.notification.domain.repository.NotificationTaskRepository;
import com.dianshang.platform.notification.domain.repository.NotificationTemplateRepository;
import com.dianshang.platform.notification.infrastructure.persistence.JdbcNotificationTaskRepository;
import com.dianshang.platform.notification.infrastructure.persistence.JdbcNotificationTemplateRepository;
import com.dianshang.platform.campaign.domain.repository.CampaignActivityRepository;
import com.dianshang.platform.campaign.domain.repository.CouponTemplateRepository;
import com.dianshang.platform.campaign.infrastructure.persistence.JdbcCampaignActivityRepository;
import com.dianshang.platform.campaign.infrastructure.persistence.JdbcCouponTemplateRepository;
import com.dianshang.platform.finance.domain.repository.FinanceBillRepository;
import com.dianshang.platform.finance.domain.repository.SettlementRecordRepository;
import com.dianshang.platform.finance.infrastructure.persistence.JdbcFinanceBillRepository;
import com.dianshang.platform.finance.infrastructure.persistence.JdbcSettlementRecordRepository;
import com.dianshang.platform.member.domain.repository.MemberProfileRepository;
import com.dianshang.platform.member.domain.repository.MemberTagRepository;
import com.dianshang.platform.member.infrastructure.persistence.JdbcMemberProfileRepository;
import com.dianshang.platform.member.infrastructure.persistence.JdbcMemberTagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcPersistenceModeTest {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private AuthRolePermissionRepository authRolePermissionRepository;

    @Autowired
    private TenantProfileRepository tenantProfileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private SupportSessionRepository supportSessionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BillingOrderRepository billingOrderRepository;

    @Autowired
    private InvoiceRequestRepository invoiceRequestRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ChannelAccountRepository channelAccountRepository;

    @Autowired
    private CandidateProductRepository candidateProductRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductDraftRepository productDraftRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSourceMappingRepository productSourceMappingRepository;

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Autowired
    private ReplenishmentTaskRepository replenishmentTaskRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private LogisticsRecordRepository logisticsRecordRepository;

    @Autowired
    private ExceptionTaskRepository exceptionTaskRepository;

    @Autowired
    private CustomerServiceTicketRepository customerServiceTicketRepository;

    @Autowired
    private AfterSaleRecordRepository afterSaleRecordRepository;

    @Autowired
    private CustomerConversationRepository customerConversationRepository;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @Autowired
    private FaqKnowledgeRepository faqKnowledgeRepository;

    @Autowired
    private LivePlanRepository livePlanRepository;

    @Autowired
    private LiveProductItemRepository liveProductItemRepository;

    @Autowired
    private LiveScriptRepository liveScriptRepository;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private CampaignActivityRepository campaignActivityRepository;

    @Autowired
    private CouponTemplateRepository couponTemplateRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private MemberTagRepository memberTagRepository;

    @Autowired
    private FinanceBillRepository financeBillRepository;

    @Autowired
    private SettlementRecordRepository settlementRecordRepository;

    @Test
    void shouldUseJdbcRepositoriesByDefault() {
        assertThat(authUserRepository).isInstanceOf(JdbcAuthUserRepository.class);
        assertThat(authRolePermissionRepository).isInstanceOf(JdbcAuthRolePermissionRepository.class);
        assertThat(tenantProfileRepository).isInstanceOf(JdbcTenantProfileRepository.class);
        assertThat(organizationRepository).isInstanceOf(JdbcOrganizationRepository.class);
        assertThat(organizationMemberRepository).isInstanceOf(JdbcOrganizationMemberRepository.class);
        assertThat(supportSessionRepository).isInstanceOf(JdbcSupportSessionRepository.class);
        assertThat(auditLogRepository).isInstanceOf(JdbcAuditLogRepository.class);
        assertThat(billingOrderRepository).isInstanceOf(JdbcBillingOrderRepository.class);
        assertThat(invoiceRequestRepository).isInstanceOf(JdbcInvoiceRequestRepository.class);
        assertThat(storeRepository).isInstanceOf(JdbcStoreRepository.class);
        assertThat(channelAccountRepository).isInstanceOf(JdbcChannelAccountRepository.class);
        assertThat(candidateProductRepository).isInstanceOf(JdbcCandidateProductRepository.class);
        assertThat(supplierRepository).isInstanceOf(JdbcSupplierRepository.class);
        assertThat(productDraftRepository).isInstanceOf(JdbcProductDraftRepository.class);
        assertThat(productRepository).isInstanceOf(JdbcProductRepository.class);
        assertThat(productSourceMappingRepository).isInstanceOf(JdbcProductSourceMappingRepository.class);
        assertThat(inventorySnapshotRepository).isInstanceOf(JdbcInventorySnapshotRepository.class);
        assertThat(replenishmentTaskRepository).isInstanceOf(JdbcReplenishmentTaskRepository.class);
        assertThat(orderRepository).isInstanceOf(JdbcOrderRepository.class);
        assertThat(orderItemRepository).isInstanceOf(JdbcOrderItemRepository.class);
        assertThat(fulfillmentTaskRepository).isInstanceOf(JdbcFulfillmentTaskRepository.class);
        assertThat(logisticsRecordRepository).isInstanceOf(JdbcLogisticsRecordRepository.class);
        assertThat(exceptionTaskRepository).isInstanceOf(JdbcExceptionTaskRepository.class);
        assertThat(customerServiceTicketRepository).isInstanceOf(JdbcCustomerServiceTicketRepository.class);
        assertThat(afterSaleRecordRepository).isInstanceOf(JdbcAfterSaleRecordRepository.class);
        assertThat(customerConversationRepository).isInstanceOf(JdbcCustomerConversationRepository.class);
        assertThat(conversationMessageRepository).isInstanceOf(JdbcConversationMessageRepository.class);
        assertThat(faqKnowledgeRepository).isInstanceOf(JdbcFaqKnowledgeRepository.class);
        assertThat(livePlanRepository).isInstanceOf(JdbcLivePlanRepository.class);
        assertThat(liveProductItemRepository).isInstanceOf(JdbcLiveProductItemRepository.class);
        assertThat(liveScriptRepository).isInstanceOf(JdbcLiveScriptRepository.class);
        assertThat(liveSessionRepository).isInstanceOf(JdbcLiveSessionRepository.class);
        assertThat(notificationTaskRepository).isInstanceOf(JdbcNotificationTaskRepository.class);
        assertThat(notificationTemplateRepository).isInstanceOf(JdbcNotificationTemplateRepository.class);
        assertThat(campaignActivityRepository).isInstanceOf(JdbcCampaignActivityRepository.class);
        assertThat(couponTemplateRepository).isInstanceOf(JdbcCouponTemplateRepository.class);
        assertThat(memberProfileRepository).isInstanceOf(JdbcMemberProfileRepository.class);
        assertThat(memberTagRepository).isInstanceOf(JdbcMemberTagRepository.class);
        assertThat(financeBillRepository).isInstanceOf(JdbcFinanceBillRepository.class);
        assertThat(settlementRecordRepository).isInstanceOf(JdbcSettlementRecordRepository.class);
    }
}
