package backend.contentasset.infrastructure.persistence;

import backend.contentasset.domain.repository.ContentAssetRepository;
import backend.contentasset.model.ContentAsset;
import backend.contentasset.model.ContentAssetReference;
import backend.contentasset.model.ContentAssetVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcContentAssetRepository implements ContentAssetRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcContentAssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ContentAsset saveAsset(ContentAsset asset) {
        if (exists("SELECT COUNT(1) FROM content_asset WHERE asset_id = ?", asset.assetId())) {
            jdbcTemplate.update(
                    """
                    UPDATE content_asset
                    SET store_id = ?, asset_category = ?, asset_type = ?, asset_name = ?, asset_status = ?, preview_mode = ?,
                        preview_url = ?, preview_text = ?, generated_flag = ?, source_channel = ?, created_at = ?, updated_at = ?
                    WHERE asset_id = ?
                    """,
                    asset.storeId(),
                    asset.assetCategory(),
                    asset.assetType(),
                    asset.assetName(),
                    asset.assetStatus(),
                    asset.previewMode(),
                    asset.previewUrl(),
                    asset.previewText(),
                    asset.generated(),
                    asset.sourceChannel(),
                    toTimestamp(asset.createdAt()),
                    toTimestamp(asset.updatedAt()),
                    asset.assetId()
            );
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO content_asset (
                        asset_id, store_id, asset_category, asset_type, asset_name, asset_status, preview_mode,
                        preview_url, preview_text, generated_flag, source_channel, created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    asset.assetId(),
                    asset.storeId(),
                    asset.assetCategory(),
                    asset.assetType(),
                    asset.assetName(),
                    asset.assetStatus(),
                    asset.previewMode(),
                    asset.previewUrl(),
                    asset.previewText(),
                    asset.generated(),
                    asset.sourceChannel(),
                    toTimestamp(asset.createdAt()),
                    toTimestamp(asset.updatedAt())
            );
        }
        return asset;
    }

    @Override
    public List<ContentAsset> findAssetsByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", storeIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query(
                """
                SELECT asset_id, store_id, asset_category, asset_type, asset_name, asset_status, preview_mode,
                       preview_url, preview_text, generated_flag, source_channel, created_at, updated_at
                FROM content_asset
                WHERE store_id IN (%s)
                ORDER BY updated_at DESC, asset_id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new ContentAsset(
                        rs.getString("asset_id"),
                        rs.getString("store_id"),
                        rs.getString("asset_category"),
                        rs.getString("asset_type"),
                        rs.getString("asset_name"),
                        rs.getString("asset_status"),
                        rs.getString("preview_mode"),
                        rs.getString("preview_url"),
                        rs.getString("preview_text"),
                        rs.getBoolean("generated_flag"),
                        rs.getString("source_channel"),
                        toOffsetDateTime(rs.getTimestamp("created_at")),
                        toOffsetDateTime(rs.getTimestamp("updated_at"))
                ),
                storeIds.toArray()
        );
    }

    @Override
    public Optional<ContentAsset> findAssetById(String assetId) {
        List<ContentAsset> assets = jdbcTemplate.query(
                """
                SELECT asset_id, store_id, asset_category, asset_type, asset_name, asset_status, preview_mode,
                       preview_url, preview_text, generated_flag, source_channel, created_at, updated_at
                FROM content_asset
                WHERE asset_id = ?
                """,
                (rs, rowNum) -> new ContentAsset(
                        rs.getString("asset_id"),
                        rs.getString("store_id"),
                        rs.getString("asset_category"),
                        rs.getString("asset_type"),
                        rs.getString("asset_name"),
                        rs.getString("asset_status"),
                        rs.getString("preview_mode"),
                        rs.getString("preview_url"),
                        rs.getString("preview_text"),
                        rs.getBoolean("generated_flag"),
                        rs.getString("source_channel"),
                        toOffsetDateTime(rs.getTimestamp("created_at")),
                        toOffsetDateTime(rs.getTimestamp("updated_at"))
                ),
                assetId
        );
        return assets.stream().findFirst();
    }

    @Override
    public ContentAssetVersion saveVersion(ContentAssetVersion version) {
        if (exists("SELECT COUNT(1) FROM content_asset_version WHERE version_id = ?", version.versionId())) {
            jdbcTemplate.update(
                    """
                    UPDATE content_asset_version
                    SET asset_id = ?, version_no = ?, version_label = ?, version_status = ?, change_summary = ?,
                        content_snapshot = ?, preview_url = ?, created_at = ?
                    WHERE version_id = ?
                    """,
                    version.assetId(),
                    version.versionNo(),
                    version.versionLabel(),
                    version.versionStatus(),
                    version.changeSummary(),
                    version.contentSnapshot(),
                    version.previewUrl(),
                    toTimestamp(version.createdAt()),
                    version.versionId()
            );
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO content_asset_version (
                        version_id, asset_id, version_no, version_label, version_status, change_summary,
                        content_snapshot, preview_url, created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    version.versionId(),
                    version.assetId(),
                    version.versionNo(),
                    version.versionLabel(),
                    version.versionStatus(),
                    version.changeSummary(),
                    version.contentSnapshot(),
                    version.previewUrl(),
                    toTimestamp(version.createdAt())
            );
        }
        return version;
    }

    @Override
    public List<ContentAssetVersion> findVersionsByAssetId(String assetId) {
        return jdbcTemplate.query(
                """
                SELECT version_id, asset_id, version_no, version_label, version_status, change_summary,
                       content_snapshot, preview_url, created_at
                FROM content_asset_version
                WHERE asset_id = ?
                ORDER BY version_no DESC, created_at DESC
                """,
                (rs, rowNum) -> new ContentAssetVersion(
                        rs.getString("version_id"),
                        rs.getString("asset_id"),
                        rs.getInt("version_no"),
                        rs.getString("version_label"),
                        rs.getString("version_status"),
                        rs.getString("change_summary"),
                        rs.getString("content_snapshot"),
                        rs.getString("preview_url"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                assetId
        );
    }

    @Override
    public ContentAssetReference saveReference(ContentAssetReference reference) {
        if (exists("SELECT COUNT(1) FROM content_asset_reference WHERE reference_id = ?", reference.referenceId())) {
            jdbcTemplate.update(
                    """
                    UPDATE content_asset_reference
                    SET asset_id = ?, reference_type = ?, reference_name = ?, reference_target_id = ?,
                        quote_text = ?, created_at = ?
                    WHERE reference_id = ?
                    """,
                    reference.assetId(),
                    reference.referenceType(),
                    reference.referenceName(),
                    reference.referenceTargetId(),
                    reference.quoteText(),
                    toTimestamp(reference.createdAt()),
                    reference.referenceId()
            );
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO content_asset_reference (
                        reference_id, asset_id, reference_type, reference_name, reference_target_id, quote_text, created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    reference.referenceId(),
                    reference.assetId(),
                    reference.referenceType(),
                    reference.referenceName(),
                    reference.referenceTargetId(),
                    reference.quoteText(),
                    toTimestamp(reference.createdAt())
            );
        }
        return reference;
    }

    @Override
    public List<ContentAssetReference> findReferencesByAssetId(String assetId) {
        return jdbcTemplate.query(
                """
                SELECT reference_id, asset_id, reference_type, reference_name, reference_target_id, quote_text, created_at
                FROM content_asset_reference
                WHERE asset_id = ?
                ORDER BY created_at DESC, reference_id DESC
                """,
                (rs, rowNum) -> new ContentAssetReference(
                        rs.getString("reference_id"),
                        rs.getString("asset_id"),
                        rs.getString("reference_type"),
                        rs.getString("reference_name"),
                        rs.getString("reference_target_id"),
                        rs.getString("quote_text"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                assetId
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM content_asset_reference");
        jdbcTemplate.update("DELETE FROM content_asset_version");
        jdbcTemplate.update("DELETE FROM content_asset");
    }

    private boolean exists(String sql, String id) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

