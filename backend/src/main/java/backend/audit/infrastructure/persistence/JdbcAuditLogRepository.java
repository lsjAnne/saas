package backend.audit.infrastructure.persistence;

import backend.audit.model.AuditLogRecord;
import backend.audit.domain.repository.AuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcAuditLogRepository implements AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        jdbcTemplate.update(
                """
                INSERT INTO audit_log (tenant_id, operator_id, operator_type, action_type, target_type, target_id, trace_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                record.tenantId(),
                record.operatorId(),
                record.operatorType(),
                record.actionType(),
                record.targetType(),
                record.targetId(),
                record.traceId(),
                toTimestamp(record.createdAt())
        );
        return record;
    }

    @Override
    public List<AuditLogRecord> findAll() {
        return jdbcTemplate.query(
                """
                SELECT tenant_id, operator_id, operator_type, action_type, target_type, target_id, trace_id, created_at
                FROM audit_log
                ORDER BY id
                """,
                (rs, rowNum) -> new AuditLogRecord(
                        rs.getString("tenant_id"),
                        rs.getString("operator_id"),
                        rs.getString("operator_type"),
                        rs.getString("action_type"),
                        rs.getString("target_type"),
                        rs.getString("target_id"),
                        rs.getString("trace_id"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                )
        );
    }

    @Override
    public List<AuditLogRecord> findByTenantId(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT tenant_id, operator_id, operator_type, action_type, target_type, target_id, trace_id, created_at
                FROM audit_log
                WHERE tenant_id = ?
                ORDER BY id
                """,
                (rs, rowNum) -> new AuditLogRecord(
                        rs.getString("tenant_id"),
                        rs.getString("operator_id"),
                        rs.getString("operator_type"),
                        rs.getString("action_type"),
                        rs.getString("target_type"),
                        rs.getString("target_id"),
                        rs.getString("trace_id"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                tenantId
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM audit_log");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

