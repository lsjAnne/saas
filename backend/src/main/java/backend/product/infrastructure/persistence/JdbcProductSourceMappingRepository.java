package backend.product.infrastructure.persistence;

import backend.product.domain.repository.ProductSourceMappingRepository;
import backend.product.model.ProductSourceMapping;
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
public class JdbcProductSourceMappingRepository implements ProductSourceMappingRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductSourceMappingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProductSourceMapping save(ProductSourceMapping productSourceMapping) {
        if (productSourceMapping.productMappingId() == null || productSourceMapping.productMappingId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO product_source_mapping (
                            store_id, product_id, supplier_id, mapping_type, is_active, risk_flag, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(productSourceMapping.storeId()));
                statement.setLong(2, JdbcIdCodec.parseProductId(productSourceMapping.productId()));
                statement.setLong(3, JdbcIdCodec.parseSupplierId(productSourceMapping.supplierId()));
                statement.setString(4, productSourceMapping.mappingType());
                statement.setBoolean(5, productSourceMapping.active());
                statement.setBoolean(6, productSourceMapping.riskFlag());
                statement.setTimestamp(7, toTimestamp(productSourceMapping.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("product_source_mapping涓婚敭鐢熸垚澶辫触");
            }
            return new ProductSourceMapping(
                    JdbcIdCodec.formatProductMappingId(key.longValue()),
                    productSourceMapping.storeId(),
                    productSourceMapping.productId(),
                    productSourceMapping.supplierId(),
                    productSourceMapping.mappingType(),
                    productSourceMapping.active(),
                    productSourceMapping.riskFlag(),
                    productSourceMapping.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE product_source_mapping
                SET supplier_id = ?, mapping_type = ?, is_active = ?, risk_flag = ?
                WHERE id = ?
                """,
                JdbcIdCodec.parseSupplierId(productSourceMapping.supplierId()),
                productSourceMapping.mappingType(),
                productSourceMapping.active(),
                productSourceMapping.riskFlag(),
                JdbcIdCodec.parseProductMappingId(productSourceMapping.productMappingId())
        );
        return productSourceMapping;
    }

    @Override
    public List<ProductSourceMapping> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, supplier_id, mapping_type, is_active, risk_flag, created_at
                FROM product_source_mapping
                WHERE store_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> mapMapping(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("product_id"),
                        rs.getLong("supplier_id"),
                        rs.getString("mapping_type"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("risk_flag"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public List<ProductSourceMapping> findByProductId(String productId) {
        return jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, supplier_id, mapping_type, is_active, risk_flag, created_at
                FROM product_source_mapping
                WHERE product_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> mapMapping(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("product_id"),
                        rs.getLong("supplier_id"),
                        rs.getString("mapping_type"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("risk_flag"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseProductId(productId)
        );
    }

    @Override
    public Optional<ProductSourceMapping> findByProductMappingId(String productMappingId) {
        List<ProductSourceMapping> mappings = jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, supplier_id, mapping_type, is_active, risk_flag, created_at
                FROM product_source_mapping
                WHERE id = ?
                """,
                (rs, rowNum) -> mapMapping(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("product_id"),
                        rs.getLong("supplier_id"),
                        rs.getString("mapping_type"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("risk_flag"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseProductMappingId(productMappingId)
        );
        return mappings.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM product_source_mapping");
    }

    private ProductSourceMapping mapMapping(long productMappingId,
                                            long storeId,
                                            long productId,
                                            long supplierId,
                                            String mappingType,
                                            boolean active,
                                            boolean riskFlag,
                                            Timestamp createdAt) {
        return new ProductSourceMapping(
                JdbcIdCodec.formatProductMappingId(productMappingId),
                JdbcIdCodec.formatStoreId(storeId),
                JdbcIdCodec.formatProductId(productId),
                JdbcIdCodec.formatSupplierId(supplierId),
                mappingType,
                active,
                riskFlag,
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

