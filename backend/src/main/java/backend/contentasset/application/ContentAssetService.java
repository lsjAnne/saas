package backend.contentasset.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.contentasset.domain.repository.ContentAssetRepository;
import backend.contentasset.dto.ArchiveContentAssetVersionRequest;
import backend.contentasset.dto.ContentAssetDetailView;
import backend.contentasset.dto.ContentAssetReferenceCopyView;
import backend.contentasset.dto.GenerateContentAssetRequest;
import backend.contentasset.dto.UploadContentAssetRequest;
import backend.contentasset.model.ContentAsset;
import backend.contentasset.model.ContentAssetReference;
import backend.contentasset.model.ContentAssetVersion;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ContentAssetService {

    private static final List<String> ALLOWED_CATEGORIES = List.of("product", "live", "service");
    private static final List<String> ALLOWED_TYPES = List.of("image", "video", "script", "faq", "copy", "cover");

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final ContentAssetRepository contentAssetRepository;

    public ContentAssetService(AuditLogService auditLogService,
                               StoreRepository storeRepository,
                               ContentAssetRepository contentAssetRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.contentAssetRepository = contentAssetRepository;
    }

    public List<ContentAsset> listAssets(String tenantId) {
        return contentAssetRepository.findAssetsByStoreIds(ownedStoreIds(tenantId));
    }

    public ContentAssetDetailView getAssetDetail(String tenantId, String assetId) {
        ContentAsset asset = requireOwnedAsset(tenantId, assetId);
        return buildDetail(asset);
    }

    public ContentAssetDetailView uploadAsset(String tenantId, UploadContentAssetRequest request) {
        validateCategory(request.assetCategory());
        validateType(request.assetType());
        requireOwnedStore(tenantId, request.storeId());

        OffsetDateTime now = OffsetDateTime.now();
        ContentAsset asset = new ContentAsset(
                nextId("asset"),
                request.storeId(),
                request.assetCategory(),
                request.assetType(),
                request.assetName(),
                "active",
                resolvePreviewMode(request.assetType()),
                blankToNull(request.sourceUrl()),
                fallback(request.previewText(), request.assetName() + " uploaded and ready for reuse"),
                false,
                "upload",
                now,
                now
        );
        contentAssetRepository.saveAsset(asset);
        contentAssetRepository.saveVersion(new ContentAssetVersion(
                nextId("asset-version"),
                asset.assetId(),
                1,
                "v1",
                "active",
                fallback(request.changeSummary(), "initial upload"),
                fallback(request.previewText(), request.assetName()),
                blankToNull(request.sourceUrl()),
                now
        ));
        buildDefaultReferences(asset, blankToNull(request.referenceHint()), now)
                .forEach(contentAssetRepository::saveReference);
        auditLogService.recordForTenant(tenantId, "UPLOAD_CONTENT_ASSET", "content_asset", asset.assetId());
        return buildDetail(asset);
    }

    public ContentAssetDetailView generateAsset(String tenantId, GenerateContentAssetRequest request) {
        validateCategory(request.assetCategory());
        validateType(request.assetType());
        requireOwnedStore(tenantId, request.storeId());

        if (shouldGenerateProductVideo(request)) {
            return generateProductVideoAsset(tenantId, request);
        }

        OffsetDateTime now = OffsetDateTime.now();
        String generatedPreview = buildGeneratedPreview(request);
        String assetId = nextId("asset");
        ContentAsset asset = new ContentAsset(
                assetId,
                request.storeId(),
                request.assetCategory(),
                request.assetType(),
                request.assetName(),
                "active",
                resolvePreviewMode(request.assetType()),
                buildGeneratedPreviewUrl(request.assetType(), assetId, null, null, null, null),
                generatedPreview,
                true,
                "generate",
                now,
                now
        );
        contentAssetRepository.saveAsset(asset);
        contentAssetRepository.saveVersion(new ContentAssetVersion(
                nextId("asset-version"),
                asset.assetId(),
                1,
                "v1",
                "active",
                "AI generated first version",
                generatedPreview,
                asset.previewUrl(),
                now
        ));
        buildDefaultReferences(asset, blankToNull(request.referenceHint()), now)
                .forEach(contentAssetRepository::saveReference);
        auditLogService.recordForTenant(tenantId, "GENERATE_CONTENT_ASSET", "content_asset", asset.assetId());
        return buildDetail(asset);
    }

    public ContentAssetDetailView publishVideoAsset(String tenantId,
                                                    String assetId,
                                                    String platformCode,
                                                    String publishTitle,
                                                    String publishRemark) {
        ContentAsset asset = requireOwnedAsset(tenantId, assetId);
        if (!"video".equals(asset.assetType())) {
            throw new BusinessException("7604", "only video asset can be published", HttpStatus.BAD_REQUEST);
        }

        if (platformCode == null || platformCode.isBlank()) {
            throw new BusinessException("7605", "publish platform is required", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<ContentAssetVersion> versions = sortedVersions(asset.assetId());
        int nextVersionNo = versions.isEmpty() ? 1 : versions.get(0).versionNo() + 1;
        contentAssetRepository.saveVersion(new ContentAssetVersion(
                nextId("asset-version"),
                asset.assetId(),
                nextVersionNo,
                "v" + nextVersionNo,
                "active",
                "published to " + platformCode,
                buildPublishSnapshot(platformCode, publishTitle, publishRemark),
                asset.previewUrl(),
                now
        ));
        contentAssetRepository.saveReference(new ContentAssetReference(
                nextId("asset-ref"),
                asset.assetId(),
                "publish",
                "video-publish-record",
                platformCode,
                buildPublishReferenceQuote(publishTitle, publishRemark),
                now
        ));

        ContentAsset updatedAsset = new ContentAsset(
                asset.assetId(),
                asset.storeId(),
                asset.assetCategory(),
                asset.assetType(),
                asset.assetName(),
                asset.assetStatus(),
                asset.previewMode(),
                asset.previewUrl(),
                appendPublishPreview(asset.previewText(), platformCode, publishTitle),
                asset.generated(),
                asset.sourceChannel(),
                asset.createdAt(),
                now
        );
        contentAssetRepository.saveAsset(updatedAsset);
        auditLogService.recordForTenant(tenantId, "PUBLISH_CONTENT_VIDEO_ASSET", "content_asset", asset.assetId());
        return buildDetail(updatedAsset);
    }

    public ContentAssetDetailView archiveLatestVersion(String tenantId,
                                                       String assetId,
                                                       ArchiveContentAssetVersionRequest request) {
        ContentAsset current = requireOwnedAsset(tenantId, assetId);
        List<ContentAssetVersion> versions = sortedVersions(assetId);
        if (versions.isEmpty()) {
            throw new BusinessException("7601", "content asset has no version history", HttpStatus.BAD_REQUEST);
        }

        ContentAssetVersion latest = versions.get(0);
        ContentAssetVersion archived = new ContentAssetVersion(
                latest.versionId(),
                latest.assetId(),
                latest.versionNo(),
                latest.versionLabel(),
                "archived",
                appendRemark(latest.changeSummary(), request == null ? null : request.remark()),
                latest.contentSnapshot(),
                latest.previewUrl(),
                latest.createdAt()
        );
        contentAssetRepository.saveVersion(archived);

        ContentAsset updated = new ContentAsset(
                current.assetId(),
                current.storeId(),
                current.assetCategory(),
                current.assetType(),
                current.assetName(),
                "archived",
                current.previewMode(),
                current.previewUrl(),
                current.previewText(),
                current.generated(),
                current.sourceChannel(),
                current.createdAt(),
                OffsetDateTime.now()
        );
        contentAssetRepository.saveAsset(updated);
        auditLogService.recordForTenant(tenantId, "ARCHIVE_CONTENT_ASSET_VERSION", "content_asset", assetId);
        return buildDetail(updated);
    }

    public ContentAssetReferenceCopyView copyReference(String tenantId, String assetId) {
        ContentAsset asset = requireOwnedAsset(tenantId, assetId);
        List<ContentAssetReference> references = sortedReferences(assetId);
        StringBuilder builder = new StringBuilder();
        builder.append("asset://").append(asset.assetId()).append(System.lineSeparator())
                .append("name=").append(asset.assetName()).append(System.lineSeparator())
                .append("category=").append(asset.assetCategory()).append(System.lineSeparator())
                .append("source=").append(asset.sourceChannel());
        if (!references.isEmpty()) {
            builder.append(System.lineSeparator()).append("references=");
            builder.append(String.join(" | ", references.stream()
                    .map(reference -> reference.referenceType() + ":" + reference.referenceTargetId())
                    .toList()));
        }
        auditLogService.recordForTenant(tenantId, "COPY_CONTENT_ASSET_REFERENCE", "content_asset", asset.assetId());
        return new ContentAssetReferenceCopyView(
                asset.assetId(),
                asset.assetName(),
                builder.toString(),
                references.size(),
                OffsetDateTime.now()
        );
    }

    public void clear() {
        contentAssetRepository.deleteAll();
    }

    private ContentAssetDetailView buildDetail(ContentAsset asset) {
        return new ContentAssetDetailView(
                asset,
                sortedVersions(asset.assetId()),
                sortedReferences(asset.assetId())
        );
    }

    private List<ContentAssetVersion> sortedVersions(String assetId) {
        return contentAssetRepository.findVersionsByAssetId(assetId).stream()
                .sorted(Comparator.comparingInt(ContentAssetVersion::versionNo).reversed()
                        .thenComparing(ContentAssetVersion::createdAt, Comparator.reverseOrder()))
                .toList();
    }

    private List<ContentAssetReference> sortedReferences(String assetId) {
        return contentAssetRepository.findReferencesByAssetId(assetId).stream()
                .sorted(Comparator.comparing(ContentAssetReference::createdAt).reversed())
                .toList();
    }

    private ContentAsset requireOwnedAsset(String tenantId, String assetId) {
        ContentAsset asset = contentAssetRepository.findAssetById(assetId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, asset.storeId());
        return asset;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "invalid tenant context", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private void validateCategory(String assetCategory) {
        if (!ALLOWED_CATEGORIES.contains(assetCategory)) {
            throw new BusinessException("7602", "invalid asset category", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateType(String assetType) {
        if (!ALLOWED_TYPES.contains(assetType)) {
            throw new BusinessException("7603", "invalid asset type", HttpStatus.BAD_REQUEST);
        }
    }

    private List<ContentAssetReference> buildDefaultReferences(ContentAsset asset,
                                                               String referenceHint,
                                                               OffsetDateTime now) {
        return switch (asset.assetCategory()) {
            case "product" -> List.of(
                    new ContentAssetReference(nextId("asset-ref"), asset.assetId(), "product", "product-detail-draft", asset.storeId() + "-draft", fallback(referenceHint, "used by product detail and selling points"), now),
                    new ContentAssetReference(nextId("asset-ref"), asset.assetId(), "product", "short-video-product-card", asset.storeId() + "-product", "used by product placement and demo script", now)
            );
            case "live" -> List.of(
                    new ContentAssetReference(nextId("asset-ref"), asset.assetId(), "live", "live-opening-script", asset.storeId() + "-live-script", fallback(referenceHint, "used by opening and transition"), now),
                    new ContentAssetReference(nextId("asset-ref"), asset.assetId(), "live", "live-product-demo-card", asset.storeId() + "-live-card", "used by live demo and clips", now)
            );
            default -> List.of(
                    new ContentAssetReference(nextId("asset-ref"), asset.assetId(), "service", "service-quick-reply", asset.storeId() + "-service-reply", fallback(referenceHint, "used by support SOP and quick reply"), now),
                    new ContentAssetReference(nextId("asset-ref"), asset.assetId(), "service", "after-sale-faq", asset.storeId() + "-service-faq", "used by after-sale FAQ", now)
            );
        };
    }

    private ContentAssetDetailView generateProductVideoAsset(String tenantId, GenerateContentAssetRequest request) {
        ContentAsset sourceAsset = requireOwnedAsset(tenantId, request.sourceAssetId());
        validateProductVideoSource(sourceAsset);

        OffsetDateTime now = OffsetDateTime.now();
        String clipTemplate = fallback(request.clipTemplate(), "highlight-carousel");
        int durationSeconds = request.durationSeconds() == null || request.durationSeconds() <= 0
                ? 20
                : request.durationSeconds();
        String publishPlatform = fallback(request.publishPlatform(), "douyin");
        String previewText = buildGeneratedVideoPreview(request, sourceAsset, clipTemplate, durationSeconds, publishPlatform);
        String assetId = nextId("asset");

        ContentAsset asset = new ContentAsset(
                assetId,
                request.storeId(),
                request.assetCategory(),
                request.assetType(),
                request.assetName(),
                "active",
                "video",
                buildGeneratedPreviewUrl(
                        "video",
                        assetId,
                        sourceAsset.assetId(),
                        clipTemplate,
                        publishPlatform,
                        durationSeconds
                ),
                previewText,
                true,
                "video-generate",
                now,
                now
        );
        contentAssetRepository.saveAsset(asset);

        contentAssetRepository.saveVersion(new ContentAssetVersion(
                nextId("asset-version"),
                asset.assetId(),
                1,
                "v1",
                "archived",
                "storyboard generated from " + sourceAsset.assetName(),
                buildStoryboardSnapshot(request, sourceAsset, durationSeconds),
                sourceAsset.previewUrl(),
                now
        ));
        contentAssetRepository.saveVersion(new ContentAssetVersion(
                nextId("asset-version"),
                asset.assetId(),
                2,
                "v2",
                "active",
                "edited cut ready | template=" + clipTemplate + " | duration=" + durationSeconds + "s",
                buildEditedCutSnapshot(sourceAsset, clipTemplate, durationSeconds, publishPlatform),
                asset.previewUrl(),
                now
        ));

        contentAssetRepository.saveReference(new ContentAssetReference(
                nextId("asset-ref"),
                asset.assetId(),
                "source_asset",
                "product-image-source",
                sourceAsset.assetId(),
                fallback(request.referenceHint(), "used as the source image for product short video"),
                now
        ));
        contentAssetRepository.saveReference(new ContentAssetReference(
                nextId("asset-ref"),
                asset.assetId(),
                "clip_template",
                "video-clip-template",
                clipTemplate,
                "edited with " + clipTemplate,
                now
        ));
        contentAssetRepository.saveReference(new ContentAssetReference(
                nextId("asset-ref"),
                asset.assetId(),
                "publish_target",
                "planned-publish-platform",
                publishPlatform,
                "planned publish target for the current edited cut",
                now
        ));
        auditLogService.recordForTenant(tenantId, "GENERATE_PRODUCT_VIDEO_ASSET", "content_asset", asset.assetId());
        return buildDetail(asset);
    }

    private String buildGeneratedPreview(GenerateContentAssetRequest request) {
        String categoryLabel = switch (request.assetCategory()) {
            case "product" -> "product-asset";
            case "live" -> "live-asset";
            default -> "service-asset";
        };
        return "%s | %s | %s".formatted(
                categoryLabel,
                fallback(request.tone(), "high-conversion"),
                request.brief()
        );
    }

    private String buildGeneratedVideoPreview(GenerateContentAssetRequest request,
                                              ContentAsset sourceAsset,
                                              String clipTemplate,
                                              int durationSeconds,
                                              String publishPlatform) {
        return "product-video | source=%s | template=%s | duration=%ss | target=%s | %s".formatted(
                sourceAsset.assetName(),
                clipTemplate,
                durationSeconds,
                publishPlatform,
                request.brief()
        );
    }

    private String buildGeneratedPreviewUrl(String assetType,
                                            String assetId,
                                            String sourceAssetId,
                                            String clipTemplate,
                                            String publishPlatform,
                                            Integer durationSeconds) {
        return switch (assetType) {
            case "image", "cover" -> buildGeneratedPreviewUri("image", assetId, null, null, null, null);
            case "video" -> buildGeneratedPreviewUri("video", assetId, sourceAssetId, clipTemplate, publishPlatform, durationSeconds);
            default -> null;
        };
    }

    private String buildGeneratedPreviewUri(String type,
                                            String assetId,
                                            String sourceAssetId,
                                            String clipTemplate,
                                            String publishPlatform,
                                            Integer durationSeconds) {
        StringBuilder builder = new StringBuilder("generated://").append(type);
        if (assetId != null && !assetId.isBlank()) {
            builder.append("/").append(assetId);
        }
        List<String> params = new java.util.ArrayList<>();
        if (sourceAssetId != null && !sourceAssetId.isBlank()) {
            params.add("source=" + sourceAssetId);
        }
        if (clipTemplate != null && !clipTemplate.isBlank()) {
            params.add("template=" + clipTemplate);
        }
        if (publishPlatform != null && !publishPlatform.isBlank()) {
            params.add("target=" + publishPlatform);
        }
        if (durationSeconds != null && durationSeconds > 0) {
            params.add("duration=" + durationSeconds);
        }
        if (!params.isEmpty()) {
            builder.append("?").append(String.join("&", params));
        }
        return builder.toString();
    }

    private String resolvePreviewMode(String assetType) {
        return switch (assetType) {
            case "image", "cover" -> "image";
            case "video" -> "video";
            default -> "text";
        };
    }

    private boolean shouldGenerateProductVideo(GenerateContentAssetRequest request) {
        return "video".equals(request.assetType()) && request.sourceAssetId() != null && !request.sourceAssetId().isBlank();
    }

    private void validateProductVideoSource(ContentAsset sourceAsset) {
        if (!"product".equals(sourceAsset.assetCategory())) {
            throw new BusinessException("7606", "product video source must belong to product assets", HttpStatus.BAD_REQUEST);
        }
        if (!List.of("image", "cover").contains(sourceAsset.assetType())) {
            throw new BusinessException("7607", "product video source must be an image or cover asset", HttpStatus.BAD_REQUEST);
        }
    }

    private String buildStoryboardSnapshot(GenerateContentAssetRequest request,
                                           ContentAsset sourceAsset,
                                           int durationSeconds) {
        return """
                storyboard:
                - scene 1: hook shot with %s
                - scene 2: detail close-up and selling points
                - scene 3: offer + call to action
                duration=%ss
                brief=%s
                """.formatted(sourceAsset.assetName(), durationSeconds, request.brief());
    }

    private String buildEditedCutSnapshot(ContentAsset sourceAsset,
                                          String clipTemplate,
                                          int durationSeconds,
                                          String publishPlatform) {
        return """
                edited-cut:
                - source=%s
                - template=%s
                - duration=%ss
                - target=%s
                """.formatted(sourceAsset.assetId(), clipTemplate, durationSeconds, publishPlatform);
    }

    private String buildPublishSnapshot(String platformCode, String publishTitle, String publishRemark) {
        return "publish package | platform=%s | title=%s | remark=%s".formatted(
                platformCode,
                fallback(publishTitle, "untitled-video-package"),
                fallback(publishRemark, "none")
        );
    }

    private String buildPublishReferenceQuote(String publishTitle, String publishRemark) {
        return "title=%s | remark=%s".formatted(
                fallback(publishTitle, "untitled-video-package"),
                fallback(publishRemark, "none")
        );
    }

    private String appendPublishPreview(String previewText, String platformCode, String publishTitle) {
        return fallback(previewText, "video asset ready") + " | published to " + platformCode + " as " + fallback(publishTitle, "untitled-video-package");
    }

    private String appendRemark(String summary, String remark) {
        if (remark == null || remark.isBlank()) {
            return summary;
        }
        return summary + " | archived remark=" + remark;
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
