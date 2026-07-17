package backend.product.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.product.application.ProductService;
import backend.product.dto.CreateCandidateProductRequest;
import backend.product.dto.GenerateProductDraftRequest;
import backend.product.dto.PublishProductDraftRequest;
import backend.product.dto.UpdateCandidateProductRequest;
import backend.product.dto.UpdateCandidateProductStatusRequest;
import backend.product.dto.UpdateProductDraftRequest;
import backend.product.model.CandidateProduct;
import backend.product.model.Product;
import backend.product.model.ProductDraft;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.PRODUCT_MANAGE)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/candidate-products")
    public ApiResponse<List<CandidateProduct>> listCandidateProducts() {
        return ApiResponse.success(
                productService.listCandidateProducts(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/products")
    public ApiResponse<List<Product>> listProducts() {
        return ApiResponse.success(
                productService.listProducts(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/candidate-products")
    public ApiResponse<CandidateProduct> createCandidateProduct(@Valid @RequestBody CreateCandidateProductRequest request) {
        return ApiResponse.success(
                productService.createCandidateProduct(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/candidate-products/{id}")
    public ApiResponse<CandidateProduct> getCandidateProduct(@PathVariable String id) {
        return ApiResponse.success(
                productService.getCandidateProduct(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/candidate-products/{id}")
    public ApiResponse<CandidateProduct> updateCandidateProduct(@PathVariable String id,
                                                                @Valid @RequestBody UpdateCandidateProductRequest request) {
        return ApiResponse.success(
                productService.updateCandidateProduct(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/candidate-products/{id}/status")
    public ApiResponse<CandidateProduct> updateCandidateProductStatus(@PathVariable String id,
                                                                      @Valid @RequestBody UpdateCandidateProductStatusRequest request) {
        return ApiResponse.success(
                productService.updateCandidateProductStatus(TenantAccessSupport.requiredTenantId(), id, request.status()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/product-drafts")
    public ApiResponse<List<ProductDraft>> listProductDrafts() {
        return ApiResponse.success(
                productService.listProductDrafts(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/product-drafts/generate")
    public ApiResponse<ProductDraft> generateProductDraft(@Valid @RequestBody GenerateProductDraftRequest request) {
        return ApiResponse.success(
                productService.generateProductDraft(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/product-drafts/{id}")
    public ApiResponse<ProductDraft> getProductDraft(@PathVariable String id) {
        return ApiResponse.success(
                productService.getProductDraft(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/product-drafts/{id}")
    public ApiResponse<ProductDraft> updateProductDraft(@PathVariable String id,
                                                        @Valid @RequestBody UpdateProductDraftRequest request) {
        return ApiResponse.success(
                productService.updateProductDraft(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/product-drafts/{id}/publish")
    public ApiResponse<Product> publishProductDraft(@PathVariable String id,
                                                    @RequestBody(required = false) PublishProductDraftRequest request) {
        PublishProductDraftRequest actualRequest = request == null ? new PublishProductDraftRequest(null) : request;
        return ApiResponse.success(
                productService.publishProductDraft(TenantAccessSupport.requiredTenantId(), id, actualRequest),
                TraceIdHolder.get()
        );
    }
}

