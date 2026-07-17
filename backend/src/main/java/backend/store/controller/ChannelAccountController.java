package backend.store.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.store.application.StoreChannelService;
import backend.store.dto.CreateChannelAccountRequest;
import backend.store.model.ChannelAccount;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channel-accounts")
@RequireTenantPermission(AuthPermissionCodes.CHANNEL_MANAGE)
public class ChannelAccountController {

    private final StoreChannelService storeChannelService;

    public ChannelAccountController(StoreChannelService storeChannelService) {
        this.storeChannelService = storeChannelService;
    }

    @GetMapping
    public ApiResponse<List<ChannelAccount>> list() {
        return ApiResponse.success(
                storeChannelService.listChannelAccounts(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping
    public ApiResponse<ChannelAccount> create(@Valid @RequestBody CreateChannelAccountRequest request) {
        return ApiResponse.success(
                storeChannelService.createChannelAccount(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/refresh-auth")
    public ApiResponse<ChannelAccount> refreshAuth(@PathVariable String id) {
        return ApiResponse.success(
                storeChannelService.refreshChannelAuth(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }
}

