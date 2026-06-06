package com.dianshang.platform.store.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.store.application.StoreChannelService;
import com.dianshang.platform.store.dto.CreateChannelAccountRequest;
import com.dianshang.platform.store.model.ChannelAccount;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
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
