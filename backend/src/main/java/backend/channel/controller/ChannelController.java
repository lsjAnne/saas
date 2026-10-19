package backend.channel.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.auth.security.RequireTenantPermission;
import backend.channel.application.ChannelAuthService;
import backend.channel.application.ChannelSyncService;
import backend.channel.dto.AuthorizeChannelRequest;
import backend.channel.dto.ChannelAuthorizeView;
import backend.channel.dto.ChannelConnectRequest;
import backend.channel.dto.ChannelSyncRequest;
import backend.channel.dto.ChannelSyncResult;
import backend.channel.dto.ChannelTokenView;
import backend.channel.dto.RefreshChannelTokenRequest;
import backend.channel.dto.ShipOrderRequest;
import backend.channel.model.ChannelConnectivity;
import backend.channel.model.ChannelShipRequest;
import backend.channel.model.ChannelShipResult;
import backend.channel.model.ChannelType;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.tenant.context.TenantAccessSupport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/channels")
@RequireTenantPermission(AuthPermissionCodes.CHANNEL_MANAGE)
public class ChannelController {

    private final ChannelAuthService channelAuthService;
    private final ChannelSyncService channelSyncService;

    public ChannelController(ChannelAuthService channelAuthService,
                             ChannelSyncService channelSyncService) {
        this.channelAuthService = channelAuthService;
        this.channelSyncService = channelSyncService;
    }

    @PostMapping("/{channelType}/authorize")
    public ApiResponse<ChannelAuthorizeView> authorize(@PathVariable String channelType,
                                                       @Valid @RequestBody AuthorizeChannelRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        return ApiResponse.success(
                channelAuthService.startAuthorization(tenantId, request.organizationId(), ChannelType.fromCode(channelType)),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{channelType}/refresh-token")
    public ApiResponse<ChannelTokenView> refreshToken(@PathVariable String channelType,
                                                     @Valid @RequestBody RefreshChannelTokenRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        return ApiResponse.success(
                channelAuthService.refreshToken(tenantId, request.channelTokenId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/sync/orders")
    @RequireTenantPermission(AuthPermissionCodes.CHANNEL_SYNC_MANAGE)
    public ApiResponse<ChannelSyncResult> syncOrders(@Valid @RequestBody ChannelSyncRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        return ApiResponse.success(
                channelSyncService.syncOrders(tenantId, request.channelAccountId(), request.storeId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/sync/products")
    @RequireTenantPermission(AuthPermissionCodes.CHANNEL_SYNC_MANAGE)
    public ApiResponse<ChannelSyncResult> syncProducts(@Valid @RequestBody ChannelSyncRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        return ApiResponse.success(
                channelSyncService.syncProducts(tenantId, request.channelAccountId(), request.storeId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/sync/after-sales")
    @RequireTenantPermission(AuthPermissionCodes.CHANNEL_SYNC_MANAGE)
    public ApiResponse<ChannelSyncResult> syncAfterSales(@Valid @RequestBody ChannelSyncRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        return ApiResponse.success(
                channelSyncService.syncAfterSales(tenantId, request.channelAccountId(), request.storeId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/ship")
    @RequireTenantPermission(AuthPermissionCodes.CHANNEL_SYNC_MANAGE)
    public ApiResponse<ChannelShipResult> ship(@Valid @RequestBody ShipOrderRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        ChannelShipRequest shipRequest = new ChannelShipRequest(
                tenantId,
                null,
                request.channelAccountId(),
                request.storeId(),
                request.platformOrderId(),
                request.logisticsCompany(),
                request.logisticsNo(),
                Map.of()
        );
        return ApiResponse.success(
                channelSyncService.shipOrder(tenantId, shipRequest),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{channelType}/connectivity")
    @RequireTenantPermission(AuthPermissionCodes.CHANNEL_SYNC_MANAGE)
    public ApiResponse<ChannelConnectivity> connectivity(@PathVariable String channelType,
                                                         @Valid @RequestBody ChannelConnectRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        return ApiResponse.success(
                channelSyncService.checkConnectivity(tenantId, request.channelAccountId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/tokens")
    public ApiResponse<List<ChannelTokenView>> listTokens() {
        String tenantId = TenantAccessSupport.requiredTenantId();
        return ApiResponse.success(
                channelAuthService.listTokens(tenantId),
                TraceIdHolder.get()
        );
    }
}
