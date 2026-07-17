package backend.inventory.infrastructure.persistence;

import backend.inventory.domain.repository.InventorySnapshotRepository;
import backend.inventory.model.InventorySnapshot;
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
public class JdbcInventorySnapshotRepository implements InventorySnapshotRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcInventorySnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public InventorySnapshot save(InventorySnapshot inventorySnapshot) {
        if (inventorySnapshot.inventorySnapshotId() == null || inventorySnapshot.inventorySnapshotId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO inventory_snapshot (
                            store_id, product_id, sku_id, available_stock, reserved_stock, safety_stock, snapshot_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(inventorySnapshot.storeId()));
                statement.setLong(2, JdbcIdCodec.parseProductId(inventorySnapshot.productId()));
                statement.setLong(3, JdbcIdCodec.parseSkuId(inventorySnapshot.skuId()));
                statement.setInt(4, inventorySnapshot.availableStock());
                statement.setInt(5, inventorySnapshot.reservedStock());
                statement.setInt(6, inventorySnapshot.safetyStock());
                statement.setTimestamp(7, toTimestamp(inventorySnapshot.snapshotAt()));
                statement.setTimestamp(8, toTimestamp(inventorySnapshot.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("inventory_snapshot涓婚敭鐢熸垚澶辫触");
            }
            return new InventorySnapshot(
                    JdbcIdCodec.formatInventorySnapshotId(key.longValue()),
                    inventorySnapshot.storeId(),
                    inventorySnapshot.productId(),
                    inventorySnapshot.skuId(),
                    inventorySnapshot.availableStock(),
                    inventorySnapshot.reservedStock(),
                    inventorySnapshot.safetyStock(),
                    inventorySnapshot.snapshotAt(),
                    inventorySnapshot.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE inventory_snapshot
                SET available_stock = ?, reserved_stock = ?, safety_stock = ?, snapshot_at = ?
                WHERE id = ?
                """,
                inventorySnapshot.availableStock(),
                inventorySnapshot.reservedStock(),
                inventorySnapshot.safetyStock(),
                toTimestamp(inventorySnapshot.snapshotAt()),
                JdbcIdCodec.parseInventorySnapshotId(inventorySnapshot.inventorySnapshotId())
        );
        return inventorySnapshot;
    }

    @Override
    public List<InventorySnapshot> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, sku_id, available_stock, reserved_stock, safety_stock, snapshot_at, created_at
                FROM inventory_snapshot
                WHERE store_id IN (%s)
                ORDER BY snapshot_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapSnapshot(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("product_id"),
                        rs.getLong("sku_id"),
                        rs.getInt("available_stock"),
                        rs.getInt("reserved_stock"),
                        rs.getInt("safety_stock"),
                        rs.getTimestamp("snapshot_at"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public Optional<InventorySnapshot> findByInventorySnapshotId(String inventorySnapshotId) {
        List<InventorySnapshot> snapshots = jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, sku_id, available_stock, reserved_stock, safety_stock, snapshot_at, created_at
                FROM inventory_snapshot
                WHERE id = ?
                """,
                (rs, rowNum) -> mapSnapshot(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("product_id"),
                        rs.getLong("sku_id"),
                        rs.getInt("available_stock"),
                        rs.getInt("reserved_stock"),
                        rs.getInt("safety_stock"),
                        rs.getTimestamp("snapshot_at"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseInventorySnapshotId(inventorySnapshotId)
        );
        return snapshots.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM inventory_snapshot");
    }

    private InventorySnapshot mapSnapshot(long inventorySnapshotId,
                                          long storeId,
                                          long productId,
                                          long skuId,
                                          int availableStock,
                                          int reservedStock,
                                          int safetyStock,
                                          Timestamp snapshotAt,
                                          Timestamp createdAt) {
        return new InventorySnapshot(
                JdbcIdCodec.formatInventorySnapshotId(inventorySnapshotId),
                JdbcIdCodec.formatStoreId(storeId),
                JdbcIdCodec.formatProductId(productId),
                JdbcIdCodec.formatSkuId(skuId),
                availableStock,
                reservedStock,
                safetyStock,
                toOffsetDateTime(snapshotAt),
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

