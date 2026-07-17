package backend.notification.infrastructure.persistence;

import backend.notification.domain.repository.NotificationTaskRepository;
import backend.notification.model.NotificationTask;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcNotificationTaskRepository implements NotificationTaskRepository {

    private static final RowMapper<NotificationTask> ROW_MAPPER = (rs, rowNum) -> new NotificationTask(
            JdbcIdCodec.formatNotificationTaskId(rs.getLong("id")),
            JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
            rs.getString("notify_type"),
            rs.getString("template_code"),
            rs.getString("target_receiver"),
            rs.getString("send_status"),
            rs.getInt("retry_count"),
            rs.getString("payload_json"),
            extractTextMeta(rs.getString("payload_json"), "priority", "normal"),
            extractOffsetDateTimeMeta(rs.getString("payload_json"), "scheduledAt"),
            extractTextMeta(rs.getString("payload_json"), "batchId", null),
            extractTextMeta(rs.getString("payload_json"), "deadLetterReason", null),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcNotificationTaskRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<NotificationTask> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT id, tenant_id, notify_type, template_code, target_receiver, send_status, retry_count, payload_json, created_at
                        FROM notification_task
                        WHERE tenant_id = ?
                        ORDER BY created_at DESC, id DESC
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<NotificationTask> findByNotificationTaskId(String notificationTaskId) {
        List<NotificationTask> matches = jdbcTemplate.query("""
                        SELECT id, tenant_id, notify_type, template_code, target_receiver, send_status, retry_count, payload_json, created_at
                        FROM notification_task
                        WHERE id = ?
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseNotificationTaskId(notificationTaskId)
        );
        return matches.stream().findFirst();
    }

    @Override
    public NotificationTask save(NotificationTask notificationTask) {
        if (notificationTask.notificationTaskId() == null || notificationTask.notificationTaskId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO notification_task (
                            tenant_id, notify_type, template_code, target_receiver, send_status, retry_count, payload_json, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, new String[]{"id"});
                statement.setLong(1, JdbcIdCodec.parseTenantId(notificationTask.tenantId()));
                statement.setString(2, notificationTask.notifyType());
                statement.setString(3, notificationTask.templateCode());
                statement.setString(4, notificationTask.targetReceiver());
                statement.setString(5, notificationTask.sendStatus());
                statement.setInt(6, notificationTask.retryCount());
                statement.setString(7, notificationTask.payloadJson());
                statement.setTimestamp(8, toTimestamp(notificationTask.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("notification_task primary key generation failed");
            }
            return new NotificationTask(
                    JdbcIdCodec.formatNotificationTaskId(key.longValue()),
                    notificationTask.tenantId(),
                    notificationTask.notifyType(),
                    notificationTask.templateCode(),
                    notificationTask.targetReceiver(),
                    notificationTask.sendStatus(),
                    notificationTask.retryCount(),
                    notificationTask.payloadJson(),
                    notificationTask.priority(),
                    notificationTask.scheduledAt(),
                    notificationTask.batchId(),
                    notificationTask.deadLetterReason(),
                    notificationTask.createdAt()
            );
        }

        jdbcTemplate.update("""
                        UPDATE notification_task
                        SET notify_type = ?, template_code = ?, target_receiver = ?, send_status = ?, retry_count = ?, payload_json = ?
                        WHERE id = ?
                        """,
                notificationTask.notifyType(),
                notificationTask.templateCode(),
                notificationTask.targetReceiver(),
                notificationTask.sendStatus(),
                notificationTask.retryCount(),
                notificationTask.payloadJson(),
                JdbcIdCodec.parseNotificationTaskId(notificationTask.notificationTaskId())
        );
        return notificationTask;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM notification_task");
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static String extractTextMeta(String payloadJson, String fieldName, String defaultValue) {
        JsonNode meta = parseMeta(payloadJson);
        if (meta != null && meta.hasNonNull(fieldName)) {
            return meta.get(fieldName).asText();
        }
        return defaultValue;
    }

    private static OffsetDateTime extractOffsetDateTimeMeta(String payloadJson, String fieldName) {
        JsonNode meta = parseMeta(payloadJson);
        if (meta != null && meta.hasNonNull(fieldName)) {
            return OffsetDateTime.parse(meta.get(fieldName).asText());
        }
        return null;
    }

    private static JsonNode parseMeta(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = new ObjectMapper().readTree(payloadJson);
            JsonNode meta = root.get("_meta");
            return meta != null && meta.isObject() ? meta : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}

