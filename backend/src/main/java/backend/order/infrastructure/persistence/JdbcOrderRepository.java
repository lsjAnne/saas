package backend.order.infrastructure.persistence;

import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
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
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OrderMain save(OrderMain orderMain) {
        if (orderMain.orderId() == null || orderMain.orderId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO order_main (
                            store_id, platform_order_id, order_status, logistics_status, total_amount, estimated_profit,
                            buyer_name, buyer_phone_mask, shipping_address, timeout_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(orderMain.storeId()));
                statement.setString(2, orderMain.platformOrderId());
                statement.setString(3, orderMain.orderStatus());
                statement.setString(4, orderMain.logisticsStatus());
                statement.setBigDecimal(5, orderMain.totalAmount());
                statement.setBigDecimal(6, orderMain.estimatedProfit());
                statement.setString(7, orderMain.buyerName());
                statement.setString(8, orderMain.buyerPhoneMask());
                statement.setString(9, orderMain.shippingAddress());
                statement.setTimestamp(10, toTimestamp(orderMain.timeoutAt()));
                statement.setTimestamp(11, toTimestamp(orderMain.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("order_main涓婚敭鐢熸垚澶辫触");
            }
            return new OrderMain(
                    JdbcIdCodec.formatOrderId(key.longValue()),
                    orderMain.storeId(),
                    orderMain.platformOrderId(),
                    orderMain.orderStatus(),
                    orderMain.logisticsStatus(),
                    orderMain.totalAmount(),
                    orderMain.estimatedProfit(),
                    orderMain.buyerName(),
                    orderMain.buyerPhoneMask(),
                    orderMain.shippingAddress(),
                    orderMain.timeoutAt(),
                    orderMain.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE order_main
                SET order_status = ?, logistics_status = ?, total_amount = ?, estimated_profit = ?,
                    buyer_name = ?, buyer_phone_mask = ?, shipping_address = ?, timeout_at = ?
                WHERE id = ?
                """,
                orderMain.orderStatus(),
                orderMain.logisticsStatus(),
                orderMain.totalAmount(),
                orderMain.estimatedProfit(),
                orderMain.buyerName(),
                orderMain.buyerPhoneMask(),
                orderMain.shippingAddress(),
                toTimestamp(orderMain.timeoutAt()),
                JdbcIdCodec.parseOrderId(orderMain.orderId())
        );
        return orderMain;
    }

    @Override
    public List<OrderMain> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, platform_order_id, order_status, logistics_status, total_amount, estimated_profit,
                       buyer_name, buyer_phone_mask, shipping_address, timeout_at, created_at
                FROM order_main
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapOrder(rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("platform_order_id"),
                        rs.getString("order_status"),
                        rs.getString("logistics_status"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("estimated_profit"),
                        rs.getString("buyer_name"),
                        rs.getString("buyer_phone_mask"),
                        rs.getString("shipping_address"),
                        rs.getTimestamp("timeout_at"),
                        rs.getTimestamp("created_at")),
                arguments
        );
    }

    @Override
    public Optional<OrderMain> findByOrderId(String orderId) {
        List<OrderMain> orders = jdbcTemplate.query(
                """
                SELECT id, store_id, platform_order_id, order_status, logistics_status, total_amount, estimated_profit,
                       buyer_name, buyer_phone_mask, shipping_address, timeout_at, created_at
                FROM order_main
                WHERE id = ?
                """,
                (rs, rowNum) -> mapOrder(rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("platform_order_id"),
                        rs.getString("order_status"),
                        rs.getString("logistics_status"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("estimated_profit"),
                        rs.getString("buyer_name"),
                        rs.getString("buyer_phone_mask"),
                        rs.getString("shipping_address"),
                        rs.getTimestamp("timeout_at"),
                        rs.getTimestamp("created_at")),
                JdbcIdCodec.parseOrderId(orderId)
        );
        return orders.stream().findFirst();
    }

    @Override
    public Optional<OrderMain> findByStoreAndPlatformOrderId(String storeId, String platformOrderId) {
        List<OrderMain> orders = jdbcTemplate.query(
                """
                SELECT id, store_id, platform_order_id, order_status, logistics_status, total_amount, estimated_profit,
                       buyer_name, buyer_phone_mask, shipping_address, timeout_at, created_at
                FROM order_main
                WHERE store_id = ? AND platform_order_id = ?
                """,
                (rs, rowNum) -> mapOrder(rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("platform_order_id"),
                        rs.getString("order_status"),
                        rs.getString("logistics_status"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("estimated_profit"),
                        rs.getString("buyer_name"),
                        rs.getString("buyer_phone_mask"),
                        rs.getString("shipping_address"),
                        rs.getTimestamp("timeout_at"),
                        rs.getTimestamp("created_at")),
                JdbcIdCodec.parseStoreId(storeId),
                platformOrderId
        );
        return orders.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM order_main");
    }

    private OrderMain mapOrder(long orderId,
                               long storeId,
                               String platformOrderId,
                               String orderStatus,
                               String logisticsStatus,
                               java.math.BigDecimal totalAmount,
                               java.math.BigDecimal estimatedProfit,
                               String buyerName,
                               String buyerPhoneMask,
                               String shippingAddress,
                               Timestamp timeoutAt,
                               Timestamp createdAt) {
        return new OrderMain(
                JdbcIdCodec.formatOrderId(orderId),
                JdbcIdCodec.formatStoreId(storeId),
                platformOrderId,
                orderStatus,
                logisticsStatus,
                totalAmount,
                estimatedProfit,
                buyerName,
                buyerPhoneMask,
                shippingAddress,
                toOffsetDateTime(timeoutAt),
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

