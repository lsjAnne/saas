package backend.channel.application;

import backend.audit.application.AuditLogService;
import backend.channel.domain.repository.ChannelTokenRepository;
import backend.channel.dto.ChannelSyncItem;
import backend.channel.dto.ChannelSyncResult;
import backend.channel.model.ChannelAfterSale;
import backend.channel.model.ChannelAuthContext;
import backend.channel.model.ChannelConnectivity;
import backend.channel.model.ChannelOrder;
import backend.channel.model.ChannelProduct;
import backend.channel.model.ChannelShipRequest;
import backend.channel.model.ChannelShipResult;
import backend.channel.model.ChannelToken;
import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import backend.order.application.OrderApplicationService;
import backend.order.dto.CreateOrderSyncRequest;
import backend.order.dto.OrderSyncResult;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.ChannelAccount;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 渠道同步编排：订单/商品/售后拉取、发货推送、连通性检查。本轮订单同步复用 OrderApplicationService.syncOrder 入口。
 */
@Service
public class ChannelSyncService {

    private final ChannelAccountRepository channelAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final ChannelTokenRepository channelTokenRepository;
    private final ChannelClientRegistry channelClientRegistry;
    private final ChannelPlatformProperties channelPlatformProperties;
    private final OrderApplicationService orderApplicationService;
    private final AuditLogService auditLogService;

    public ChannelSyncService(ChannelAccountRepository channelAccountRepository,
                              OrganizationRepository organizationRepository,
                              StoreRepository storeRepository,
                              ChannelTokenRepository channelTokenRepository,
                              ChannelClientRegistry channelClientRegistry,
                              ChannelPlatformProperties channelPlatformProperties,
                              OrderApplicationService orderApplicationService,
                              AuditLogService auditLogService) {
        this.channelAccountRepository = channelAccountRepository;
        this.organizationRepository = organizationRepository;
        this.storeRepository = storeRepository;
        this.channelTokenRepository = channelTokenRepository;
        this.channelClientRegistry = channelClientRegistry;
        this.channelPlatformProperties = channelPlatformProperties;
        this.orderApplicationService = orderApplicationService;
        this.auditLogService = auditLogService;
    }

    public ChannelSyncResult syncOrders(String tenantId, String channelAccountId, String storeId) {
        requireOwnedStore(tenantId, storeId);
        ResolvedChannel resolved = resolveChannel(tenantId, channelAccountId, storeId);
        ChannelClient client = channelClientRegistry.resolve(ChannelType.fromCode(resolved.account().channelType()));
        List<ChannelOrder> orders = client.pullOrders(resolved.context());

        int total = orders.size();
        int succeeded = 0;
        int reused = 0;
        List<ChannelSyncItem> failed = new ArrayList<>();
        for (ChannelOrder order : orders) {
            try {
                OrderSyncResult result = orderApplicationService.syncOrder(tenantId, new CreateOrderSyncRequest(
                        storeId,
                        order.platformOrderId(),
                        null,
                        order.skuId(),
                        order.quantity(),
                        order.unitPrice(),
                        order.buyerName(),
                        order.buyerPhoneMask(),
                        order.shippingAddress()));
                if (result.reused()) {
                    reused++;
                } else {
                    succeeded++;
                }
            } catch (Exception exception) {
                failed.add(new ChannelSyncItem(order.platformOrderId(), exception.getMessage()));
            }
        }
        auditLogService.recordForTenant(tenantId, "CHANNEL_SYNC_ORDERS", "channel_account", channelAccountId);
        return new ChannelSyncResult(resolved.account().channelType(), storeId, "orders", total, succeeded, reused, failed);
    }

    public ChannelSyncResult syncProducts(String tenantId, String channelAccountId, String storeId) {
        requireOwnedStore(tenantId, storeId);
        ResolvedChannel resolved = resolveChannel(tenantId, channelAccountId, storeId);
        ChannelClient client = channelClientRegistry.resolve(ChannelType.fromCode(resolved.account().channelType()));
        List<ChannelProduct> products = client.pullProducts(resolved.context());
        // 待联调：本轮不创建商品主数据/供应商映射，真实回写 ProductMappingService 待后续轮次接入。
        auditLogService.recordForTenant(tenantId, "CHANNEL_SYNC_PRODUCTS", "channel_account", channelAccountId);
        return new ChannelSyncResult(resolved.account().channelType(), storeId, "products", products.size(), 0, 0, List.of());
    }

    public ChannelShipResult shipOrder(String tenantId, ChannelShipRequest request) {
        ResolvedChannel resolved = resolveChannel(tenantId, request.channelAccountId(), request.storeId());
        ChannelClient client = channelClientRegistry.resolve(ChannelType.fromCode(resolved.account().channelType()));
        ChannelShipResult result = client.shipOrder(request);
        auditLogService.recordForTenant(tenantId, "CHANNEL_SHIP_ORDER", "channel_account", request.channelAccountId());
        return result;
    }

    public ChannelSyncResult syncAfterSales(String tenantId, String channelAccountId, String storeId) {
        requireOwnedStore(tenantId, storeId);
        ResolvedChannel resolved = resolveChannel(tenantId, channelAccountId, storeId);
        ChannelClient client = channelClientRegistry.resolve(ChannelType.fromCode(resolved.account().channelType()));
        List<ChannelAfterSale> afterSales = client.pullAfterSales(resolved.context());
        // 待联调：售后单落库/异常单联动待后续轮次接入。
        auditLogService.recordForTenant(tenantId, "CHANNEL_SYNC_AFTER_SALES", "channel_account", channelAccountId);
        return new ChannelSyncResult(resolved.account().channelType(), storeId, "after_sales", afterSales.size(), 0, 0, List.of());
    }

    public ChannelConnectivity checkConnectivity(String tenantId, String channelAccountId) {
        ChannelAccount account = channelAccountRepository.findByChannelAccountId(channelAccountId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedOrganization(tenantId, account.organizationId());
        ChannelType channelType = ChannelType.fromCode(account.channelType());
        ChannelPlatformProperties.ChannelPlatformSettings settings = channelPlatformProperties.settings(channelType);
        ChannelAuthContext context = new ChannelAuthContext(
                tenantId,
                account.organizationId(),
                channelAccountId,
                null,
                settings.getAppKey(),
                settings.getAppSecret(),
                null,
                Map.of()
        );
        ChannelConnectivity result = channelClientRegistry.resolve(channelType).checkConnectivity(context);
        auditLogService.recordForTenant(tenantId, "CHANNEL_CONNECTIVITY_CHECK", "channel_account", channelAccountId);
        return result;
    }

    private ResolvedChannel resolveChannel(String tenantId, String channelAccountId, String storeId) {
        ChannelAccount account = channelAccountRepository.findByChannelAccountId(channelAccountId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedOrganization(tenantId, account.organizationId());
        ChannelToken token = channelTokenRepository
                .findByOrganizationAndTypeAndAccount(account.organizationId(), account.channelType(), account.accountName())
                .orElseThrow(() -> new BusinessException("8103", "channel not authorized, please authorize first", HttpStatus.BAD_REQUEST));
        ChannelToken usable = requireUsableToken(token);
        ChannelPlatformProperties.ChannelPlatformSettings settings = channelPlatformProperties
                .settings(ChannelType.fromCode(usable.channelType()));
        ChannelAuthContext context = new ChannelAuthContext(
                tenantId,
                account.organizationId(),
                channelAccountId,
                storeId,
                settings.getAppKey(),
                settings.getAppSecret(),
                null,
                Map.of()
        );
        return new ResolvedChannel(account, usable, context);
    }

    private ChannelToken requireUsableToken(ChannelToken token) {
        if (token.expiresAt() != null && token.expiresAt().isBefore(OffsetDateTime.now())) {
            ChannelClient client = channelClientRegistry.resolve(ChannelType.fromCode(token.channelType()));
            ChannelToken refreshed = client.refreshToken(token);
            return channelTokenRepository.save(new ChannelToken(
                    token.channelTokenId(),
                    token.tenantId(),
                    token.organizationId(),
                    token.channelType(),
                    refreshed.accountName(),
                    refreshed.accessToken(),
                    refreshed.refreshToken(),
                    refreshed.tokenType(),
                    refreshed.scope(),
                    refreshed.expiresAt(),
                    refreshed.refreshExpiresAt(),
                    refreshed.extraConfig() == null ? Map.of() : refreshed.extraConfig(),
                    token.createdAt(),
                    OffsetDateTime.now()
            ));
        }
        return token;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private Organization requireOwnedOrganization(String tenantId, String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(organization.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return organization;
    }

    private record ResolvedChannel(ChannelAccount account, ChannelToken token, ChannelAuthContext context) {
    }
}
