package backend.contentasset.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.contentasset.application.ContentAssetService;
import backend.contentasset.dto.ArchiveContentAssetVersionRequest;
import backend.contentasset.dto.ContentAssetDetailView;
import backend.contentasset.dto.ContentAssetReferenceCopyView;
import backend.contentasset.dto.GenerateContentAssetRequest;
import backend.contentasset.dto.UploadContentAssetRequest;
import backend.contentasset.model.ContentAsset;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
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
@RequestMapping("/api/content-assets")
@RequireTenantPermission(AuthPermissionCodes.PRODUCT_MANAGE)
public class ContentAssetController {

    private final ContentAssetService contentAssetService;

    public ContentAssetController(ContentAssetService contentAssetService) {
        this.contentAssetService = contentAssetService;
    }

    @GetMapping
    public ApiResponse<List<ContentAsset>> listAssets() {
        return ApiResponse.success(
                contentAssetService.listAssets(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentAssetDetailView> getAssetDetail(@PathVariable String id) {
        return ApiResponse.success(
                contentAssetService.getAssetDetail(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/upload")
    public ApiResponse<ContentAssetDetailView> uploadAsset(@Valid @RequestBody UploadContentAssetRequest request) {
        return ApiResponse.success(
                contentAssetService.uploadAsset(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/generate")
    public ApiResponse<ContentAssetDetailView> generateAsset(@Valid @RequestBody GenerateContentAssetRequest request) {
        return ApiResponse.success(
                contentAssetService.generateAsset(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/archive-version")
    public ApiResponse<ContentAssetDetailView> archiveVersion(@PathVariable String id,
                                                              @RequestBody(required = false) ArchiveContentAssetVersionRequest request) {
        ArchiveContentAssetVersionRequest actualRequest = request == null
                ? new ArchiveContentAssetVersionRequest(null)
                : request;
        return ApiResponse.success(
                contentAssetService.archiveLatestVersion(TenantAccessSupport.requiredTenantId(), id, actualRequest),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/copy-reference")
    public ApiResponse<ContentAssetReferenceCopyView> copyReference(@PathVariable String id) {
        return ApiResponse.success(
                contentAssetService.copyReference(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ContentAssetDetailView> publishAsset(@PathVariable String id,
                                                            @RequestBody(required = false) Map<String, String> request) {
        Map<String, String> actualRequest = request == null ? Map.of() : request;
        return ApiResponse.success(
                contentAssetService.publishVideoAsset(
                        TenantAccessSupport.requiredTenantId(),
                        id,
                        actualRequest.get("platformCode"),
                        actualRequest.get("publishTitle"),
                        actualRequest.get("publishRemark")
                ),
                TraceIdHolder.get()
        );
    }
}

