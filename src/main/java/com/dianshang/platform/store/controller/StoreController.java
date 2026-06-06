package com.dianshang.platform.store.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.store.application.StoreChannelService;
import com.dianshang.platform.store.dto.ConnectStoreRequest;
import com.dianshang.platform.store.dto.UpdateStoreSettingsRequest;
import com.dianshang.platform.store.model.Store;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequireTenantPermission(AuthPermissionCodes.STORE_MANAGE)
public class StoreController {

    private final StoreChannelService storeChannelService;

    public StoreController(StoreChannelService storeChannelService) {
        this.storeChannelService = storeChannelService;
    }

    @PostMapping("/connect")
    public ApiResponse<Store> connect(@Valid @RequestBody ConnectStoreRequest request) {
        return ApiResponse.success(
                storeChannelService.connectStore(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping
    public ApiResponse<List<Store>> list() {
        return ApiResponse.success(
                storeChannelService.listStores(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<Store> detail(@PathVariable String id) {
        return ApiResponse.success(
                storeChannelService.getStore(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/{id}/settings")
    public ApiResponse<Store> updateSettings(@PathVariable String id,
                                             @Valid @RequestBody UpdateStoreSettingsRequest request) {
        return ApiResponse.success(
                storeChannelService.updateStoreSettings(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }
}
