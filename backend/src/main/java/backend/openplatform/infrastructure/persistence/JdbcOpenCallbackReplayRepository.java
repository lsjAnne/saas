package backend.openplatform.infrastructure.persistence;

import backend.openplatform.domain.repository.OpenCallbackReplayRepository;
import backend.openplatform.model.OpenCallbackReplayRecord;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcOpenCallbackReplayRepository implements OpenCallbackReplayRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOpenCallbackReplayRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean exists(String subscriptionId, String requestId) {
        List<Integer> records = jdbcTemplate.query(
                """
                SELECT 1
                FROM open_callback_replay
                WHERE subscription_id = ? AND request_id = ?
                """,
                (rs, rowNum) -> rs.getInt(1),
                JdbcIdCodec.parseWebhookSubscriptionId(subscriptionId),
                requestId
        );
        return !records.isEmpty();
    }

    @Override
    public OpenCallbackReplayRecord save(OpenCallbackReplayRecord replayRecord) {
        jdbcTemplate.update(
                """
                INSERT INTO open_callback_replay (subscription_id, request_id, created_at)
                VALUES (?, ?, ?)
                """,
                JdbcIdCodec.parseWebhookSubscriptionId(replayRecord.subscriptionId()),
                replayRecord.requestId(),
                toTimestamp(replayRecord.createdAt())
        );
        return replayRecord;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM open_callback_replay");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }
}

