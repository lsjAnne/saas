package backend.notification.infrastructure.persistence;

import backend.notification.domain.repository.NotificationTemplateRepository;
import backend.notification.model.NotificationTemplate;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
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
public class JdbcNotificationTemplateRepository implements NotificationTemplateRepository {

    private static final RowMapper<NotificationTemplate> ROW_MAPPER = (rs, rowNum) -> new NotificationTemplate(
            JdbcIdCodec.formatNotificationTemplateId(rs.getLong("id")),
            JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
            rs.getString("template_code"),
            rs.getString("template_name"),
            rs.getString("notify_type"),
            rs.getString("title_template"),
            rs.getString("content_template"),
            rs.getBoolean("is_enabled"),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcNotificationTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<NotificationTemplate> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT id, tenant_id, template_code, template_name, notify_type, title_template, content_template, is_enabled, created_at
                        FROM notification_template
                        WHERE tenant_id = ?
                        ORDER BY template_code, id
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<NotificationTemplate> findByNotificationTemplateId(String notificationTemplateId) {
        List<NotificationTemplate> matches = jdbcTemplate.query("""
                        SELECT id, tenant_id, template_code, template_name, notify_type, title_template, content_template, is_enabled, created_at
                        FROM notification_template
                        WHERE id = ?
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseNotificationTemplateId(notificationTemplateId)
        );
        return matches.stream().findFirst();
    }

    @Override
    public Optional<NotificationTemplate> findByTenantIdAndTemplateCode(String tenantId, String templateCode) {
        List<NotificationTemplate> matches = jdbcTemplate.query("""
                        SELECT id, tenant_id, template_code, template_name, notify_type, title_template, content_template, is_enabled, created_at
                        FROM notification_template
                        WHERE tenant_id = ? AND template_code = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseTenantId(tenantId),
                templateCode
        );
        return matches.stream().findFirst();
    }

    @Override
    public NotificationTemplate save(NotificationTemplate notificationTemplate) {
        if (notificationTemplate.notificationTemplateId() == null || notificationTemplate.notificationTemplateId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO notification_template (
                            tenant_id, template_code, template_name, notify_type, title_template, content_template, is_enabled, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, new String[]{"id"});
                statement.setLong(1, JdbcIdCodec.parseTenantId(notificationTemplate.tenantId()));
                statement.setString(2, notificationTemplate.templateCode());
                statement.setString(3, notificationTemplate.templateName());
                statement.setString(4, notificationTemplate.notifyType());
                statement.setString(5, notificationTemplate.titleTemplate());
                statement.setString(6, notificationTemplate.contentTemplate());
                statement.setBoolean(7, notificationTemplate.enabled());
                statement.setTimestamp(8, toTimestamp(notificationTemplate.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("notification_template primary key generation failed");
            }
            return new NotificationTemplate(
                    JdbcIdCodec.formatNotificationTemplateId(key.longValue()),
                    notificationTemplate.tenantId(),
                    notificationTemplate.templateCode(),
                    notificationTemplate.templateName(),
                    notificationTemplate.notifyType(),
                    notificationTemplate.titleTemplate(),
                    notificationTemplate.contentTemplate(),
                    notificationTemplate.enabled(),
                    notificationTemplate.createdAt()
            );
        }

        jdbcTemplate.update("""
                        UPDATE notification_template
                        SET template_name = ?, notify_type = ?, title_template = ?, content_template = ?, is_enabled = ?
                        WHERE id = ?
                        """,
                notificationTemplate.templateName(),
                notificationTemplate.notifyType(),
                notificationTemplate.titleTemplate(),
                notificationTemplate.contentTemplate(),
                notificationTemplate.enabled(),
                JdbcIdCodec.parseNotificationTemplateId(notificationTemplate.notificationTemplateId())
        );
        return notificationTemplate;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM notification_template");
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

