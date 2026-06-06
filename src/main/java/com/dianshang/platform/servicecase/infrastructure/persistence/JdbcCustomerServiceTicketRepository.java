package com.dianshang.platform.servicecase.infrastructure.persistence;

import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import com.dianshang.platform.servicecase.domain.repository.CustomerServiceTicketRepository;
import com.dianshang.platform.servicecase.model.CustomerServiceTicket;
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
public class JdbcCustomerServiceTicketRepository implements CustomerServiceTicketRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCustomerServiceTicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CustomerServiceTicket save(CustomerServiceTicket customerServiceTicket) {
        if (customerServiceTicket.ticketId() == null || customerServiceTicket.ticketId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO customer_service_ticket (
                            store_id, order_id, customer_id, ticket_status, risk_flag, ai_reply_suggestion, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(customerServiceTicket.storeId()));
                statement.setLong(2, JdbcIdCodec.parseOrderId(customerServiceTicket.orderId()));
                statement.setString(3, customerServiceTicket.customerId());
                statement.setString(4, customerServiceTicket.ticketStatus());
                statement.setBoolean(5, customerServiceTicket.riskFlag());
                statement.setString(6, customerServiceTicket.aiReplySuggestion());
                statement.setTimestamp(7, toTimestamp(customerServiceTicket.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("customer_service_ticket主键生成失败");
            }
            return new CustomerServiceTicket(
                    JdbcIdCodec.formatTicketId(key.longValue()),
                    customerServiceTicket.storeId(),
                    customerServiceTicket.orderId(),
                    customerServiceTicket.customerId(),
                    customerServiceTicket.ticketStatus(),
                    customerServiceTicket.riskFlag(),
                    customerServiceTicket.aiReplySuggestion(),
                    customerServiceTicket.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE customer_service_ticket
                SET customer_id = ?, ticket_status = ?, risk_flag = ?, ai_reply_suggestion = ?
                WHERE id = ?
                """,
                customerServiceTicket.customerId(),
                customerServiceTicket.ticketStatus(),
                customerServiceTicket.riskFlag(),
                customerServiceTicket.aiReplySuggestion(),
                JdbcIdCodec.parseTicketId(customerServiceTicket.ticketId())
        );
        return customerServiceTicket;
    }

    @Override
    public List<CustomerServiceTicket> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, order_id, customer_id, ticket_status, risk_flag, ai_reply_suggestion, created_at
                FROM customer_service_ticket
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapTicket(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("order_id"),
                        rs.getString("customer_id"),
                        rs.getString("ticket_status"),
                        rs.getBoolean("risk_flag"),
                        rs.getString("ai_reply_suggestion"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public Optional<CustomerServiceTicket> findByTicketId(String ticketId) {
        List<CustomerServiceTicket> tickets = jdbcTemplate.query(
                """
                SELECT id, store_id, order_id, customer_id, ticket_status, risk_flag, ai_reply_suggestion, created_at
                FROM customer_service_ticket
                WHERE id = ?
                """,
                (rs, rowNum) -> mapTicket(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getLong("order_id"),
                        rs.getString("customer_id"),
                        rs.getString("ticket_status"),
                        rs.getBoolean("risk_flag"),
                        rs.getString("ai_reply_suggestion"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseTicketId(ticketId)
        );
        return tickets.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM customer_service_ticket");
    }

    private CustomerServiceTicket mapTicket(long ticketId,
                                            long storeId,
                                            long orderId,
                                            String customerId,
                                            String ticketStatus,
                                            boolean riskFlag,
                                            String aiReplySuggestion,
                                            Timestamp createdAt) {
        return new CustomerServiceTicket(
                JdbcIdCodec.formatTicketId(ticketId),
                JdbcIdCodec.formatStoreId(storeId),
                JdbcIdCodec.formatOrderId(orderId),
                customerId,
                ticketStatus,
                riskFlag,
                aiReplySuggestion,
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
