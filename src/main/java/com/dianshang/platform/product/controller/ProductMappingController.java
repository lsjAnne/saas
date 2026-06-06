package com.dianshang.platform.product.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.product.application.ProductMappingService;
import com.dianshang.platform.product.dto.CreateProductMappingRequest;
import com.dianshang.platform.product.dto.SwitchProductMappingSupplierRequest;
import com.dianshang.platform.product.model.ProductSourceMapping;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-mappings")
@RequireTenantPermission(AuthPermissionCodes.PRODUCT_MANAGE)
public class ProductMappingController {

    private final ProductMappingService productMappingService;

    public ProductMappingController(ProductMappingService productMappingService) {
        this.productMappingService = productMappingService;
    }

    @GetMapping
    public ApiResponse<List<ProductSourceMapping>> list() {
        return ApiResponse.success(
                productMappingService.listProductMappings(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping
    public ApiResponse<ProductSourceMapping> create(@Valid @RequestBody CreateProductMappingRequest request) {
        return ApiResponse.success(
                productMappingService.createProductMapping(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/switch-supplier")
    public ApiResponse<ProductSourceMapping> switchSupplier(@PathVariable String id,
                                                            @Valid @RequestBody SwitchProductMappingSupplierRequest request) {
        return ApiResponse.success(
                productMappingService.switchSupplier(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }
}
