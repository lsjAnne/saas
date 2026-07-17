package backend.inventory.infrastructure.persistence;

import backend.inventory.domain.repository.ReplenishmentTaskRepository;
import backend.inventory.model.ReplenishmentTask;
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
public class JdbcReplenishmentTaskRepository implements ReplenishmentTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReplenishmentTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ReplenishmentTask save(ReplenishmentTask replenishmentTask) {
        if (replenishmentTask.replenishmentTaskId() == null || replenishmentTask.replenishmentTaskId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO replenishment_task (
                            store_id, product_id, sku_id, suggested_qty, task_status, approval_status, reason_text, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(replenishmentTask.storeId()));
                statement.setLong(2, JdbcIdCodec.parseProductId(replenishmentTask.productId()));
                statement.setLong(3, JdbcIdCodec.parseSkuId(replenishmentTask.skuId()));
                statement.setInt(4, replenishmentTask.suggestedQty());
                statement.setString(5, replenishmentTask.taskStatus());
                statement.setString(6, replenishmentTask.approvalStatus());
                statement.setString(7, replenishmentTask.reasonText());
                statement.setTimestamp(8, toTimestamp(replenishmentTask.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("replenishment_task涓婚敭鐢熸垚澶辫触");
            }
            return new ReplenishmentTask(
                    JdbcIdCodec.formatReplenishmentTaskId(key.longValue()),
                    replenishmentTask.storeId(),
                    replenishmentTask.productId(),
                    replenishmentTask.skuId(),
                    replenishmentTask.suggestedQty(),
                    replenishmentTask.taskStatus(),
                    replenishmentTask.approvalStatus(),
                    replenishmentTask.reasonText(),
                    replenishmentTask.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE replenishment_task
                SET suggested_qty = ?, task_status = ?, approval_status = ?, reason_text = ?
                WHERE id = ?
                """,
                replenishmentTask.suggestedQty(),
                replenishmentTask.taskStatus(),
                replenishmentTask.approvalStatus(),
                replenishmentTask.reasonText(),
                JdbcIdCodec.parseReplenishmentTaskId(replenishmentTask.replenishmentTaskId())
        );
        return replenishmentTask;
    }

    @Override
    public List<ReplenishmentTask> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, sku_id, suggested_qty, task_status, approval_status, reason_text, created_at
                FROM replenishment_task
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapTask(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("product_id"),
                        rs.getLong("sku_id"),
                        rs.getInt("suggested_qty"),
                        rs.getString("task_status"),
                        rs.getString("approval_status"),
                        rs.getString("reason_text"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public Optional<ReplenishmentTask> findByReplenishmentTaskId(String replenishmentTaskId) {
        List<ReplenishmentTask> tasks = jdbcTemplate.query(
                """
                SELECT id, store_id, product_id, sku_id, suggested_qty, task_status, approval_status, reason_text, created_at
                FROM replenishment_task
                WHERE id = ?
                """,
                (rs, rowNum) -> mapTask(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("product_id"),
                        rs.getLong("sku_id"),
                        rs.getInt("suggested_qty"),
                        rs.getString("task_status"),
                        rs.getString("approval_status"),
                        rs.getString("reason_text"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseReplenishmentTaskId(replenishmentTaskId)
        );
        return tasks.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM replenishment_task");
    }

    private ReplenishmentTask mapTask(long replenishmentTaskId,
                                      long storeId,
                                      long productId,
                                      long skuId,
                                      int suggestedQty,
                                      String taskStatus,
                                      String approvalStatus,
                                      String reasonText,
                                      Timestamp createdAt) {
        return new ReplenishmentTask(
                JdbcIdCodec.formatReplenishmentTaskId(replenishmentTaskId),
                JdbcIdCodec.formatStoreId(storeId),
                JdbcIdCodec.formatProductId(productId),
                JdbcIdCodec.formatSkuId(skuId),
                suggestedQty,
                taskStatus,
                approvalStatus,
                reasonText,
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

