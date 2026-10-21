package backend.automation.infrastructure.persistence;

import backend.automation.domain.repository.AutomationExecutionRepository;
import backend.automation.model.AutomationExecution;
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
public class JdbcAutomationExecutionRepository implements AutomationExecutionRepository {

    private static final RowMapper<AutomationExecution> ROW_MAPPER = (rs, rowNum) -> new AutomationExecution(
            rs.getString("execution_id"),
            rs.getString("tenant_id"),
            rs.getString("store_id"),
            rs.getString("rule_id"),
            rs.getString("action_type"),
            rs.getString("trigger_source"),
            rs.getString("target_type"),
            rs.getString("target_id"),
            rs.getString("result"),
            rs.getString("status"),
            rs.getInt("retry_count"),
            rs.getLong("cost_ms"),
            rs.getString("error_message"),
            toOffsetDateTime(rs.getTimestamp("executed_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcAutomationExecutionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AutomationExecution save(AutomationExecution execution) {
        if (findByExecutionId(execution.executionId()).isPresent()) {
            jdbcTemplate.update("""
                            UPDATE automation_execution
                            SET result = ?, status = ?, retry_count = ?, cost_ms = ?, error_message = ?, executed_at = ?
                            WHERE execution_id = ?
                            """,
                    execution.result(), execution.status(), execution.retryCount(), execution.costMs(),
                    execution.errorMessage(), toTimestamp(execution.executedAt()), execution.executionId());
            return execution;
        }
        jdbcTemplate.update("""
                        INSERT INTO automation_execution (
                            execution_id, tenant_id, store_id, rule_id, action_type, trigger_source,
                            target_type, target_id, result, status, retry_count, cost_ms, error_message, executed_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                execution.executionId(), execution.tenantId(), execution.storeId(), execution.ruleId(),
                execution.actionType(), execution.triggerSource(), execution.targetType(), execution.targetId(),
                execution.result(), execution.status(), execution.retryCount(), execution.costMs(),
                execution.errorMessage(), toTimestamp(execution.executedAt()));
        return execution;
    }

    @Override
    public List<AutomationExecution> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT execution_id, tenant_id, store_id, rule_id, action_type, trigger_source,
                               target_type, target_id, result, status, retry_count, cost_ms, error_message, executed_at
                        FROM automation_execution
                        WHERE tenant_id = ?
                        ORDER BY executed_at DESC, execution_id
                        """,
                ROW_MAPPER, tenantId);
    }

    @Override
    public Optional<AutomationExecution> findByExecutionId(String executionId) {
        return jdbcTemplate.query("""
                        SELECT execution_id, tenant_id, store_id, rule_id, action_type, trigger_source,
                               target_type, target_id, result, status, retry_count, cost_ms, error_message, executed_at
                        FROM automation_execution
                        WHERE execution_id = ?
                        """,
                ROW_MAPPER, executionId).stream().findFirst();
    }

    @Override
    public Optional<AutomationExecution> findExisting(String tenantId, String targetId, String ruleId, String actionType) {
        return jdbcTemplate.query("""
                        SELECT execution_id, tenant_id, store_id, rule_id, action_type, trigger_source,
                               target_type, target_id, result, status, retry_count, cost_ms, error_message, executed_at
                        FROM automation_execution
                        WHERE tenant_id = ? AND target_id = ? AND rule_id = ? AND action_type = ?
                        ORDER BY executed_at DESC, execution_id
                        """,
                ROW_MAPPER, tenantId, targetId, ruleId, actionType).stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM automation_execution");
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
