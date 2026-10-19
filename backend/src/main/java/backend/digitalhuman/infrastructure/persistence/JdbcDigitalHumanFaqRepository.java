package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanFaqRepository;
import backend.digitalhuman.model.DigitalHumanFaq;
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
public class JdbcDigitalHumanFaqRepository implements DigitalHumanFaqRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanFaqRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanFaq save(DigitalHumanFaq faq) {
        if (faq.faqId() == null || faq.faqId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_faq (
                            tenant_id, organization_id, product_id, product_name, question_type,
                            keywords, question, answer, enabled, hit_count, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(faq.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(faq.organizationId()));
                statement.setString(3, faq.productId());
                statement.setString(4, faq.productName());
                statement.setString(5, faq.questionType());
                statement.setString(6, faq.keywords());
                statement.setString(7, faq.question());
                statement.setString(8, faq.answer());
                statement.setShort(9, (short) (faq.enabled() ? 1 : 0));
                statement.setLong(10, faq.hitCount());
                statement.setTimestamp(11, toTimestamp(faq.createdAt()));
                statement.setTimestamp(12, toTimestamp(faq.updatedAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_faq primary key generation failed");
            }
            return new DigitalHumanFaq(
                    JdbcIdCodec.formatDigitalHumanFaqId(key.longValue()),
                    faq.tenantId(),
                    faq.organizationId(),
                    faq.productId(),
                    faq.productName(),
                    faq.questionType(),
                    faq.keywords(),
                    faq.question(),
                    faq.answer(),
                    faq.enabled(),
                    faq.hitCount(),
                    faq.createdAt(),
                    faq.updatedAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE digital_human_faq
                SET product_id = ?, product_name = ?, question_type = ?, keywords = ?,
                    question = ?, answer = ?, enabled = ?, hit_count = ?, updated_at = ?
                WHERE id = ?
                """,
                faq.productId(),
                faq.productName(),
                faq.questionType(),
                faq.keywords(),
                faq.question(),
                faq.answer(),
                faq.enabled() ? 1 : 0,
                faq.hitCount(),
                toTimestamp(faq.updatedAt()),
                JdbcIdCodec.parseDigitalHumanFaqId(faq.faqId())
        );
        return faq;
    }

    @Override
    public List<DigitalHumanFaq> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, product_id, product_name, question_type,
                       keywords, question, answer, enabled, hit_count, created_at, updated_at
                FROM digital_human_faq
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<DigitalHumanFaq> findById(String tenantId, String faqId) {
        List<DigitalHumanFaq> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, product_id, product_name, question_type,
                       keywords, question, answer, enabled, hit_count, created_at, updated_at
                FROM digital_human_faq
                WHERE tenant_id = ? AND id = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseDigitalHumanFaqId(faqId)
        );
        return result.stream().findFirst();
    }

    @Override
    public void deleteById(String tenantId, String faqId) {
        jdbcTemplate.update(
                "DELETE FROM digital_human_faq WHERE tenant_id = ? AND id = ?",
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseDigitalHumanFaqId(faqId)
        );
    }

    private DigitalHumanFaq mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanFaq(
                JdbcIdCodec.formatDigitalHumanFaqId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("product_id"),
                rs.getString("product_name"),
                rs.getString("question_type"),
                rs.getString("keywords"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getShort("enabled") == 1,
                rs.getLong("hit_count"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static Timestamp toTimestamp(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Timestamp.from(offsetDateTime.toInstant());
    }
}
