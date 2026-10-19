package backend.channel.application;

import backend.audit.application.AuditLogService;
import backend.channel.domain.repository.ChannelTokenRepository;
import backend.channel.dto.ChannelSyncResult;
import backend.channel.infrastructure.persistence.InMemoryChannelTokenRepository;
import backend.channel.model.ChannelAfterSale;
import backend.channel.model.ChannelAuthContext;
import backend.channel.model.ChannelCallbackRequest;
import backend.channel.model.ChannelConnectivity;
import backend.channel.model.ChannelOrder;
import backend.channel.model.ChannelProduct;
import backend.channel.model.ChannelShipRequest;
import backend.channel.model.ChannelShipResult;
import backend.channel.model.ChannelToken;
import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import backend.order.application.OrderApplicationService;
import backend.order.dto.OrderSyncResult;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.ChannelAccount;
import backend.store.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelSyncServiceTest {

    private ChannelAccountRepository channelAccountRepository;
    private OrganizationRepository organizationRepository;
    private StoreRepository storeRepository;
    private OrderApplicationService orderApplicationService;
    private ChannelClientRegistry channelClientRegistry;
    private AuditLogService auditLogService;
    private InMemoryChannelTokenRepository tokenRepository;
    private ChannelSyncService service;

    @BeforeEach
    void setUp() {
        channelAccountRepository = mock(ChannelAccountRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        storeRepository = mock(StoreRepository.class);
        orderApplicationService = mock(OrderApplicationService.class);
        channelClientRegistry = mock(ChannelClientRegistry.class);
        auditLogService = mock(AuditLogService.class);
        tokenRepository = new InMemoryChannelTokenRepository();
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        service = new ChannelSyncService(
                channelAccountRepository,
                organizationRepository,
                storeRepository,
                tokenRepository,
                channelClientRegistry,
                properties,
                orderApplicationService,
                auditLogService
        );

        OffsetDateTime now = OffsetDateTime.now();
        when(organizationRepository.findById("org-1"))
                .thenReturn(Optional.of(new Organization("org-1", "tenant-1", "org", "active", now)));
        when(channelAccountRepository.findByChannelAccountId("ch-1"))
                .thenReturn(Optional.of(new ChannelAccount("ch-1", "org-1", "pdd", "拼多多测试店铺",
                        "connected", now.plusDays(30), Map.of(), now)));
        when(storeRepository.findByStoreId("store-1"))
                .thenReturn(Optional.of(new Store("store-1", "tenant-1", "org-1", "owner", "pdd", "shop",
                        "name", "connected", BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), now)));
        when(channelClientRegistry.resolve(ChannelType.PDD)).thenReturn(new FakeSyncClient());
        seedToken(now);
    }

    @Test
    void shouldSyncOrdersAndMapIntoCreateOrderSyncRequest() {
        when(orderApplicationService.syncOrder(eq("tenant-1"), any()))
                .thenReturn(new OrderSyncResult("task-1", "store-1", "order-1", "fulfill-1", false))
                .thenReturn(new OrderSyncResult("task-2", "store-1", "order-2", "fulfill-2", true));

        ChannelSyncResult result = service.syncOrders("tenant-1", "ch-1", "store-1");

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.succeededCount()).isEqualTo(1);
        assertThat(result.reusedCount()).isEqualTo(1);
        assertThat(result.failedItems()).isEmpty();
        verify(orderApplicationService, times(2)).syncOrder(eq("tenant-1"), any());
    }

    @Test
    void shouldRejectSyncOrdersWhenChannelNotAuthorized() {
        tokenRepository.deleteAll();
        assertThatThrownBy(() -> service.syncOrders("tenant-1", "ch-1", "store-1"))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8103"));
    }

    @Test
    void shouldReturnPulledProductCountWithoutWritingBack() {
        ChannelSyncResult result = service.syncProducts("tenant-1", "ch-1", "store-1");
        assertThat(result.syncType()).isEqualTo("products");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.succeededCount()).isZero();
    }

    @Test
    void shouldShipOrderThroughClient() {
        ChannelShipRequest request = new ChannelShipRequest("tenant-1", "org-1", "ch-1", "store-1",
                "pdd-order-1001", "sf", "SF123", Map.of());
        ChannelShipResult result = service.shipOrder("tenant-1", request);
        assertThat(result.success()).isTrue();
        assertThat(result.platformShipmentId()).isEqualTo("pdd-ship-pdd-order-1001");
    }

    @Test
    void shouldReturnAfterSalePullResult() {
        ChannelSyncResult result = service.syncAfterSales("tenant-1", "ch-1", "store-1");
        assertThat(result.syncType()).isEqualTo("after_sales");
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void shouldCheckConnectivity() {
        var result = service.checkConnectivity("tenant-1", "ch-1");
        assertThat(result.reachable()).isTrue();
    }

    private void seedToken(OffsetDateTime now) {
        tokenRepository.save(new ChannelToken(null, "tenant-1", "org-1", "pdd", "拼多多测试店铺",
                "access", "refresh", "Bearer", null, now.plusDays(7), now.plusDays(30), Map.of(), now, now));
    }

    static class FakeSyncClient implements ChannelClient {

        @Override
        public ChannelType channelType() {
            return ChannelType.PDD;
        }

        @Override
        public String buildAuthorizeUrl(ChannelAuthContext context) {
            return "http://auth.pdd";
        }

        @Override
        public ChannelToken handleCallback(ChannelCallbackRequest request) {
            return null;
        }

        @Override
        public ChannelToken refreshToken(ChannelToken current) {
            return current;
        }

        @Override
        public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
            return new ChannelConnectivity(true, "mock ok", OffsetDateTime.now());
        }

        @Override
        public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
            return List.of(new ChannelProduct("pdd-prod-1001", "title", "sku-1", new BigDecimal("9.9"), "on_sale", Map.of()));
        }

        @Override
        public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
            return List.of(
                    new ChannelOrder("pdd-order-1001", "p", "sku-1", 1, new BigDecimal("9.9"),
                            "测试买家", "138****0000", "杭州市", Map.of()),
                    new ChannelOrder("pdd-order-1002", "p", "sku-2", 2, new BigDecimal("19.9"),
                            "测试买家", "138****0000", "杭州市", Map.of())
            );
        }

        @Override
        public ChannelShipResult shipOrder(ChannelShipRequest request) {
            return new ChannelShipResult(request.platformOrderId(), true, "ok", "pdd-ship-" + request.platformOrderId());
        }

        @Override
        public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
            return List.of(new ChannelAfterSale("pdd-as-1", "pdd-order-1001", "refunding", "reason",
                    new BigDecimal("9.9"), OffsetDateTime.now(), Map.of()));
        }
    }
}
