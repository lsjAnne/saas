package com.dianshang.platform.product.infrastructure.persistence;

import com.dianshang.platform.product.domain.repository.ProductDraftRepository;
import com.dianshang.platform.product.model.ProductDraft;
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
public class JdbcProductDraftRepository implements ProductDraftRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductDraftRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProductDraft save(ProductDraft productDraft) {
        if (productDraft.productDraftId() == null || productDraft.productDraftId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO product_draft (
                            store_id, candidate_product_id, title, selling_points, detail_content,
                            faq_content, suggested_price, status, ai_version, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(productDraft.storeId()));
                statement.setLong(2, JdbcIdCodec.parseCandidateProductId(productDraft.candidateProductId()));
                statement.setString(3, productDraft.title());
                statement.setString(4, productDraft.sellingPoints());
                statement.setString(5, productDraft.detailContent());
                statement.setString(6, productDraft.faqContent());
                statement.setBigDecimal(7, productDraft.suggestedPrice());
                statement.setString(8, productDraft.status());
                statement.setString(9, productDraft.aiVersion());
                statement.setTimestamp(10, toTimestamp(productDraft.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("product_draft主键生成失败");
            }
            return new ProductDraft(
                    JdbcIdCodec.formatProductDraftId(key.longValue()),
                    productDraft.storeId(),
                    productDraft.candidateProductId(),
                    productDraft.title(),
                    productDraft.sellingPoints(),
                    productDraft.detailContent(),
                    productDraft.faqContent(),
                    productDraft.suggestedPrice(),
                    productDraft.status(),
                    productDraft.aiVersion(),
                    productDraft.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE product_draft
                SET title = ?, selling_points = ?, detail_content = ?, faq_content = ?,
                    suggested_price = ?, status = ?
                WHERE id = ?
                """,
                productDraft.title(),
                productDraft.sellingPoints(),
                productDraft.detailContent(),
                productDraft.faqContent(),
                productDraft.suggestedPrice(),
                productDraft.status(),
                JdbcIdCodec.parseProductDraftId(productDraft.productDraftId())
        );
        return productDraft;
    }

    @Override
    public List<ProductDraft> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, candidate_product_id, title, selling_points, detail_content,
                       faq_content, suggested_price, status, ai_version, created_at
                FROM product_draft
                WHERE store_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> mapProductDraft(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("candidate_product_id"),
                        rs.getString("title"),
                        rs.getString("selling_points"),
                        rs.getString("detail_content"),
                        rs.getString("faq_content"),
                        rs.getBigDecimal("suggested_price"),
                        rs.getString("status"),
                        rs.getString("ai_version"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public Optional<ProductDraft> findByProductDraftId(String productDraftId) {
        List<ProductDraft> productDrafts = jdbcTemplate.query(
                """
                SELECT id, store_id, candidate_product_id, title, selling_points, detail_content,
                       faq_content, suggested_price, status, ai_version, created_at
                FROM product_draft
                WHERE id = ?
                """,
                (rs, rowNum) -> mapProductDraft(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("candidate_product_id"),
                        rs.getString("title"),
                        rs.getString("selling_points"),
                        rs.getString("detail_content"),
                        rs.getString("faq_content"),
                        rs.getBigDecimal("suggested_price"),
                        rs.getString("status"),
                        rs.getString("ai_version"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseProductDraftId(productDraftId)
        );
        return productDrafts.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM product_draft");
    }

    private ProductDraft mapProductDraft(long productDraftId,
                                         long storeId,
                                         long candidateProductId,
                                         String title,
                                         String sellingPoints,
                                         String detailContent,
                                         String faqContent,
                                         BigDecimal suggestedPrice,
                                         String status,
                                         String aiVersion,
                                         Timestamp createdAt) {
        return new ProductDraft(
                JdbcIdCodec.formatProductDraftId(productDraftId),
                JdbcIdCodec.formatStoreId(storeId),
                JdbcIdCodec.formatCandidateProductId(candidateProductId),
                title,
                sellingPoints,
                detailContent,
                faqContent,
                suggestedPrice,
                status,
                aiVersion,
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
