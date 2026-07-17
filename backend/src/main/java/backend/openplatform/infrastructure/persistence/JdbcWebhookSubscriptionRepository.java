package backend.openplatform.infrastructure.persistence;

import backend.openplatform.domain.repository.WebhookSubscriptionRepository;
import backend.openplatform.model.WebhookSubscription;
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
public class JdbcWebhookSubscriptionRepository implements WebhookSubscriptionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcWebhookSubscriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WebhookSubscription save(WebhookSubscription webhookSubscription) {
        if (webhookSubscription.subscriptionId() == null || webhookSubscription.subscriptionId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO webhook_subscription (
                            organization_id, event_code, callback_url,
                            secret_token, status, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseOrganizationId(webhookSubscription.organizationId()));
                statement.setString(2, webhookSubscription.eventCode());
                statement.setString(3, webhookSubscription.callbackUrl());
                statement.setString(4, webhookSubscription.secretToken());
                statement.setString(5, webhookSubscription.status());
                statement.setTimestamp(6, toTimestamp(webhookSubscription.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("webhook_subscription primary key generate failed");
            }
            return new WebhookSubscription(
                    JdbcIdCodec.formatWebhookSubscriptionId(key.longValue()),
                    webhookSubscription.organizationId(),
                    webhookSubscription.eventCode(),
                    webhookSubscription.callbackUrl(),
                    webhookSubscription.secretToken(),
                    webhookSubscription.status(),
                    webhookSubscription.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE webhook_subscription
                SET event_code = ?, callback_url = ?, secret_token = ?, status = ?
                WHERE id = ?
                """,
                webhookSubscription.eventCode(),
                webhookSubscription.callbackUrl(),
                webhookSubscription.secretToken(),
                webhookSubscription.status(),
                JdbcIdCodec.parseWebhookSubscriptionId(webhookSubscription.subscriptionId())
        );
        return webhookSubscription;
    }

    @Override
    public List<WebhookSubscription> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        String placeholders = organizationIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = organizationIds.stream().map(JdbcIdCodec::parseOrganizationId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, organization_id, event_code, callback_url, secret_token, status, created_at
                FROM webhook_subscription
                WHERE organization_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> new WebhookSubscription(
                        JdbcIdCodec.formatWebhookSubscriptionId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("event_code"),
                        rs.getString("callback_url"),
                        rs.getString("secret_token"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                arguments
        );
    }

    @Override
    public Optional<WebhookSubscription> findBySubscriptionId(String subscriptionId) {
        List<WebhookSubscription> subscriptions = jdbcTemplate.query(
                """
                SELECT id, organization_id, event_code, callback_url, secret_token, status, created_at
                FROM webhook_subscription
                WHERE id = ?
                """,
                (rs, rowNum) -> new WebhookSubscription(
                        JdbcIdCodec.formatWebhookSubscriptionId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("event_code"),
                        rs.getString("callback_url"),
                        rs.getString("secret_token"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseWebhookSubscriptionId(subscriptionId)
        );
        return subscriptions.stream().findFirst();
    }

    @Override
    public Optional<WebhookSubscription> findByOrganizationIdAndEventCodeAndCallbackUrl(String organizationId,
                                                                                         String eventCode,
                                                                                         String callbackUrl) {
        List<WebhookSubscription> subscriptions = jdbcTemplate.query(
                """
                SELECT id, organization_id, event_code, callback_url, secret_token, status, created_at
                FROM webhook_subscription
                WHERE organization_id = ? AND event_code = ? AND callback_url = ?
                """,
                (rs, rowNum) -> new WebhookSubscription(
                        JdbcIdCodec.formatWebhookSubscriptionId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("event_code"),
                        rs.getString("callback_url"),
                        rs.getString("secret_token"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseOrganizationId(organizationId),
                eventCode,
                callbackUrl
        );
        return subscriptions.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM webhook_subscription");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

