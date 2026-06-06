package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.TenantSubscriptionRepository;
import com.dianshang.platform.saas.model.TenantSubscription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcTenantSubscriptionRepository implements TenantSubscriptionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantSubscriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantSubscription save(TenantSubscription subscription) {
        long subscriptionDbId = JdbcIdCodec.parseSubscriptionId(subscription.subscriptionId());
        long tenantDbId = JdbcIdCodec.parseTenantId(subscription.tenantId());
        long planDbId = mapPlanId(subscription.planCode());
        int updated = jdbcTemplate.update(
                """
                UPDATE tenant_subscription
                SET tenant_id = ?, plan_id = ?, subscription_status = ?, started_at = ?, expired_at = ?, seat_count = ?, auto_renew_flag = ?
                WHERE id = ?
                """,
                tenantDbId,
                planDbId,
                subscription.subscriptionStatus(),
                toTimestamp(subscription.startedAt()),
                toTimestamp(subscription.expiredAt()),
                subscription.seatCount(),
                subscription.autoRenew(),
                subscriptionDbId
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO tenant_subscription (id, tenant_id, plan_id, subscription_status, started_at, expired_at, seat_count, auto_renew_flag)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    subscriptionDbId,
                    tenantDbId,
                    planDbId,
                    subscription.subscriptionStatus(),
                    toTimestamp(subscription.startedAt()),
                    toTimestamp(subscription.expiredAt()),
                    subscription.seatCount(),
                    subscription.autoRenew()
            );
        }
        return subscription;
    }

    @Override
    public Optional<TenantSubscription> findByTenantId(String tenantId) {
        List<TenantSubscription> subscriptions = jdbcTemplate.query(
                """
                SELECT ts.id, ts.tenant_id, ts.subscription_status, ts.started_at, ts.expired_at, ts.seat_count, ts.auto_renew_flag,
                       sp.plan_code, sp.plan_name
                FROM tenant_subscription ts
                JOIN subscription_plan sp ON sp.id = ts.plan_id
                WHERE ts.tenant_id = ?
                ORDER BY ts.id DESC
                """,
                (rs, rowNum) -> new TenantSubscription(
                        "sub-" + rs.getLong("id"),
                        "tenant-" + rs.getLong("tenant_id"),
                        rs.getString("plan_code"),
                        rs.getString("plan_name"),
                        rs.getString("subscription_status"),
                        toOffsetDateTime(rs.getTimestamp("started_at")),
                        toOffsetDateTime(rs.getTimestamp("expired_at")),
                        rs.getInt("seat_count"),
                        rs.getBoolean("auto_renew_flag")
                ),
                JdbcIdCodec.parseTenantId(tenantId)
        );
        return subscriptions.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tenant_subscription");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private long mapPlanId(String planCode) {
        return switch (planCode) {
            case "trial" -> 1L;
            case "basic" -> 2L;
            case "pro" -> 3L;
            default -> 100L + Math.abs(planCode.hashCode());
        };
    }
}
