package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.UsageQuotaRepository;
import backend.saas.model.UsageQuota;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcUsageQuotaRepository implements UsageQuotaRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUsageQuotaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveAll(String tenantId, List<UsageQuota> quotas) {
        long tenantDbId = JdbcIdCodec.parseTenantId(tenantId);
        jdbcTemplate.update("DELETE FROM usage_quota WHERE tenant_id = ?", tenantDbId);
        for (UsageQuota quota : quotas) {
            jdbcTemplate.update(
                    """
                    INSERT INTO usage_quota (tenant_id, quota_code, quota_limit, used_amount, reset_at)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    tenantDbId,
                    quota.quotaCode(),
                    quota.quotaLimit(),
                    quota.usedAmount(),
                    toTimestamp(quota.resetAt())
            );
        }
    }

    @Override
    public List<UsageQuota> findByTenantId(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT tenant_id, quota_code, quota_limit, used_amount, reset_at
                FROM usage_quota
                WHERE tenant_id = ?
                ORDER BY id
                """,
                (rs, rowNum) -> new UsageQuota(
                        "tenant-" + rs.getLong("tenant_id"),
                        rs.getString("quota_code"),
                        rs.getInt("quota_limit"),
                        rs.getInt("used_amount"),
                        toOffsetDateTime(rs.getTimestamp("reset_at"))
                ),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM usage_quota");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

