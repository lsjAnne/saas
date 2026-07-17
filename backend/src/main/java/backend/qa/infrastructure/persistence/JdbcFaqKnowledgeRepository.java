package backend.qa.infrastructure.persistence;

import backend.qa.domain.repository.FaqKnowledgeRepository;
import backend.qa.model.FaqKnowledge;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcFaqKnowledgeRepository implements FaqKnowledgeRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFaqKnowledgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FaqKnowledge save(FaqKnowledge faqKnowledge) {
        if (faqKnowledge.faqId() == null || faqKnowledge.faqId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO faq_knowledge (
                            store_id, product_id, question, answer, source_type, is_enabled, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(faqKnowledge.storeId()));
                if (faqKnowledge.productId() == null || faqKnowledge.productId().isBlank()) {
                    statement.setNull(2, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(2, JdbcIdCodec.parseProductId(faqKnowledge.productId()));
                }
                statement.setString(3, faqKnowledge.question());
                statement.setString(4, faqKnowledge.answer());
                statement.setString(5, faqKnowledge.sourceType());
                statement.setBoolean(6, faqKnowledge.enabled());
                statement.setTimestamp(7, toTimestamp(faqKnowledge.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("faq_knowledge涓婚敭鐢熸垚澶辫触");
            }
            return new FaqKnowledge(
                    JdbcIdCodec.formatFaqId(key.longValue()),
                    faqKnowledge.storeId(),
                    faqKnowledge.productId(),
                    faqKnowledge.question(),
                    faqKnowledge.answer(),
                    faqKnowledge.sourceType(),
                    faqKnowledge.enabled(),
                    faqKnowledge.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE faq_knowledge
                SET product_id = ?, question = ?, answer = ?, source_type = ?, is_enabled = ?
                WHERE id = ?
                """,
                faqKnowledge.productId() == null || faqKnowledge.productId().isBlank()
                        ? null : JdbcIdCodec.parseProductId(faqKnowledge.productId()),
                faqKnowledge.question(),
                faqKnowledge.answer(),
                faqKnowledge.sourceType(),
                faqKnowledge.enabled(),
                JdbcIdCodec.parseFaqId(faqKnowledge.faqId())
        );
        return faqKnowledge;
    }

    @Override
    public List<FaqKnowledge> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, question, answer, source_type, is_enabled, created_at
                FROM faq_knowledge
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new FaqKnowledge(
                        JdbcIdCodec.formatFaqId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getLong("product_id") == 0 ? null : JdbcIdCodec.formatProductId(rs.getLong("product_id")),
                        rs.getString("question"),
                        rs.getString("answer"),
                        rs.getString("source_type"),
                        rs.getBoolean("is_enabled"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                arguments
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM faq_knowledge");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

