package backend.saas.infrastructure.persistence;

import backend.common.exception.BusinessException;
import backend.saas.domain.repository.TenantProfileRepository;
import backend.saas.model.TenantProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcTenantProfileRepository implements TenantProfileRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcTenantProfileRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public TenantProfile save(TenantProfile profile) {
        long tenantDbId = JdbcIdCodec.parseTenantId(profile.tenantId());
        int updated = jdbcTemplate.update(
                """
                UPDATE tenant
                SET tenant_code = ?, tenant_name = ?, tenant_status = ?, trial_end_at = ?, owner_name = ?, mobile = ?, default_organization_id = ?, feature_flags = CAST(? AS JSON)
                WHERE id = ?
                """,
                profile.tenantCode(),
                profile.tenantName(),
                profile.tenantStatus(),
                toTimestamp(profile.trialEndAt()),
                profile.ownerName(),
                profile.mobile(),
                profile.defaultOrganizationId(),
                toFeatureFlagsJson(profile.featureFlags()),
                tenantDbId
        );
        if (updated == 0) {
                jdbcTemplate.update(
                    """
                    INSERT INTO tenant (id, tenant_code, tenant_name, tenant_status, trial_end_at, owner_name, mobile, default_organization_id, feature_flags, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                    """,
                    tenantDbId,
                    profile.tenantCode(),
                    profile.tenantName(),
                    profile.tenantStatus(),
                    toTimestamp(profile.trialEndAt()),
                    profile.ownerName(),
                    profile.mobile(),
                    profile.defaultOrganizationId(),
                    toFeatureFlagsJson(profile.featureFlags()),
                    toTimestamp(profile.createdAt())
            );
        }
        return profile;
    }

    @Override
    public Optional<TenantProfile> findByTenantId(String tenantId) {
        List<TenantProfile> profiles = jdbcTemplate.query(
                """
                SELECT id, tenant_code, tenant_name, tenant_status, trial_end_at, owner_name, mobile, default_organization_id, feature_flags, created_at
                FROM tenant
                WHERE id = ?
                """,
                (rs, rowNum) -> new TenantProfile(
                        "tenant-" + rs.getLong("id"),
                        rs.getString("tenant_code"),
                        rs.getString("tenant_name"),
                        rs.getString("tenant_status"),
                        rs.getString("owner_name"),
                        rs.getString("mobile"),
                        rs.getString("default_organization_id"),
                        parseFeatureFlags(rs.getString("feature_flags")),
                        toOffsetDateTime(rs.getTimestamp("trial_end_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseTenantId(tenantId)
        );
        return profiles.stream().findFirst();
    }

    @Override
    public List<TenantProfile> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_code, tenant_name, tenant_status, trial_end_at, owner_name, mobile, default_organization_id, feature_flags, created_at
                FROM tenant
                ORDER BY id
                """,
                (rs, rowNum) -> new TenantProfile(
                        "tenant-" + rs.getLong("id"),
                        rs.getString("tenant_code"),
                        rs.getString("tenant_name"),
                        rs.getString("tenant_status"),
                        rs.getString("owner_name"),
                        rs.getString("mobile"),
                        rs.getString("default_organization_id"),
                        parseFeatureFlags(rs.getString("feature_flags")),
                        toOffsetDateTime(rs.getTimestamp("trial_end_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                )
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tenant");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private String toFeatureFlagsJson(Map<String, Boolean> featureFlags) {
        try {
            return featureFlags == null || featureFlags.isEmpty()
                    ? null
                    : objectMapper.writeValueAsString(featureFlags);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("1002", "feature flags serialize failed", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Boolean> parseFeatureFlags(String featureFlagsJson) {
        if (featureFlagsJson == null || featureFlagsJson.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(featureFlagsJson);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            return objectMapper.convertValue(node, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException("1002", "feature flags parse failed", HttpStatus.BAD_REQUEST);
        }
    }
}

