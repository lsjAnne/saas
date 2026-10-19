package backend.channel.application;

import backend.audit.application.AuditLogService;
import backend.channel.domain.repository.ChannelAuthStateRepository;
import backend.channel.domain.repository.ChannelTokenRepository;
import backend.channel.dto.ChannelAuthorizeView;
import backend.channel.dto.ChannelTokenView;
import backend.channel.model.ChannelAuthContext;
import backend.channel.model.ChannelAuthState;
import backend.channel.model.ChannelCallbackRequest;
import backend.channel.model.ChannelToken;
import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.model.ChannelAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 渠道授权闭环：发起授权 → 回调换 token → 令牌刷新/过期自动续期。
 */
@Service
public class ChannelAuthService {

    private static final long AUTH_STATE_TTL_MINUTES = 30;

    private final OrganizationRepository organizationRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelAuthStateRepository authStateRepository;
    private final ChannelTokenRepository tokenRepository;
    private final ChannelClientRegistry channelClientRegistry;
    private final ChannelPlatformProperties channelPlatformProperties;
    private final AuditLogService auditLogService;

    public ChannelAuthService(OrganizationRepository organizationRepository,
                              ChannelAccountRepository channelAccountRepository,
                              ChannelAuthStateRepository authStateRepository,
                              ChannelTokenRepository tokenRepository,
                              ChannelClientRegistry channelClientRegistry,
                              ChannelPlatformProperties channelPlatformProperties,
                              AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.authStateRepository = authStateRepository;
        this.tokenRepository = tokenRepository;
        this.channelClientRegistry = channelClientRegistry;
        this.channelPlatformProperties = channelPlatformProperties;
        this.auditLogService = auditLogService;
    }

    public ChannelAuthorizeView startAuthorization(String tenantId, String organizationId, ChannelType channelType) {
        requireOwnedOrganization(tenantId, organizationId);
        ChannelPlatformProperties.ChannelPlatformSettings settings = channelPlatformProperties.settings(channelType);
        // 生产环境回调域名应由部署配置覆盖，这里仅做本地默认拼接。
        String redirectUri = "http://localhost:8080" + settings.getCallbackPath();
        String state = UUID.randomUUID().toString();
        ChannelAuthState authState = new ChannelAuthState(
                null,
                tenantId,
                organizationId,
                channelType.code(),
                null,
                state,
                redirectUri,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(AUTH_STATE_TTL_MINUTES)
        );
        authStateRepository.save(authState);

        ChannelAuthContext context = new ChannelAuthContext(
                tenantId,
                organizationId,
                null,
                null,
                settings.getAppKey(),
                settings.getAppSecret(),
                redirectUri,
                Map.of()
        );
        String authorizeUrl = channelClientRegistry.resolve(channelType).buildAuthorizeUrl(context);
        auditLogService.recordForTenant(tenantId, "CHANNEL_AUTHORIZE_START", "channel_auth_state", state);
        return new ChannelAuthorizeView(channelType.code(), authorizeUrl, state);
    }

    public ChannelTokenView completeAuthorization(ChannelType channelType, String code, String state, Map<String, String> rawParams) {
        ChannelAuthState authState = authStateRepository.findByState(state)
                .filter(item -> item.expiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new BusinessException("8102", "invalid or expired authorization state", HttpStatus.BAD_REQUEST));

        ChannelClient client = channelClientRegistry.resolve(channelType);
        ChannelToken token = client.handleCallback(new ChannelCallbackRequest(channelType, code, state, rawParams));

        ChannelToken existing = tokenRepository
                .findByOrganizationAndTypeAndAccount(authState.organizationId(), channelType.code(), token.accountName())
                .orElse(null);
        OffsetDateTime now = OffsetDateTime.now();
        ChannelToken toSave = new ChannelToken(
                existing == null ? null : existing.channelTokenId(),
                authState.tenantId(),
                authState.organizationId(),
                channelType.code(),
                token.accountName(),
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.scope(),
                token.expiresAt(),
                token.refreshExpiresAt(),
                normalizeMap(token.extraConfig()),
                existing == null ? now : existing.createdAt(),
                now
        );
        ChannelToken saved = tokenRepository.save(toSave);

        upsertChannelAccount(authState, saved);
        authStateRepository.deleteByState(state);
        auditLogService.recordForTenant(authState.tenantId(), "CHANNEL_AUTHORIZE_CALLBACK", "channel_token", saved.channelTokenId());
        return toView(saved);
    }

    public ChannelTokenView refreshToken(String tenantId, String channelTokenId) {
        ChannelToken current = tokenRepository.findByChannelTokenId(channelTokenId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(current.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        ChannelClient client = channelClientRegistry.resolve(ChannelType.fromCode(current.channelType()));
        ChannelToken refreshed = client.refreshToken(current);
        OffsetDateTime now = OffsetDateTime.now();
        ChannelToken saved = tokenRepository.save(new ChannelToken(
                current.channelTokenId(),
                current.tenantId(),
                current.organizationId(),
                current.channelType(),
                refreshed.accountName(),
                refreshed.accessToken(),
                refreshed.refreshToken(),
                refreshed.tokenType(),
                refreshed.scope(),
                refreshed.expiresAt(),
                refreshed.refreshExpiresAt(),
                normalizeMap(refreshed.extraConfig()),
                current.createdAt(),
                now
        ));
        auditLogService.recordForTenant(tenantId, "CHANNEL_TOKEN_REFRESH", "channel_token", saved.channelTokenId());
        return toView(saved);
    }

    public ChannelToken requireUsableToken(String tenantId, String channelTokenId) {
        ChannelToken current = tokenRepository.findByChannelTokenId(channelTokenId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(current.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        if (current.expiresAt() != null && current.expiresAt().isBefore(OffsetDateTime.now())) {
            return doRefresh(tenantId, current);
        }
        return current;
    }

    public List<ChannelTokenView> listTokens(String tenantId) {
        List<String> organizationIds = organizationRepository.findByTenantId(tenantId).stream()
                .map(Organization::id)
                .toList();
        return tokenRepository.findByOrganizationIds(organizationIds).stream()
                .map(this::toView)
                .toList();
    }

    private ChannelToken doRefresh(String tenantId, ChannelToken current) {
        ChannelClient client = channelClientRegistry.resolve(ChannelType.fromCode(current.channelType()));
        ChannelToken refreshed = client.refreshToken(current);
        OffsetDateTime now = OffsetDateTime.now();
        return tokenRepository.save(new ChannelToken(
                current.channelTokenId(),
                current.tenantId(),
                current.organizationId(),
                current.channelType(),
                refreshed.accountName(),
                refreshed.accessToken(),
                refreshed.refreshToken(),
                refreshed.tokenType(),
                refreshed.scope(),
                refreshed.expiresAt(),
                refreshed.refreshExpiresAt(),
                normalizeMap(refreshed.extraConfig()),
                current.createdAt(),
                now
        ));
    }

    private void upsertChannelAccount(ChannelAuthState authState, ChannelToken saved) {
        if (authState.channelAccountId() != null && !authState.channelAccountId().isBlank()) {
            ChannelAccount account = channelAccountRepository.findByChannelAccountId(authState.channelAccountId())
                    .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
            if (!authState.organizationId().equals(account.organizationId())) {
                throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
            }
            channelAccountRepository.save(account.withAuthStatus("connected", saved.expiresAt()));
            return;
        }
        channelAccountRepository.save(new ChannelAccount(
                null,
                authState.organizationId(),
                ChannelType.fromCode(authState.channelType()).code(),
                saved.accountName(),
                "connected",
                saved.expiresAt(),
                Map.of(),
                OffsetDateTime.now()
        ));
    }

    private ChannelTokenView toView(ChannelToken token) {
        return new ChannelTokenView(
                token.channelTokenId(),
                token.tenantId(),
                token.organizationId(),
                token.channelType(),
                token.accountName(),
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.scope(),
                token.expiresAt(),
                token.refreshExpiresAt(),
                token.extraConfig(),
                token.createdAt(),
                token.updatedAt()
        );
    }

    private Organization requireOwnedOrganization(String tenantId, String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(organization.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return organization;
    }

    private Map<String, Object> normalizeMap(Map<String, Object> values) {
        return values == null ? Map.of() : values;
    }
}
