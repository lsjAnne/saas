package com.dianshang.platform.product.infrastructure.persistence;

import com.dianshang.platform.product.domain.repository.CandidateProductRepository;
import com.dianshang.platform.product.model.CandidateProduct;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcCandidateProductRepository implements CandidateProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCandidateProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CandidateProduct save(CandidateProduct candidateProduct) {
        if (candidateProduct.candidateProductId() == null || candidateProduct.candidateProductId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO candidate_product (
                            store_id, source_type, source_url, source_url_hash, title, category, status,
                            estimated_profit, risk_level, recommendation_reason, ai_summary, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(candidateProduct.storeId()));
                statement.setString(2, candidateProduct.sourceType());
                statement.setString(3, candidateProduct.sourceUrl());
                statement.setString(4, candidateProduct.sourceUrlHash());
                statement.setString(5, candidateProduct.title());
                statement.setString(6, candidateProduct.category());
                statement.setString(7, candidateProduct.status());
                statement.setBigDecimal(8, candidateProduct.estimatedProfit());
                statement.setString(9, candidateProduct.riskLevel());
                statement.setString(10, candidateProduct.recommendationReason());
                statement.setString(11, candidateProduct.aiSummary());
                statement.setTimestamp(12, toTimestamp(candidateProduct.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("candidate_product主键生成失败");
            }
            return new CandidateProduct(
                    JdbcIdCodec.formatCandidateProductId(key.longValue()),
                    candidateProduct.storeId(),
                    candidateProduct.sourceType(),
                    candidateProduct.sourceUrl(),
                    candidateProduct.sourceUrlHash(),
                    candidateProduct.title(),
                    candidateProduct.category(),
                    candidateProduct.status(),
                    candidateProduct.estimatedProfit(),
                    candidateProduct.riskLevel(),
                    candidateProduct.recommendationReason(),
                    candidateProduct.aiSummary(),
                    candidateProduct.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE candidate_product
                SET source_url = ?, source_url_hash = ?, title = ?, category = ?, status = ?,
                    estimated_profit = ?, risk_level = ?, recommendation_reason = ?, ai_summary = ?
                WHERE id = ?
                """,
                candidateProduct.sourceUrl(),
                candidateProduct.sourceUrlHash(),
                candidateProduct.title(),
                candidateProduct.category(),
                candidateProduct.status(),
                candidateProduct.estimatedProfit(),
                candidateProduct.riskLevel(),
                candidateProduct.recommendationReason(),
                candidateProduct.aiSummary(),
                JdbcIdCodec.parseCandidateProductId(candidateProduct.candidateProductId())
        );
        return candidateProduct;
    }

    @Override
    public List<CandidateProduct> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, source_type, source_url, source_url_hash, title, category, status,
                       estimated_profit, risk_level, recommendation_reason, ai_summary, created_at
                FROM candidate_product
                WHERE store_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> mapCandidateProduct(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("source_type"),
                        rs.getString("source_url"),
                        rs.getString("source_url_hash"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("status"),
                        rs.getBigDecimal("estimated_profit"),
                        rs.getString("risk_level"),
                        rs.getString("recommendation_reason"),
                        rs.getString("ai_summary"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public Optional<CandidateProduct> findByCandidateProductId(String candidateProductId) {
        List<CandidateProduct> candidateProducts = jdbcTemplate.query(
                """
                SELECT id, store_id, source_type, source_url, source_url_hash, title, category, status,
                       estimated_profit, risk_level, recommendation_reason, ai_summary, created_at
                FROM candidate_product
                WHERE id = ?
                """,
                (rs, rowNum) -> mapCandidateProduct(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("source_type"),
                        rs.getString("source_url"),
                        rs.getString("source_url_hash"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("status"),
                        rs.getBigDecimal("estimated_profit"),
                        rs.getString("risk_level"),
                        rs.getString("recommendation_reason"),
                        rs.getString("ai_summary"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseCandidateProductId(candidateProductId)
        );
        return candidateProducts.stream().findFirst();
    }

    @Override
    public Optional<CandidateProduct> findByStoreAndSourceUrlHash(String storeId, String sourceUrlHash) {
        List<CandidateProduct> candidateProducts = jdbcTemplate.query(
                """
                SELECT id, store_id, source_type, source_url, source_url_hash, title, category, status,
                       estimated_profit, risk_level, recommendation_reason, ai_summary, created_at
                FROM candidate_product
                WHERE store_id = ? AND source_url_hash = ?
                """,
                (rs, rowNum) -> mapCandidateProduct(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("source_type"),
                        rs.getString("source_url"),
                        rs.getString("source_url_hash"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("status"),
                        rs.getBigDecimal("estimated_profit"),
                        rs.getString("risk_level"),
                        rs.getString("recommendation_reason"),
                        rs.getString("ai_summary"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseStoreId(storeId),
                sourceUrlHash
        );
        return candidateProducts.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM candidate_product");
    }

    private CandidateProduct mapCandidateProduct(long candidateProductId,
                                                 long storeId,
                                                 String sourceType,
                                                 String sourceUrl,
                                                 String sourceUrlHash,
                                                 String title,
                                                 String category,
                                                 String status,
                                                 BigDecimal estimatedProfit,
                                                 String riskLevel,
                                                 String recommendationReason,
                                                 String aiSummary,
                                                 Timestamp createdAt) {
        return new CandidateProduct(
                JdbcIdCodec.formatCandidateProductId(candidateProductId),
                JdbcIdCodec.formatStoreId(storeId),
                sourceType,
                sourceUrl,
                sourceUrlHash,
                title,
                category,
                status,
                estimatedProfit,
                riskLevel,
                recommendationReason,
                aiSummary,
                toOffsetDateTime(createdAt)
        );
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
