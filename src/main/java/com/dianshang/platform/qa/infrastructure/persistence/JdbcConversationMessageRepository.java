package com.dianshang.platform.qa.infrastructure.persistence;

import com.dianshang.platform.qa.domain.repository.ConversationMessageRepository;
import com.dianshang.platform.qa.model.ConversationMessage;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
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
public class JdbcConversationMessageRepository implements ConversationMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcConversationMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ConversationMessage save(ConversationMessage conversationMessage) {
        if (conversationMessage.messageId() == null || conversationMessage.messageId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO conversation_message (
                            conversation_id, sender_type, message_type, content_text, ai_generated_flag, risk_flag, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseConversationId(conversationMessage.conversationId()));
                statement.setString(2, conversationMessage.senderType());
                statement.setString(3, conversationMessage.messageType());
                statement.setString(4, conversationMessage.contentText());
                statement.setBoolean(5, conversationMessage.aiGeneratedFlag());
                statement.setBoolean(6, conversationMessage.riskFlag());
                statement.setTimestamp(7, toTimestamp(conversationMessage.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("conversation_message主键生成失败");
            }
            return new ConversationMessage(
                    JdbcIdCodec.formatConversationMessageId(key.longValue()),
                    conversationMessage.conversationId(),
                    conversationMessage.senderType(),
                    conversationMessage.messageType(),
                    conversationMessage.contentText(),
                    conversationMessage.aiGeneratedFlag(),
                    conversationMessage.riskFlag(),
                    conversationMessage.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE conversation_message
                SET sender_type = ?, message_type = ?, content_text = ?, ai_generated_flag = ?, risk_flag = ?
                WHERE id = ?
                """,
                conversationMessage.senderType(),
                conversationMessage.messageType(),
                conversationMessage.contentText(),
                conversationMessage.aiGeneratedFlag(),
                conversationMessage.riskFlag(),
                JdbcIdCodec.parseConversationMessageId(conversationMessage.messageId())
        );
        return conversationMessage;
    }

    @Override
    public List<ConversationMessage> findByConversationId(String conversationId) {
        return jdbcTemplate.query(
                """
                SELECT id, conversation_id, sender_type, message_type, content_text, ai_generated_flag, risk_flag, created_at
                FROM conversation_message
                WHERE conversation_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new ConversationMessage(
                        JdbcIdCodec.formatConversationMessageId(rs.getLong("id")),
                        JdbcIdCodec.formatConversationId(rs.getLong("conversation_id")),
                        rs.getString("sender_type"),
                        rs.getString("message_type"),
                        rs.getString("content_text"),
                        rs.getBoolean("ai_generated_flag"),
                        rs.getBoolean("risk_flag"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseConversationId(conversationId)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM conversation_message");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
