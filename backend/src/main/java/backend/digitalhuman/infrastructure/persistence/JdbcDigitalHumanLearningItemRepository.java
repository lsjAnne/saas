package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanLearningItemRepository;
import backend.digitalhuman.model.DigitalHumanLearningItem;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcDigitalHumanLearningItemRepository implements DigitalHumanLearningItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanLearningItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanLearningItem save(DigitalHumanLearningItem item) {
        if (item.itemId() == null || item.itemId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_learning_item (
                            tenant_id, organization_id, session_id, question, question_type,
                            product_name, occurrence_count, status, faq_id, last_seen_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(item.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(item.organizationId()));
                statement.setString(3, item.sessionId());
                statement.setString(4, item.question());
                statement.setString(5, item.questionType());
                statement.setString(6, item.productName());
                statement.setLong(7, item.occurrenceCount());
                statement.setString(8, item.status());
                statement.setString(9, item.faqId());
                statement.setTimestamp(10, toTimestamp(item.lastSeenAt()));
                statement.setTimestamp(11, toTimestamp(item.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_learning_item primary key generation failed");
            }
            return new DigitalHumanLearningItem(
                    JdbcIdCodec.formatDigitalHumanLearningItemId(key.longValue()),
                    item.tenantId(),
                    item.organizationId(),
                    item.sessionId(),
                    item.question(),
                    item.questionType(),
                    item.productName(),
                    item.occurrenceCount(),
                    item.status(),
                    item.faqId(),
                    item.lastSeenAt(),
                    item.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE digital_human_learning_item
                SET occurrence_count = ?, status = ?, faq_id = ?, last_seen_at = ?
                WHERE id = ?
                """,
                item.occurrenceCount(),
                item.status(),
                item.faqId(),
                toTimestamp(item.lastSeenAt()),
                JdbcIdCodec.parseDigitalHumanLearningItemId(item.itemId())
        );
        return item;
    }

    @Override
    public List<DigitalHumanLearningItem> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, session_id, question, question_type,
                       product_name, occurrence_count, status, faq_id, last_seen_at, created_at
                FROM digital_human_learning_item
                WHERE tenant_id = ?
                ORDER BY last_seen_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<DigitalHumanLearningItem> findById(String tenantId, String itemId) {
        List<DigitalHumanLearningItem> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, session_id, question, question_type,
                       product_name, occurrence_count, status, faq_id, last_seen_at, created_at
                FROM digital_human_learning_item
                WHERE tenant_id = ? AND id = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseDigitalHumanLearningItemId(itemId)
        );
        return result.stream().findFirst();
    }

    @Override
    public void deleteById(String tenantId, String itemId) {
        jdbcTemplate.update(
                "DELETE FROM digital_human_learning_item WHERE tenant_id = ? AND id = ?",
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseDigitalHumanLearningItemId(itemId)
        );
    }

    private DigitalHumanLearningItem mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanLearningItem(
                JdbcIdCodec.formatDigitalHumanLearningItemId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("session_id"),
                rs.getString("question"),
                rs.getString("question_type"),
                rs.getString("product_name"),
                rs.getLong("occurrence_count"),
                rs.getString("status"),
                rs.getString("faq_id"),
                toOffsetDateTime(rs.getTimestamp("last_seen_at")),
                toOffsetDateTime(rs.getTimestamp("created_at"))
        );
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static Timestamp toTimestamp(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Timestamp.from(offsetDateTime.toInstant());
    }
}
