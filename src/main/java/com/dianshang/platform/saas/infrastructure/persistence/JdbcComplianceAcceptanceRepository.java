package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.ComplianceAcceptanceRepository;
import com.dianshang.platform.saas.model.ComplianceAcceptanceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcComplianceAcceptanceRepository implements ComplianceAcceptanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcComplianceAcceptanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ComplianceAcceptanceRecord save(ComplianceAcceptanceRecord acceptance) {
        jdbcTemplate.update("""
                INSERT INTO tenant_compliance_acceptance (
                    acceptance_id, tenant_id, document_code, document_version,
                    accepted_by, accepted_source, accepted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                acceptance.acceptanceId(),
                acceptance.tenantId(),
                acceptance.documentCode(),
                acceptance.documentVersion(),
                acceptance.acceptedBy(),
                acceptance.acceptedSource(),
                toTimestamp(acceptance.acceptedAt())
        );
        return acceptance;
    }

    @Override
    public List<ComplianceAcceptanceRecord> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT acceptance_id, tenant_id, document_code, document_version,
                       accepted_by, accepted_source, accepted_at
                FROM tenant_compliance_acceptance
                WHERE tenant_id = ?
                ORDER BY accepted_at DESC, acceptance_id DESC
                """, (rs, rowNum) -> new ComplianceAcceptanceRecord(
                rs.getString("acceptance_id"),
                rs.getString("tenant_id"),
                rs.getString("document_code"),
                rs.getString("document_version"),
                rs.getString("accepted_by"),
                rs.getString("accepted_source"),
                fromTimestamp(rs.getTimestamp("accepted_at"))
        ), tenantId);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tenant_compliance_acceptance");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime fromTimestamp(Timestamp value) {
        return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
