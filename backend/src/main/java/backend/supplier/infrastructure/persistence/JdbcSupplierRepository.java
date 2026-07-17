package backend.supplier.infrastructure.persistence;

import backend.saas.infrastructure.persistence.JdbcIdCodec;
import backend.supplier.domain.repository.SupplierRepository;
import backend.supplier.model.Supplier;
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
public class JdbcSupplierRepository implements SupplierRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSupplierRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Supplier save(Supplier supplier) {
        if (supplier.supplierId() == null || supplier.supplierId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO supplier (
                            store_id, supplier_platform_type, supplier_platform_id, supplier_name, source_url,
                            price_score, delivery_score, stability_score, risk_level, dropship_support_flag,
                            is_primary, is_backup, blacklist_flag, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(supplier.storeId()));
                statement.setString(2, supplier.supplierPlatformType());
                statement.setString(3, supplier.supplierPlatformId());
                statement.setString(4, supplier.supplierName());
                statement.setString(5, supplier.sourceUrl());
                setNullableInt(statement, 6, supplier.priceScore());
                setNullableInt(statement, 7, supplier.deliveryScore());
                setNullableInt(statement, 8, supplier.stabilityScore());
                statement.setString(9, supplier.riskLevel());
                statement.setBoolean(10, supplier.dropshipSupportFlag());
                statement.setBoolean(11, supplier.primary());
                statement.setBoolean(12, supplier.backup());
                statement.setBoolean(13, supplier.blacklistFlag());
                statement.setTimestamp(14, toTimestamp(supplier.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("supplier涓婚敭鐢熸垚澶辫触");
            }
            return new Supplier(
                    JdbcIdCodec.formatSupplierId(key.longValue()),
                    supplier.storeId(),
                    supplier.supplierPlatformType(),
                    supplier.supplierPlatformId(),
                    supplier.supplierName(),
                    supplier.sourceUrl(),
                    supplier.priceScore(),
                    supplier.deliveryScore(),
                    supplier.stabilityScore(),
                    supplier.riskLevel(),
                    supplier.dropshipSupportFlag(),
                    supplier.primary(),
                    supplier.backup(),
                    supplier.blacklistFlag(),
                    supplier.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE supplier
                SET supplier_name = ?, source_url = ?, price_score = ?, delivery_score = ?, stability_score = ?,
                    risk_level = ?, dropship_support_flag = ?, is_primary = ?, is_backup = ?, blacklist_flag = ?
                WHERE id = ?
                """,
                supplier.supplierName(),
                supplier.sourceUrl(),
                supplier.priceScore(),
                supplier.deliveryScore(),
                supplier.stabilityScore(),
                supplier.riskLevel(),
                supplier.dropshipSupportFlag(),
                supplier.primary(),
                supplier.backup(),
                supplier.blacklistFlag(),
                JdbcIdCodec.parseSupplierId(supplier.supplierId())
        );
        return supplier;
    }

    @Override
    public List<Supplier> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, supplier_platform_type, supplier_platform_id, supplier_name, source_url,
                       price_score, delivery_score, stability_score, risk_level, dropship_support_flag,
                       is_primary, is_backup, blacklist_flag, created_at
                FROM supplier
                WHERE store_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> mapSupplier(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("supplier_platform_type"),
                        rs.getString("supplier_platform_id"),
                        rs.getString("supplier_name"),
                        rs.getString("source_url"),
                        rs.getObject("price_score", Integer.class),
                        rs.getObject("delivery_score", Integer.class),
                        rs.getObject("stability_score", Integer.class),
                        rs.getString("risk_level"),
                        rs.getBoolean("dropship_support_flag"),
                        rs.getBoolean("is_primary"),
                        rs.getBoolean("is_backup"),
                        rs.getBoolean("blacklist_flag"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public List<Supplier> findByStoreId(String storeId) {
        return jdbcTemplate.query(
                """
                SELECT id, store_id, supplier_platform_type, supplier_platform_id, supplier_name, source_url,
                       price_score, delivery_score, stability_score, risk_level, dropship_support_flag,
                       is_primary, is_backup, blacklist_flag, created_at
                FROM supplier
                WHERE store_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> mapSupplier(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("supplier_platform_type"),
                        rs.getString("supplier_platform_id"),
                        rs.getString("supplier_name"),
                        rs.getString("source_url"),
                        rs.getObject("price_score", Integer.class),
                        rs.getObject("delivery_score", Integer.class),
                        rs.getObject("stability_score", Integer.class),
                        rs.getString("risk_level"),
                        rs.getBoolean("dropship_support_flag"),
                        rs.getBoolean("is_primary"),
                        rs.getBoolean("is_backup"),
                        rs.getBoolean("blacklist_flag"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseStoreId(storeId)
        );
    }

    @Override
    public Optional<Supplier> findBySupplierId(String supplierId) {
        List<Supplier> suppliers = jdbcTemplate.query(
                """
                SELECT id, store_id, supplier_platform_type, supplier_platform_id, supplier_name, source_url,
                       price_score, delivery_score, stability_score, risk_level, dropship_support_flag,
                       is_primary, is_backup, blacklist_flag, created_at
                FROM supplier
                WHERE id = ?
                """,
                (rs, rowNum) -> mapSupplier(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("supplier_platform_type"),
                        rs.getString("supplier_platform_id"),
                        rs.getString("supplier_name"),
                        rs.getString("source_url"),
                        rs.getObject("price_score", Integer.class),
                        rs.getObject("delivery_score", Integer.class),
                        rs.getObject("stability_score", Integer.class),
                        rs.getString("risk_level"),
                        rs.getBoolean("dropship_support_flag"),
                        rs.getBoolean("is_primary"),
                        rs.getBoolean("is_backup"),
                        rs.getBoolean("blacklist_flag"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseSupplierId(supplierId)
        );
        return suppliers.stream().findFirst();
    }

    @Override
    public Optional<Supplier> findByStoreAndPlatformSupplier(String storeId, String supplierPlatformType, String supplierPlatformId) {
        List<Supplier> suppliers = jdbcTemplate.query(
                """
                SELECT id, store_id, supplier_platform_type, supplier_platform_id, supplier_name, source_url,
                       price_score, delivery_score, stability_score, risk_level, dropship_support_flag,
                       is_primary, is_backup, blacklist_flag, created_at
                FROM supplier
                WHERE store_id = ? AND supplier_platform_type = ? AND supplier_platform_id = ?
                """,
                (rs, rowNum) -> mapSupplier(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("supplier_platform_type"),
                        rs.getString("supplier_platform_id"),
                        rs.getString("supplier_name"),
                        rs.getString("source_url"),
                        rs.getObject("price_score", Integer.class),
                        rs.getObject("delivery_score", Integer.class),
                        rs.getObject("stability_score", Integer.class),
                        rs.getString("risk_level"),
                        rs.getBoolean("dropship_support_flag"),
                        rs.getBoolean("is_primary"),
                        rs.getBoolean("is_backup"),
                        rs.getBoolean("blacklist_flag"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseStoreId(storeId),
                supplierPlatformType,
                supplierPlatformId
        );
        return suppliers.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM supplier");
    }

    private Supplier mapSupplier(long supplierId,
                                 long storeId,
                                 String supplierPlatformType,
                                 String supplierPlatformId,
                                 String supplierName,
                                 String sourceUrl,
                                 Integer priceScore,
                                 Integer deliveryScore,
                                 Integer stabilityScore,
                                 String riskLevel,
                                 boolean dropshipSupportFlag,
                                 boolean primary,
                                 boolean backup,
                                 boolean blacklistFlag,
                                 Timestamp createdAt) {
        return new Supplier(
                JdbcIdCodec.formatSupplierId(supplierId),
                JdbcIdCodec.formatStoreId(storeId),
                supplierPlatformType,
                supplierPlatformId,
                supplierName,
                sourceUrl,
                priceScore,
                deliveryScore,
                stabilityScore,
                riskLevel,
                dropshipSupportFlag,
                primary,
                backup,
                blacklistFlag,
                toOffsetDateTime(createdAt)
        );
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

