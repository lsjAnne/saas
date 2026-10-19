package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanInteractionRepository;
import backend.digitalhuman.model.DigitalHumanInteraction;
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

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcDigitalHumanInteractionRepository implements DigitalHumanInteractionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanInteractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanInteraction save(DigitalHumanInteraction interaction) {
        if (interaction.interactionId() == null || interaction.interactionId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_interaction (
                            tenant_id, organization_id, session_id, barrage_id,
                            hit_type, faq_id, question, answer, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(interaction.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(interaction.organizationId()));
                statement.setString(3, interaction.sessionId());
                statement.setString(4, interaction.barrageId());
                statement.setString(5, interaction.hitType());
                statement.setString(6, interaction.faqId());
                statement.setString(7, interaction.question());
                statement.setString(8, interaction.answer());
                statement.setTimestamp(9, toTimestamp(interaction.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_interaction primary key generation failed");
            }
            return new DigitalHumanInteraction(
                    JdbcIdCodec.formatDigitalHumanInteractionId(key.longValue()),
                    interaction.tenantId(),
                    interaction.organizationId(),
                    interaction.sessionId(),
                    interaction.barrageId(),
                    interaction.hitType(),
                    interaction.faqId(),
                    interaction.question(),
                    interaction.answer(),
                    interaction.createdAt()
            );
        }
        return interaction;
    }

    @Override
    public List<DigitalHumanInteraction> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, session_id, barrage_id,
                       hit_type, faq_id, question, answer, created_at
                FROM digital_human_interaction
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public List<DigitalHumanInteraction> listBySession(String tenantId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return listByTenant(tenantId);
        }
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, session_id, barrage_id,
                       hit_type, faq_id, question, answer, created_at
                FROM digital_human_interaction
                WHERE tenant_id = ? AND session_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                sessionId
        );
    }

    private DigitalHumanInteraction mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanInteraction(
                JdbcIdCodec.formatDigitalHumanInteractionId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("session_id"),
                rs.getString("barrage_id"),
                rs.getString("hit_type"),
                rs.getString("faq_id"),
                rs.getString("question"),
                rs.getString("answer"),
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
