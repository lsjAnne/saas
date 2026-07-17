package backend.order.infrastructure.persistence;

import backend.order.domain.repository.OrderItemRepository;
import backend.order.model.OrderItem;
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

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcOrderItemRepository implements OrderItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        if (orderItem.orderItemId() == null || orderItem.orderItemId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO order_item (order_id, product_id, sku_id, quantity, unit_price, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseOrderId(orderItem.orderId()));
                statement.setLong(2, JdbcIdCodec.parseProductId(orderItem.productId()));
                statement.setLong(3, JdbcIdCodec.parseSkuId(orderItem.skuId()));
                statement.setInt(4, orderItem.quantity());
                statement.setBigDecimal(5, orderItem.unitPrice());
                statement.setTimestamp(6, toTimestamp(orderItem.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("order_item涓婚敭鐢熸垚澶辫触");
            }
            return new OrderItem(
                    JdbcIdCodec.formatOrderItemId(key.longValue()),
                    orderItem.orderId(),
                    orderItem.productId(),
                    orderItem.skuId(),
                    orderItem.quantity(),
                    orderItem.unitPrice(),
                    orderItem.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE order_item
                SET product_id = ?, sku_id = ?, quantity = ?, unit_price = ?
                WHERE id = ?
                """,
                JdbcIdCodec.parseProductId(orderItem.productId()),
                JdbcIdCodec.parseSkuId(orderItem.skuId()),
                orderItem.quantity(),
                orderItem.unitPrice(),
                JdbcIdCodec.parseOrderItemId(orderItem.orderItemId())
        );
        return orderItem;
    }

    @Override
    public List<OrderItem> findByOrderId(String orderId) {
        return jdbcTemplate.query(
                """
                SELECT id, order_id, product_id, sku_id, quantity, unit_price, created_at
                FROM order_item
                WHERE order_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new OrderItem(
                        JdbcIdCodec.formatOrderItemId(rs.getLong("id")),
                        JdbcIdCodec.formatOrderId(rs.getLong("order_id")),
                        JdbcIdCodec.formatProductId(rs.getLong("product_id")),
                        JdbcIdCodec.formatSkuId(rs.getLong("sku_id")),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseOrderId(orderId)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM order_item");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

