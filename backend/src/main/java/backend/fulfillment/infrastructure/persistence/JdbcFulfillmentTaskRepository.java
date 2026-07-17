package backend.fulfillment.infrastructure.persistence;

import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.fulfillment.model.FulfillmentTask;
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
public class JdbcFulfillmentTaskRepository implements FulfillmentTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFulfillmentTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FulfillmentTask save(FulfillmentTask fulfillmentTask) {
        if (fulfillmentTask.fulfillmentTaskId() == null || fulfillmentTask.fulfillmentTaskId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO fulfillment_task (
                            store_id, order_id, idempotency_key, status, retry_count, due_at, last_error_message, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(fulfillmentTask.storeId()));
                statement.setLong(2, JdbcIdCodec.parseOrderId(fulfillmentTask.orderId()));
                statement.setString(3, fulfillmentTask.idempotencyKey());
                statement.setString(4, fulfillmentTask.status());
                statement.setInt(5, fulfillmentTask.retryCount());
                statement.setTimestamp(6, toTimestamp(fulfillmentTask.dueAt()));
                statement.setString(7, fulfillmentTask.lastErrorMessage());
                statement.setTimestamp(8, toTimestamp(fulfillmentTask.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("fulfillment_task涓婚敭鐢熸垚澶辫触");
            }
            return new FulfillmentTask(
                    JdbcIdCodec.formatFulfillmentTaskId(key.longValue()),
                    fulfillmentTask.storeId(),
                    fulfillmentTask.orderId(),
                    fulfillmentTask.idempotencyKey(),
                    fulfillmentTask.status(),
                    fulfillmentTask.retryCount(),
                    fulfillmentTask.dueAt(),
                    fulfillmentTask.lastErrorMessage(),
                    fulfillmentTask.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE fulfillment_task
                SET status = ?, retry_count = ?, due_at = ?, last_error_message = ?
                WHERE id = ?
                """,
                fulfillmentTask.status(),
                fulfillmentTask.retryCount(),
                toTimestamp(fulfillmentTask.dueAt()),
                fulfillmentTask.lastErrorMessage(),
                JdbcIdCodec.parseFulfillmentTaskId(fulfillmentTask.fulfillmentTaskId())
        );
        return fulfillmentTask;
    }

    @Override
    public List<FulfillmentTask> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, order_id, idempotency_key, status, retry_count, due_at, last_error_message, created_at
                FROM fulfillment_task
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapTask(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("order_id"),
                        rs.getString("idempotency_key"),
                        rs.getString("status"),
                        rs.getInt("retry_count"),
                        rs.getTimestamp("due_at"),
                        rs.getString("last_error_message"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public List<FulfillmentTask> findByOrderId(String orderId) {
        return jdbcTemplate.query(
                """
                SELECT id, store_id, order_id, idempotency_key, status, retry_count, due_at, last_error_message, created_at
                FROM fulfillment_task
                WHERE order_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> mapTask(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("order_id"),
                        rs.getString("idempotency_key"),
                        rs.getString("status"),
                        rs.getInt("retry_count"),
                        rs.getTimestamp("due_at"),
                        rs.getString("last_error_message"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseOrderId(orderId)
        );
    }

    @Override
    public Optional<FulfillmentTask> findByFulfillmentTaskId(String fulfillmentTaskId) {
        List<FulfillmentTask> tasks = jdbcTemplate.query(
                """
                SELECT id, store_id, order_id, idempotency_key, status, retry_count, due_at, last_error_message, created_at
                FROM fulfillment_task
                WHERE id = ?
                """,
                (rs, rowNum) -> mapTask(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("order_id"),
                        rs.getString("idempotency_key"),
                        rs.getString("status"),
                        rs.getInt("retry_count"),
                        rs.getTimestamp("due_at"),
                        rs.getString("last_error_message"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseFulfillmentTaskId(fulfillmentTaskId)
        );
        return tasks.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM fulfillment_task");
    }

    private FulfillmentTask mapTask(long fulfillmentTaskId,
                                    long storeId,
                                    long orderId,
                                    String idempotencyKey,
                                    String status,
                                    int retryCount,
                                    Timestamp dueAt,
                                    String lastErrorMessage,
                                    Timestamp createdAt) {
        return new FulfillmentTask(
                JdbcIdCodec.formatFulfillmentTaskId(fulfillmentTaskId),
                JdbcIdCodec.formatStoreId(storeId),
                JdbcIdCodec.formatOrderId(orderId),
                idempotencyKey,
                status,
                retryCount,
                toOffsetDateTime(dueAt),
                lastErrorMessage,
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

