package com.dianshang.platform.rule.infrastructure.persistence;

import com.dianshang.platform.rule.domain.repository.AutomationRuleRepository;
import com.dianshang.platform.rule.model.AutomationRule;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcAutomationRuleRepository implements AutomationRuleRepository {

    private static final RowMapper<AutomationRule> ROW_MAPPER = (rs, rowNum) -> new AutomationRule(
            JdbcIdCodec.formatRuleId(rs.getLong("id")),
            JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
            rs.getString("rule_type"),
            rs.getString("rule_category"),
            rs.getString("risk_category"),
            rs.getString("rule_name"),
            rs.getString("rule_expression"),
            rs.getBoolean("is_enabled"),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcAutomationRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AutomationRule save(AutomationRule automationRule) {
        if (automationRule.ruleId() == null || automationRule.ruleId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO automation_rule (
                            store_id, rule_type, rule_category, risk_category, rule_name, rule_expression, is_enabled, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, new String[]{"id"});
                statement.setLong(1, JdbcIdCodec.parseStoreId(automationRule.storeId()));
                statement.setString(2, automationRule.ruleType());
                statement.setString(3, automationRule.ruleCategory());
                statement.setString(4, automationRule.riskCategory());
                statement.setString(5, automationRule.ruleName());
                statement.setString(6, automationRule.ruleExpression());
                statement.setBoolean(7, automationRule.enabled());
                statement.setTimestamp(8, toTimestamp(automationRule.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("automation_rule primary key generation failed");
            }
            return new AutomationRule(
                    JdbcIdCodec.formatRuleId(key.longValue()),
                    automationRule.storeId(),
                    automationRule.ruleType(),
                    automationRule.ruleCategory(),
                    automationRule.riskCategory(),
                    automationRule.ruleName(),
                    automationRule.ruleExpression(),
                    automationRule.enabled(),
                    automationRule.createdAt()
            );
        }
        jdbcTemplate.update("""
                        UPDATE automation_rule
                        SET store_id = ?, rule_type = ?, rule_category = ?, risk_category = ?, rule_name = ?, rule_expression = ?, is_enabled = ?
                        WHERE id = ?
                        """,
                JdbcIdCodec.parseStoreId(automationRule.storeId()),
                automationRule.ruleType(),
                automationRule.ruleCategory(),
                automationRule.riskCategory(),
                automationRule.ruleName(),
                automationRule.ruleExpression(),
                automationRule.enabled(),
                JdbcIdCodec.parseRuleId(automationRule.ruleId())
        );
        return automationRule;
    }

    @Override
    public List<AutomationRule> findByStoreIds(List<String> storeIds) {
        if (storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(storeIds.size(), "?"));
        Object[] args = storeIds.stream()
                .map(JdbcIdCodec::parseStoreId)
                .toArray();
        return jdbcTemplate.query("""
                        SELECT id, store_id, rule_type, rule_category, risk_category, rule_name, rule_expression, is_enabled, created_at
                        FROM automation_rule
                        WHERE store_id IN (%s)
                        ORDER BY created_at, id
                        """.formatted(placeholders),
                ROW_MAPPER,
                args
        );
    }

    @Override
    public Optional<AutomationRule> findByRuleId(String ruleId) {
        List<AutomationRule> rules = jdbcTemplate.query("""
                        SELECT id, store_id, rule_type, rule_category, risk_category, rule_name, rule_expression, is_enabled, created_at
                        FROM automation_rule
                        WHERE id = ?
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseRuleId(ruleId)
        );
        return rules.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM automation_rule");
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
