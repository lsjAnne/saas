package com.dianshang.platform.live.infrastructure.persistence;

import com.dianshang.platform.live.domain.repository.LiveProductItemRepository;
import com.dianshang.platform.live.model.LiveProductItem;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
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
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcLiveProductItemRepository implements LiveProductItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLiveProductItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LiveProductItem save(LiveProductItem liveProductItem) {
        if (liveProductItem.liveProductItemId() == null || liveProductItem.liveProductItemId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO live_product_item (
                            live_plan_id, candidate_product_id, product_draft_id, product_id, sort_order, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseLivePlanId(liveProductItem.livePlanId()));
                setNullableId(statement, 2, liveProductItem.candidateProductId(), JdbcIdCodec::parseCandidateProductId);
                setNullableId(statement, 3, liveProductItem.productDraftId(), JdbcIdCodec::parseProductDraftId);
                setNullableId(statement, 4, liveProductItem.productId(), JdbcIdCodec::parseProductId);
                statement.setInt(5, liveProductItem.sortOrder());
                statement.setTimestamp(6, toTimestamp(liveProductItem.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("live_product_item涓婚敭鐢熸垚澶辫触");
            }
            return new LiveProductItem(
                    JdbcIdCodec.formatLiveProductItemId(key.longValue()),
                    liveProductItem.livePlanId(),
                    liveProductItem.candidateProductId(),
                    liveProductItem.productDraftId(),
                    liveProductItem.productId(),
                    liveProductItem.sortOrder(),
                    liveProductItem.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE live_product_item
                SET candidate_product_id = ?, product_draft_id = ?, product_id = ?, sort_order = ?
                WHERE id = ?
                """,
                toNullableLong(liveProductItem.candidateProductId(), JdbcIdCodec::parseCandidateProductId),
                toNullableLong(liveProductItem.productDraftId(), JdbcIdCodec::parseProductDraftId),
                toNullableLong(liveProductItem.productId(), JdbcIdCodec::parseProductId),
                liveProductItem.sortOrder(),
                JdbcIdCodec.parseLiveProductItemId(liveProductItem.liveProductItemId())
        );
        return liveProductItem;
    }

    @Override
    public List<LiveProductItem> findByLivePlanId(String livePlanId) {
        return jdbcTemplate.query(
                """
                SELECT id, live_plan_id, candidate_product_id, product_draft_id, product_id, sort_order, created_at
                FROM live_product_item
                WHERE live_plan_id = ?
                ORDER BY sort_order ASC, created_at ASC, id ASC
                """,
                (rs, rowNum) -> new LiveProductItem(
                        JdbcIdCodec.formatLiveProductItemId(rs.getLong("id")),
                        JdbcIdCodec.formatLivePlanId(rs.getLong("live_plan_id")),
                        nullableId(rs.getLong("candidate_product_id"), JdbcIdCodec::formatCandidateProductId),
                        nullableId(rs.getLong("product_draft_id"), JdbcIdCodec::formatProductDraftId),
                        nullableId(rs.getLong("product_id"), JdbcIdCodec::formatProductId),
                        rs.getInt("sort_order"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseLivePlanId(livePlanId)
        );
    }

    @Override
    public Optional<LiveProductItem> findByLiveProductItemId(String liveProductItemId) {
        List<LiveProductItem> items = jdbcTemplate.query(
                """
                SELECT id, live_plan_id, candidate_product_id, product_draft_id, product_id, sort_order, created_at
                FROM live_product_item
                WHERE id = ?
                """,
                (rs, rowNum) -> new LiveProductItem(
                        JdbcIdCodec.formatLiveProductItemId(rs.getLong("id")),
                        JdbcIdCodec.formatLivePlanId(rs.getLong("live_plan_id")),
                        nullableId(rs.getLong("candidate_product_id"), JdbcIdCodec::formatCandidateProductId),
                        nullableId(rs.getLong("product_draft_id"), JdbcIdCodec::formatProductDraftId),
                        nullableId(rs.getLong("product_id"), JdbcIdCodec::formatProductId),
                        rs.getInt("sort_order"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseLiveProductItemId(liveProductItemId)
        );
        return items.stream().findFirst();
    }

    @Override
    public void deleteByLiveProductItemId(String liveProductItemId) {
        jdbcTemplate.update(
                "DELETE FROM live_product_item WHERE id = ?",
                JdbcIdCodec.parseLiveProductItemId(liveProductItemId)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM live_product_item");
    }

    private void setNullableId(PreparedStatement statement,
                               int index,
                               String value,
                               IdParser idParser) throws java.sql.SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, idParser.parse(value));
    }

    private Long toNullableLong(String value, IdParser idParser) {
        return value == null || value.isBlank() ? null : idParser.parse(value);
    }

    private String nullableId(long value, IdFormatter formatter) {
        return value == 0 ? null : formatter.format(value);
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    @FunctionalInterface
    private interface IdParser {
        long parse(String value);
    }

    @FunctionalInterface
    private interface IdFormatter {
        String format(long value);
    }
}
