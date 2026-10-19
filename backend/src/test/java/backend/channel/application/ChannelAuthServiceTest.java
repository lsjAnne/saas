package backend.channel.application;

import backend.audit.application.AuditLogService;
import backend.channel.domain.repository.ChannelAuthStateRepository;
import backend.channel.domain.repository.ChannelTokenRepository;
import backend.channel.infrastructure.persistence.InMemoryChannelAuthStateRepository;
import backend.channel.infrastructure.persistence.InMemoryChannelTokenRepository;
import backend.channel.model.ChannelAfterSale;
import backend.channel.model.ChannelAuthContext;
import backend.channel.model.ChannelAuthState;
import backend.channel.model.ChannelCallbackRequest;
import backend.channel.model.ChannelConnectivity;
import backend.channel.model.ChannelOrder;
import backend.channel.model.ChannelProduct;
import backend.channel.model.ChannelShipRequest;
import backend.channel.model.ChannelShipResult;
import backend.channel.model.ChannelToken;
import backend.channel.model.ChannelType;
import backend.channel.dto.ChannelTokenView;
import backend.common.exception.BusinessException;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.model.ChannelAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelAuthServiceTest {

    private OrganizationRepository organizationRepository;
    private ChannelAccountRepository channelAccountRepository;
    private AuditLogService auditLogService;
    private InMemoryChannelAuthStateRepository authStateRepository;
    private InMemoryChannelTokenRepository tokenRepository;
    private ChannelPlatformProperties properties;
    private ChannelAuthService service;
    private FakePddClient pddClient;

    @BeforeEach
    void setUp() {
        organizationRepository = mock(OrganizationRepository.class);
        channelAccountRepository = mock(ChannelAccountRepository.class);
        auditLogService = mock(AuditLogService.class);
        authStateRepository = new InMemoryChannelAuthStateRepository();
        tokenRepository = new InMemoryChannelTokenRepository();
        properties = new ChannelPlatformProperties();
        pddClient = new FakePddClient();
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(pddClient));
        service = new ChannelAuthService(
                organizationRepository,
                channelAccountRepository,
                authStateRepository,
                tokenRepository,
                registry,
                properties,
                auditLogService
        );
    }

    @Test
    void shouldStartAuthorizationAndPersistState() {
        when(organizationRepository.findById("org-1"))
                .thenReturn(Optional.of(new Organization("org-1", "tenant-1", "org", "active", OffsetDateTime.now())));

        var view = service.startAuthorization("tenant-1", "org-1", ChannelType.PDD);

        assertThat(view.channelType()).isEqualTo("pdd");
        assertThat(view.authorizeUrl()).startsWith("http://auth.pinduoduo.com");
        assertThat(authStateRepository.findByState(view.state())).isPresent();
    }

    @Test
    void shouldRejectStartAuthorizationWhenOrganizationNotOwned() {
        when(organizationRepository.findById("org-2"))
                .thenReturn(Optional.of(new Organization("org-2", "tenant-other", "org", "active", OffsetDateTime.now())));

        assertThatThrownBy(() -> service.startAuthorization("tenant-1", "org-2", ChannelType.PDD))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("1005"));
    }

    @Test
    void shouldCompleteAuthorizationAndUpsertAccountAndDeleteState() {
        OffsetDateTime now = OffsetDateTime.now();
        authStateRepository.save(new ChannelAuthState(null, "tenant-1", "org-1", "pdd", null,
                "state-ok", "/callback", now, now.plusMinutes(30)));

        ChannelTokenView view = service.completeAuthorization(ChannelType.PDD, "code-1", "state-ok", Map.of());

        assertThat(view.accountName()).isEqualTo("拼多多测试店铺");
        assertThat(tokenRepository.findByChannelTokenId(view.channelTokenId())).isPresent();
        verify(channelAccountRepository).save(any(ChannelAccount.class));
        assertThat(authStateRepository.findByState("state-ok")).isEmpty();
    }

    @Test
    void shouldRejectCallbackWhenStateMissingOrExpired() {
        assertThatThrownBy(() -> service.completeAuthorization(ChannelType.PDD, "code", "missing", Map.of()))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8102"));

        OffsetDateTime now = OffsetDateTime.now();
        authStateRepository.save(new ChannelAuthState(null, "tenant-1", "org-1", "pdd", null,
                "state-expired", "/callback", now.minusHours(1), now.minusMinutes(1)));
        assertThatThrownBy(() -> service.completeAuthorization(ChannelType.PDD, "code", "state-expired", Map.of()))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8102"));
    }

    @Test
    void shouldRejectRefreshWhenTenantMismatch() {
        ChannelToken saved = seedToken("tenant-1", "org-1", "pdd", OffsetDateTime.now().plusDays(7));

        assertThatThrownBy(() -> service.refreshToken("tenant-2", saved.channelTokenId()))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("1005"));
    }

    @Test
    void shouldRefreshTokenAndExtendExpiry() {
        ChannelToken saved = seedToken("tenant-1", "org-1", "pdd", OffsetDateTime.now().minusDays(1));

        ChannelTokenView view = service.refreshToken("tenant-1", saved.channelTokenId());

        assertThat(view.expiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
    }

    @Test
    void shouldAutoRefreshExpiredTokenOnRequireUsableToken() {
        ChannelToken expired = seedToken("tenant-1", "org-1", "pdd", OffsetDateTime.now().minusDays(1));

        ChannelToken usable = service.requireUsableToken("tenant-1", expired.channelTokenId());

        assertThat(usable.expiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
    }

    private ChannelToken seedToken(String tenantId, String orgId, String channelType, OffsetDateTime expiresAt) {
        OffsetDateTime now = OffsetDateTime.now();
        return tokenRepository.save(new ChannelToken(
                null, tenantId, orgId, channelType, "拼多多测试店铺",
                "mock-access-pdd", "mock-refresh-pdd", "Bearer", null,
                expiresAt, now.plusDays(30), Map.of(), now, now));
    }

    static class FakePddClient implements ChannelClient {

        @Override
        public ChannelType channelType() {
            return ChannelType.PDD;
        }

        @Override
        public String buildAuthorizeUrl(ChannelAuthContext context) {
            return "http://auth.pinduoduo.com?redirect=" + context.redirectUri();
        }

        @Override
        public ChannelToken handleCallback(ChannelCallbackRequest request) {
            OffsetDateTime now = OffsetDateTime.now();
            return new ChannelToken(null, null, null, "pdd", "拼多多测试店铺",
                    "mock-access-pdd", "mock-refresh-pdd", "Bearer", null,
                    now.plusDays(7), now.plusDays(30), Map.of(), now, now);
        }

        @Override
        public ChannelToken refreshToken(ChannelToken current) {
            OffsetDateTime now = OffsetDateTime.now();
            return new ChannelToken(current.channelTokenId(), current.tenantId(), current.organizationId(),
                    current.channelType(), current.accountName(), "mock-access-pdd", current.refreshToken(),
                    "Bearer", current.scope(), now.plusDays(7), current.refreshExpiresAt(),
                    current.extraConfig(), current.createdAt(), now);
        }

        @Override
        public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
            return new ChannelConnectivity(true, "mock ok", OffsetDateTime.now());
        }

        @Override
        public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
            return List.of();
        }

        @Override
        public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
            return List.of();
        }

        @Override
        public ChannelShipResult shipOrder(ChannelShipRequest request) {
            return new ChannelShipResult(request.platformOrderId(), true, "ok", "pdd-ship-1");
        }

        @Override
        public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
            return List.of();
        }
    }
}
