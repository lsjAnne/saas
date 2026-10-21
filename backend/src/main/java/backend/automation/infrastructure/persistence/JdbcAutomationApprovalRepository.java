package backend.automation.infrastructure.persistence;

import backend.automation.domain.repository.AutomationApprovalRepository;
import backend.automation.model.AutomationApproval;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcAutomationApprovalRepository implements AutomationApprovalRepository {

    private static final RowMapper<AutomationApproval> ROW_MAPPER = (rs, rowNum) -> new AutomationApproval(
            rs.getString("task_id"),
            rs.getString("execution_id"),
            rs.getString("tenant_id"),
            rs.getString("store_id"),
            rs.getString("target_type"),
            rs.getString("target_id"),
            rs.getString("reason"),
            rs.getString("status"),
            rs.getString("reviewed_by"),
            toOffsetDateTime(rs.getTimestamp("reviewed_at")),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcAutomationApprovalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AutomationApproval save(AutomationApproval approval) {
        if (findByTaskId(approval.taskId()).isPresent()) {
            jdbcTemplate.update("""
                            UPDATE automation_approval
                            SET status = ?, reviewed_by = ?, reviewed_at = ?
                            WHERE task_id = ?
                            """,
                    approval.status(), approval.reviewedBy(), toTimestamp(approval.reviewedAt()), approval.taskId());
            return approval;
        }
        jdbcTemplate.update("""
                        INSERT INTO automation_approval (
                            task_id, execution_id, tenant_id, store_id, target_type, target_id,
                            reason, status, reviewed_by, reviewed_at, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                approval.taskId(), approval.executionId(), approval.tenantId(), approval.storeId(),
                approval.targetType(), approval.targetId(), approval.reason(), approval.status(),
                approval.reviewedBy(), toTimestamp(approval.reviewedAt()), toTimestamp(approval.createdAt()));
        return approval;
    }

    @Override
    public List<AutomationApproval> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT task_id, execution_id, tenant_id, store_id, target_type, target_id,
                               reason, status, reviewed_by, reviewed_at, created_at
                        FROM automation_approval
                        WHERE tenant_id = ?
                        ORDER BY created_at DESC, task_id
                        """,
                ROW_MAPPER, tenantId);
    }

    @Override
    public Optional<AutomationApproval> findByTaskId(String taskId) {
        return jdbcTemplate.query("""
                        SELECT task_id, execution_id, tenant_id, store_id, target_type, target_id,
                               reason, status, reviewed_by, reviewed_at, created_at
                        FROM automation_approval
                        WHERE task_id = ?
                        """,
                ROW_MAPPER, taskId).stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM automation_approval");
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
