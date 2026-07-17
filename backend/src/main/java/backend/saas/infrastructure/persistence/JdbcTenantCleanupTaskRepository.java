package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.TenantCleanupTaskRepository;
import backend.saas.model.TenantCleanupTaskRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcTenantCleanupTaskRepository implements TenantCleanupTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantCleanupTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantCleanupTaskRecord save(TenantCleanupTaskRecord task) {
        int updated = jdbcTemplate.update("""
                UPDATE tenant_cleanup_task
                SET tenant_id = ?, status = ?, reason = ?, cleanup_scopes_json = ?, requested_by = ?, reviewed_by = ?, executed_by = ?,
                    impact_summary_json = ?, result_summary_json = ?, created_at = ?, executed_at = ?, completed_at = ?
                WHERE cleanup_task_id = ?
                """,
                task.tenantId(),
                task.status(),
                task.reason(),
                task.cleanupScopesJson(),
                task.requestedBy(),
                task.reviewedBy(),
                task.executedBy(),
                task.impactSummaryJson(),
                task.resultSummaryJson(),
                toTimestamp(task.createdAt()),
                toTimestamp(task.executedAt()),
                toTimestamp(task.completedAt()),
                task.cleanupTaskId()
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO tenant_cleanup_task (
                        cleanup_task_id, tenant_id, status, reason, cleanup_scopes_json, requested_by, reviewed_by, executed_by,
                        impact_summary_json, result_summary_json, created_at, executed_at, completed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    task.cleanupTaskId(),
                    task.tenantId(),
                    task.status(),
                    task.reason(),
                    task.cleanupScopesJson(),
                    task.requestedBy(),
                    task.reviewedBy(),
                    task.executedBy(),
                    task.impactSummaryJson(),
                    task.resultSummaryJson(),
                    toTimestamp(task.createdAt()),
                    toTimestamp(task.executedAt()),
                    toTimestamp(task.completedAt())
            );
        }
        return task;
    }

    @Override
    public List<TenantCleanupTaskRecord> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT cleanup_task_id, tenant_id, status, reason, cleanup_scopes_json, requested_by, reviewed_by, executed_by,
                       impact_summary_json, result_summary_json, created_at, executed_at, completed_at
                FROM tenant_cleanup_task
                WHERE tenant_id = ?
                ORDER BY created_at DESC, cleanup_task_id DESC
                """, (rs, rowNum) -> new TenantCleanupTaskRecord(
                rs.getString("cleanup_task_id"),
                rs.getString("tenant_id"),
                rs.getString("status"),
                rs.getString("reason"),
                rs.getString("cleanup_scopes_json"),
                rs.getString("requested_by"),
                rs.getString("reviewed_by"),
                rs.getString("executed_by"),
                rs.getString("impact_summary_json"),
                rs.getString("result_summary_json"),
                fromTimestamp(rs.getTimestamp("created_at")),
                fromTimestamp(rs.getTimestamp("executed_at")),
                fromTimestamp(rs.getTimestamp("completed_at"))
        ), tenantId);
    }

    @Override
    public Optional<TenantCleanupTaskRecord> findByTaskId(String cleanupTaskId) {
        List<TenantCleanupTaskRecord> tasks = jdbcTemplate.query("""
                SELECT cleanup_task_id, tenant_id, status, reason, cleanup_scopes_json, requested_by, reviewed_by, executed_by,
                       impact_summary_json, result_summary_json, created_at, executed_at, completed_at
                FROM tenant_cleanup_task
                WHERE cleanup_task_id = ?
                """, (rs, rowNum) -> new TenantCleanupTaskRecord(
                rs.getString("cleanup_task_id"),
                rs.getString("tenant_id"),
                rs.getString("status"),
                rs.getString("reason"),
                rs.getString("cleanup_scopes_json"),
                rs.getString("requested_by"),
                rs.getString("reviewed_by"),
                rs.getString("executed_by"),
                rs.getString("impact_summary_json"),
                rs.getString("result_summary_json"),
                fromTimestamp(rs.getTimestamp("created_at")),
                fromTimestamp(rs.getTimestamp("executed_at")),
                fromTimestamp(rs.getTimestamp("completed_at"))
        ), cleanupTaskId);
        return tasks.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tenant_cleanup_task");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime fromTimestamp(Timestamp value) {
        return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}

