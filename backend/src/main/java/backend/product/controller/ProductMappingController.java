package backend.product.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.product.application.ProductMappingService;
import backend.product.dto.CreateProductMappingRequest;
import backend.product.dto.SwitchProductMappingSupplierRequest;
import backend.product.model.ProductSourceMapping;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
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

    @GetMapping("/catalog")
    public ApiResponse<List<ProductMappingService.ProductMappingCatalogView>> listCatalog() {
        return ApiResponse.success(
                productMappingService.listProductMappingCatalog(TenantAccessSupport.requiredTenantId()),
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
