package backend.automation.infrastructure.persistence;

import backend.automation.domain.repository.AutomationActionRepository;
import backend.automation.model.AutomationAction;
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
public class JdbcAutomationActionRepository implements AutomationActionRepository {

    private static final RowMapper<AutomationAction> ROW_MAPPER = (rs, rowNum) -> new AutomationAction(
            rs.getString("action_id"),
            rs.getString("tenant_id"),
            rs.getString("store_id"),
            rs.getString("rule_id"),
            rs.getString("action_type"),
            rs.getString("action_config"),
            rs.getInt("priority"),
            rs.getBoolean("enabled"),
            toOffsetDateTime(rs.getTimestamp("created_at")),
            toOffsetDateTime(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcAutomationActionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AutomationAction save(AutomationAction action) {
        if (findByActionId(action.actionId()).isPresent()) {
            jdbcTemplate.update("""
                            UPDATE automation_action
                            SET tenant_id = ?, store_id = ?, rule_id = ?, action_type = ?, action_config = ?,
                                priority = ?, enabled = ?, updated_at = ?
                            WHERE action_id = ?
                            """,
                    action.tenantId(), action.storeId(), action.ruleId(), action.actionType(),
                    action.actionConfig(), action.priority(), action.enabled(),
                    toTimestamp(action.updatedAt()), action.actionId());
            return action;
        }
        jdbcTemplate.update("""
                        INSERT INTO automation_action (
                            action_id, tenant_id, store_id, rule_id, action_type, action_config,
                            priority, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                action.actionId(), action.tenantId(), action.storeId(), action.ruleId(), action.actionType(),
                action.actionConfig(), action.priority(), action.enabled(),
                toTimestamp(action.createdAt()), toTimestamp(action.updatedAt()));
        return action;
    }

    @Override
    public List<AutomationAction> findByStoreId(String storeId) {
        return jdbcTemplate.query("""
                        SELECT action_id, tenant_id, store_id, rule_id, action_type, action_config,
                               priority, enabled, created_at, updated_at
                        FROM automation_action
                        WHERE store_id = ?
                        ORDER BY priority, created_at, action_id
                        """,
                ROW_MAPPER, storeId);
    }

    @Override
    public Optional<AutomationAction> findByActionId(String actionId) {
        return jdbcTemplate.query("""
                        SELECT action_id, tenant_id, store_id, rule_id, action_type, action_config,
                               priority, enabled, created_at, updated_at
                        FROM automation_action
                        WHERE action_id = ?
                        """,
                ROW_MAPPER, actionId).stream().findFirst();
    }

    @Override
    public void deleteByActionId(String actionId) {
        jdbcTemplate.update("DELETE FROM automation_action WHERE action_id = ?", actionId);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM automation_action");
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
