package backend.saas.infrastructure.persistence;

import backend.auth.domain.repository.AuthRolePermissionRepository;
import backend.auth.domain.repository.AuthUserRepository;
import backend.auth.infrastructure.persistence.JdbcAuthRolePermissionRepository;
import backend.auth.infrastructure.persistence.JdbcAuthUserRepository;
import backend.audit.domain.repository.AuditLogRepository;
import backend.audit.infrastructure.persistence.JdbcAuditLogRepository;
import backend.organization.domain.repository.OrganizationMemberRepository;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.infrastructure.persistence.JdbcOrganizationMemberRepository;
import backend.organization.infrastructure.persistence.JdbcOrganizationRepository;
import backend.saas.domain.repository.BillingOrderRepository;
import backend.saas.domain.repository.InvoiceRequestRepository;
import backend.saas.domain.repository.TenantProfileRepository;
import backend.saas.infrastructure.persistence.JdbcTenantProfileRepository;
import backend.saas.infrastructure.persistence.JdbcBillingOrderRepository;
import backend.saas.infrastructure.persistence.JdbcInvoiceRequestRepository;
import backend.support.domain.repository.SupportSessionRepository;
import backend.support.infrastructure.persistence.JdbcSupportSessionRepository;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.domain.repository.StoreRepository;
import backend.store.infrastructure.persistence.JdbcChannelAccountRepository;
import backend.store.infrastructure.persistence.JdbcStoreRepository;
import backend.product.domain.repository.CandidateProductRepository;
import backend.product.domain.repository.ProductDraftRepository;
import backend.product.domain.repository.ProductRepository;
import backend.product.domain.repository.ProductSourceMappingRepository;
import backend.product.infrastructure.persistence.JdbcCandidateProductRepository;
import backend.product.infrastructure.persistence.JdbcProductDraftRepository;
import backend.product.infrastructure.persistence.JdbcProductRepository;
import backend.product.infrastructure.persistence.JdbcProductSourceMappingRepository;
import backend.supplier.domain.repository.SupplierRepository;
import backend.supplier.infrastructure.persistence.JdbcSupplierRepository;
import backend.inventory.domain.repository.InventorySnapshotRepository;
import backend.inventory.domain.repository.ReplenishmentTaskRepository;
import backend.inventory.infrastructure.persistence.JdbcInventorySnapshotRepository;
import backend.inventory.infrastructure.persistence.JdbcReplenishmentTaskRepository;
import backend.order.domain.repository.OrderItemRepository;
import backend.order.domain.repository.OrderRepository;
import backend.order.infrastructure.persistence.JdbcOrderItemRepository;
import backend.order.infrastructure.persistence.JdbcOrderRepository;
import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.fulfillment.infrastructure.persistence.JdbcFulfillmentTaskRepository;
import backend.fulfillment.domain.repository.LogisticsRecordRepository;
import backend.fulfillment.infrastructure.persistence.JdbcLogisticsRecordRepository;
import backend.exceptioncenter.domain.repository.ExceptionTaskRepository;
import backend.exceptioncenter.infrastructure.persistence.JdbcExceptionTaskRepository;
import backend.servicecase.domain.repository.AfterSaleRecordRepository;
import backend.servicecase.domain.repository.CustomerServiceTicketRepository;
import backend.servicecase.infrastructure.persistence.JdbcAfterSaleRecordRepository;
import backend.servicecase.infrastructure.persistence.JdbcCustomerServiceTicketRepository;
import backend.qa.domain.repository.CustomerConversationRepository;
import backend.qa.domain.repository.ConversationMessageRepository;
import backend.qa.domain.repository.FaqKnowledgeRepository;
import backend.qa.infrastructure.persistence.JdbcConversationMessageRepository;
import backend.qa.infrastructure.persistence.JdbcCustomerConversationRepository;
import backend.qa.infrastructure.persistence.JdbcFaqKnowledgeRepository;
import backend.live.domain.repository.LivePlanRepository;
import backend.live.domain.repository.LiveProductItemRepository;
import backend.live.domain.repository.LiveScriptRepository;
import backend.live.domain.repository.LiveSessionRepository;
import backend.live.infrastructure.persistence.JdbcLivePlanRepository;
import backend.live.infrastructure.persistence.JdbcLiveProductItemRepository;
import backend.live.infrastructure.persistence.JdbcLiveScriptRepository;
import backend.live.infrastructure.persistence.JdbcLiveSessionRepository;
import backend.notification.domain.repository.NotificationTaskRepository;
import backend.notification.domain.repository.NotificationTemplateRepository;
import backend.notification.infrastructure.persistence.JdbcNotificationTaskRepository;
import backend.notification.infrastructure.persistence.JdbcNotificationTemplateRepository;
import backend.campaign.domain.repository.CampaignActivityRepository;
import backend.campaign.domain.repository.CouponTemplateRepository;
import backend.campaign.infrastructure.persistence.JdbcCampaignActivityRepository;
import backend.campaign.infrastructure.persistence.JdbcCouponTemplateRepository;
import backend.finance.domain.repository.FinanceBillRepository;
import backend.finance.domain.repository.SettlementRecordRepository;
import backend.finance.infrastructure.persistence.JdbcFinanceBillRepository;
import backend.finance.infrastructure.persistence.JdbcSettlementRecordRepository;
import backend.member.domain.repository.MemberProfileRepository;
import backend.member.domain.repository.MemberTagRepository;
import backend.member.infrastructure.persistence.JdbcMemberProfileRepository;
import backend.member.infrastructure.persistence.JdbcMemberTagRepository;
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

