package backend.product.infrastructure.persistence;

import backend.product.domain.repository.ProductRepository;
import backend.product.model.Product;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcProductRepository implements ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Product save(Product product) {
        if (product.productId() == null || product.productId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO product (
                            store_id, platform_product_id, product_draft_id, title, status, health_score, published_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(product.storeId()));
                statement.setString(2, product.platformProductId());
                statement.setLong(3, JdbcIdCodec.parseProductDraftId(product.productDraftId()));
                statement.setString(4, product.title());
                statement.setString(5, product.status());
                if (product.healthScore() == null) {
                    statement.setNull(6, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(6, product.healthScore());
                }
                statement.setTimestamp(7, toTimestamp(product.publishedAt()));
                statement.setTimestamp(8, toTimestamp(product.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("product涓婚敭鐢熸垚澶辫触");
            }
            return new Product(
                    JdbcIdCodec.formatProductId(key.longValue()),
                    product.storeId(),
                    product.platformProductId(),
                    product.productDraftId(),
                    product.title(),
                    product.status(),
                    product.healthScore(),
                    product.publishedAt(),
                    product.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE product
                SET title = ?, status = ?, health_score = ?, published_at = ?
                WHERE id = ?
                """,
                product.title(),
                product.status(),
                product.healthScore(),
                toTimestamp(product.publishedAt()),
                JdbcIdCodec.parseProductId(product.productId())
        );
        return product;
    }

    @Override
    public List<Product> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, platform_product_id, product_draft_id, title, status, health_score, published_at, created_at
                FROM product
                WHERE store_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> new Product(
                        JdbcIdCodec.formatProductId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("platform_product_id"),
                        rs.getLong("product_draft_id") == 0 ? null : JdbcIdCodec.formatProductDraftId(rs.getLong("product_draft_id")),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getObject("health_score", Integer.class),
                        toOffsetDateTime(rs.getTimestamp("published_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                arguments
        );
    }

    @Override
    public Optional<Product> findByProductId(String productId) {
        List<Product> products = jdbcTemplate.query(
                """
                SELECT id, store_id, platform_product_id, product_draft_id, title, status, health_score, published_at, created_at
                FROM product
                WHERE id = ?
                """,
                (rs, rowNum) -> new Product(
                        JdbcIdCodec.formatProductId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("platform_product_id"),
                        rs.getLong("product_draft_id") == 0 ? null : JdbcIdCodec.formatProductDraftId(rs.getLong("product_draft_id")),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getObject("health_score", Integer.class),
                        toOffsetDateTime(rs.getTimestamp("published_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseProductId(productId)
        );
        return products.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM product");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

