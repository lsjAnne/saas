package backend.live.infrastructure.persistence;

import backend.live.domain.repository.LivePlanRepository;
import backend.live.model.LivePlan;
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
public class JdbcLivePlanRepository implements LivePlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLivePlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LivePlan save(LivePlan livePlan) {
        if (livePlan.livePlanId() == null || livePlan.livePlanId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO live_plan (
                            store_id, live_account_id, plan_name, plan_status, scheduled_start_at, scheduled_end_at, anchor_profile_name, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(livePlan.storeId()));
                if (livePlan.liveAccountId() == null || livePlan.liveAccountId().isBlank()) {
                    statement.setNull(2, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(2, JdbcIdCodec.parseChannelAccountId(livePlan.liveAccountId()));
                }
                statement.setString(3, livePlan.planName());
                statement.setString(4, livePlan.planStatus());
                statement.setTimestamp(5, toTimestamp(livePlan.scheduledStartAt()));
                statement.setTimestamp(6, toTimestamp(livePlan.scheduledEndAt()));
                statement.setString(7, livePlan.anchorProfileName());
                statement.setTimestamp(8, toTimestamp(livePlan.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("live_plan涓婚敭鐢熸垚澶辫触");
            }
            return new LivePlan(
                    JdbcIdCodec.formatLivePlanId(key.longValue()),
                    livePlan.storeId(),
                    livePlan.liveAccountId(),
                    livePlan.planName(),
                    livePlan.planStatus(),
                    livePlan.scheduledStartAt(),
                    livePlan.scheduledEndAt(),
                    livePlan.anchorProfileName(),
                    livePlan.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE live_plan
                SET live_account_id = ?, plan_name = ?, plan_status = ?, scheduled_start_at = ?, scheduled_end_at = ?, anchor_profile_name = ?
                WHERE id = ?
                """,
                livePlan.liveAccountId() == null || livePlan.liveAccountId().isBlank()
                        ? null : JdbcIdCodec.parseChannelAccountId(livePlan.liveAccountId()),
                livePlan.planName(),
                livePlan.planStatus(),
                toTimestamp(livePlan.scheduledStartAt()),
                toTimestamp(livePlan.scheduledEndAt()),
                livePlan.anchorProfileName(),
                JdbcIdCodec.parseLivePlanId(livePlan.livePlanId())
        );
        return livePlan;
    }

    @Override
    public List<LivePlan> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, live_account_id, plan_name, plan_status, scheduled_start_at, scheduled_end_at, anchor_profile_name, created_at
                FROM live_plan
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new LivePlan(
                        JdbcIdCodec.formatLivePlanId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getLong("live_account_id") == 0 ? null : JdbcIdCodec.formatChannelAccountId(rs.getLong("live_account_id")),
                        rs.getString("plan_name"),
                        rs.getString("plan_status"),
                        toOffsetDateTime(rs.getTimestamp("scheduled_start_at")),
                        toOffsetDateTime(rs.getTimestamp("scheduled_end_at")),
                        rs.getString("anchor_profile_name"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                arguments
        );
    }

    @Override
    public Optional<LivePlan> findByLivePlanId(String livePlanId) {
        List<LivePlan> livePlans = jdbcTemplate.query(
                """
                SELECT id, store_id, live_account_id, plan_name, plan_status, scheduled_start_at, scheduled_end_at, anchor_profile_name, created_at
                FROM live_plan
                WHERE id = ?
                """,
                (rs, rowNum) -> new LivePlan(
                        JdbcIdCodec.formatLivePlanId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getLong("live_account_id") == 0 ? null : JdbcIdCodec.formatChannelAccountId(rs.getLong("live_account_id")),
                        rs.getString("plan_name"),
                        rs.getString("plan_status"),
                        toOffsetDateTime(rs.getTimestamp("scheduled_start_at")),
                        toOffsetDateTime(rs.getTimestamp("scheduled_end_at")),
                        rs.getString("anchor_profile_name"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseLivePlanId(livePlanId)
        );
        return livePlans.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM live_plan");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

