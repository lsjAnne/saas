package backend.qa.infrastructure.persistence;

import backend.qa.domain.repository.CustomerConversationRepository;
import backend.qa.model.CustomerConversation;
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
public class JdbcCustomerConversationRepository implements CustomerConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCustomerConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CustomerConversation save(CustomerConversation customerConversation) {
        if (customerConversation.conversationId() == null || customerConversation.conversationId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO customer_conversation (
                            store_id, platform_type, platform_conversation_id, customer_id, conversation_status, risk_flag, last_message_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(customerConversation.storeId()));
                statement.setString(2, customerConversation.platformType());
                statement.setString(3, customerConversation.platformConversationId());
                statement.setString(4, customerConversation.customerId());
                statement.setString(5, customerConversation.conversationStatus());
                statement.setBoolean(6, customerConversation.riskFlag());
                statement.setTimestamp(7, toTimestamp(customerConversation.lastMessageAt()));
                statement.setTimestamp(8, toTimestamp(customerConversation.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("customer_conversation涓婚敭鐢熸垚澶辫触");
            }
            return new CustomerConversation(
                    JdbcIdCodec.formatConversationId(key.longValue()),
                    customerConversation.storeId(),
                    customerConversation.platformType(),
                    customerConversation.platformConversationId(),
                    customerConversation.customerId(),
                    customerConversation.conversationStatus(),
                    customerConversation.riskFlag(),
                    customerConversation.lastMessageAt(),
                    customerConversation.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE customer_conversation
                SET customer_id = ?, conversation_status = ?, risk_flag = ?, last_message_at = ?
                WHERE id = ?
                """,
                customerConversation.customerId(),
                customerConversation.conversationStatus(),
                customerConversation.riskFlag(),
                toTimestamp(customerConversation.lastMessageAt()),
                JdbcIdCodec.parseConversationId(customerConversation.conversationId())
        );
        return customerConversation;
    }

    @Override
    public List<CustomerConversation> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, platform_type, platform_conversation_id, customer_id, conversation_status, risk_flag, last_message_at, created_at
                FROM customer_conversation
                WHERE store_id IN (%s)
                ORDER BY last_message_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapConversation(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("platform_type"),
                        rs.getString("platform_conversation_id"),
                        rs.getString("customer_id"),
                        rs.getString("conversation_status"),
                        rs.getBoolean("risk_flag"),
                        rs.getTimestamp("last_message_at"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public Optional<CustomerConversation> findByConversationId(String conversationId) {
        List<CustomerConversation> conversations = jdbcTemplate.query(
                """
                SELECT id, store_id, platform_type, platform_conversation_id, customer_id, conversation_status, risk_flag, last_message_at, created_at
                FROM customer_conversation
                WHERE id = ?
                """,
                (rs, rowNum) -> mapConversation(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("platform_type"),
                        rs.getString("platform_conversation_id"),
                        rs.getString("customer_id"),
                        rs.getString("conversation_status"),
                        rs.getBoolean("risk_flag"),
                        rs.getTimestamp("last_message_at"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseConversationId(conversationId)
        );
        return conversations.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM customer_conversation");
    }

    private CustomerConversation mapConversation(long conversationId,
                                                 long storeId,
                                                 String platformType,
                                                 String platformConversationId,
                                                 String customerId,
                                                 String conversationStatus,
                                                 boolean riskFlag,
                                                 Timestamp lastMessageAt,
                                                 Timestamp createdAt) {
        return new CustomerConversation(
                JdbcIdCodec.formatConversationId(conversationId),
                JdbcIdCodec.formatStoreId(storeId),
                platformType,
                platformConversationId,
                customerId,
                conversationStatus,
                riskFlag,
                toOffsetDateTime(lastMessageAt),
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

