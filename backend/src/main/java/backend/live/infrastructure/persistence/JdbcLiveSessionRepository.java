package backend.live.infrastructure.persistence;

import backend.live.domain.repository.LiveSessionRepository;
import backend.live.model.LiveSession;
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
public class JdbcLiveSessionRepository implements LiveSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLiveSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LiveSession save(LiveSession liveSession) {
        if (liveSession.liveSessionId() == null || liveSession.liveSessionId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO live_session (
                            live_plan_id, tenant_id, store_id, live_account_id, session_status, room_id, actual_start_at, actual_end_at, error_message,
                            control_mode, current_scene, takeover_status, takeover_operator, promise_audit_status, promise_audit_remark, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseLivePlanId(liveSession.livePlanId()));
                statement.setLong(2, JdbcIdCodec.parseTenantId(liveSession.tenantId()));
                statement.setLong(3, JdbcIdCodec.parseStoreId(liveSession.storeId()));
                if (liveSession.liveAccountId() == null || liveSession.liveAccountId().isBlank()) {
                    statement.setNull(4, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(4, JdbcIdCodec.parseChannelAccountId(liveSession.liveAccountId()));
                }
                statement.setString(5, liveSession.sessionStatus());
                statement.setString(6, liveSession.roomId());
                statement.setTimestamp(7, toTimestamp(liveSession.actualStartAt()));
                statement.setTimestamp(8, toTimestamp(liveSession.actualEndAt()));
                statement.setString(9, liveSession.errorMessage());
                statement.setString(10, liveSession.controlMode());
                statement.setString(11, liveSession.currentScene());
                statement.setString(12, liveSession.takeoverStatus());
                statement.setString(13, liveSession.takeoverOperator());
                statement.setString(14, liveSession.promiseAuditStatus());
                statement.setString(15, liveSession.promiseAuditRemark());
                statement.setTimestamp(16, toTimestamp(liveSession.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("live_session涓婚敭鐢熸垚澶辫触");
            }
            return new LiveSession(
                    JdbcIdCodec.formatLiveSessionId(key.longValue()),
                    liveSession.livePlanId(),
                    liveSession.tenantId(),
                    liveSession.storeId(),
                    liveSession.liveAccountId(),
                    liveSession.sessionStatus(),
                    liveSession.roomId(),
                    liveSession.actualStartAt(),
                    liveSession.actualEndAt(),
                    liveSession.errorMessage(),
                    liveSession.controlMode(),
                    liveSession.currentScene(),
                    liveSession.takeoverStatus(),
                    liveSession.takeoverOperator(),
                    liveSession.promiseAuditStatus(),
                    liveSession.promiseAuditRemark(),
                    liveSession.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE live_session
                SET live_account_id = ?, session_status = ?, room_id = ?, actual_start_at = ?, actual_end_at = ?, error_message = ?,
                    control_mode = ?, current_scene = ?, takeover_status = ?, takeover_operator = ?, promise_audit_status = ?, promise_audit_remark = ?
                WHERE id = ?
                """,
                liveSession.liveAccountId() == null || liveSession.liveAccountId().isBlank()
                        ? null : JdbcIdCodec.parseChannelAccountId(liveSession.liveAccountId()),
                liveSession.sessionStatus(),
                liveSession.roomId(),
                toTimestamp(liveSession.actualStartAt()),
                toTimestamp(liveSession.actualEndAt()),
                liveSession.errorMessage(),
                liveSession.controlMode(),
                liveSession.currentScene(),
                liveSession.takeoverStatus(),
                liveSession.takeoverOperator(),
                liveSession.promiseAuditStatus(),
                liveSession.promiseAuditRemark(),
                JdbcIdCodec.parseLiveSessionId(liveSession.liveSessionId())
        );
        return liveSession;
    }

    @Override
    public Optional<LiveSession> findRunningByLiveAccountId(String liveAccountId) {
        List<LiveSession> liveSessions = jdbcTemplate.query(
                """
                SELECT id, live_plan_id, tenant_id, store_id, live_account_id, session_status, room_id, actual_start_at, actual_end_at, error_message,
                       control_mode, current_scene, takeover_status, takeover_operator, promise_audit_status, promise_audit_remark, created_at
                FROM live_session
                WHERE live_account_id = ? AND session_status = 'running'
                ORDER BY created_at DESC, id DESC
                """,
                (rs, rowNum) -> mapSession(rs.getLong("id"),
                        rs.getLong("live_plan_id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("store_id"),
                        rs.getLong("live_account_id"),
                        rs.getString("session_status"),
                        rs.getString("room_id"),
                        rs.getTimestamp("actual_start_at"),
                        rs.getTimestamp("actual_end_at"),
                        rs.getString("error_message"),
                        rs.getString("control_mode"),
                        rs.getString("current_scene"),
                        rs.getString("takeover_status"),
                        rs.getString("takeover_operator"),
                        rs.getString("promise_audit_status"),
                        rs.getString("promise_audit_remark"),
                        rs.getTimestamp("created_at")),
                JdbcIdCodec.parseChannelAccountId(liveAccountId)
        );
        return liveSessions.stream().findFirst();
    }

    @Override
    public List<LiveSession> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, live_plan_id, tenant_id, store_id, live_account_id, session_status, room_id, actual_start_at, actual_end_at, error_message,
                       control_mode, current_scene, takeover_status, takeover_operator, promise_audit_status, promise_audit_remark, created_at
                FROM live_session
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapSession(rs.getLong("id"),
                        rs.getLong("live_plan_id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("store_id"),
                        rs.getLong("live_account_id"),
                        rs.getString("session_status"),
                        rs.getString("room_id"),
                        rs.getTimestamp("actual_start_at"),
                        rs.getTimestamp("actual_end_at"),
                        rs.getString("error_message"),
                        rs.getString("control_mode"),
                        rs.getString("current_scene"),
                        rs.getString("takeover_status"),
                        rs.getString("takeover_operator"),
                        rs.getString("promise_audit_status"),
                        rs.getString("promise_audit_remark"),
                        rs.getTimestamp("created_at")),
                arguments
        );
    }

    @Override
    public Optional<LiveSession> findLatestByLivePlanId(String livePlanId) {
        List<LiveSession> liveSessions = jdbcTemplate.query(
                """
                SELECT id, live_plan_id, tenant_id, store_id, live_account_id, session_status, room_id, actual_start_at, actual_end_at, error_message,
                       control_mode, current_scene, takeover_status, takeover_operator, promise_audit_status, promise_audit_remark, created_at
                FROM live_session
                WHERE live_plan_id = ?
                ORDER BY created_at DESC, id DESC
                """,
                (rs, rowNum) -> mapSession(rs.getLong("id"),
                        rs.getLong("live_plan_id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("store_id"),
                        rs.getLong("live_account_id"),
                        rs.getString("session_status"),
                        rs.getString("room_id"),
                        rs.getTimestamp("actual_start_at"),
                        rs.getTimestamp("actual_end_at"),
                        rs.getString("error_message"),
                        rs.getString("control_mode"),
                        rs.getString("current_scene"),
                        rs.getString("takeover_status"),
                        rs.getString("takeover_operator"),
                        rs.getString("promise_audit_status"),
                        rs.getString("promise_audit_remark"),
                        rs.getTimestamp("created_at")),
                JdbcIdCodec.parseLivePlanId(livePlanId)
        );
        return liveSessions.stream().findFirst();
    }

    @Override
    public int countRunningByTenantId(String tenantId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM live_session
                WHERE tenant_id = ? AND session_status = 'running'
                """,
                Integer.class,
                JdbcIdCodec.parseTenantId(tenantId)
        );
        return count == null ? 0 : count;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM live_session");
    }

    private LiveSession mapSession(long liveSessionId,
                                   long livePlanId,
                                   long tenantId,
                                   long storeId,
                                   long liveAccountId,
                                   String sessionStatus,
                                   String roomId,
                                   Timestamp actualStartAt,
                                   Timestamp actualEndAt,
                                   String errorMessage,
                                   String controlMode,
                                   String currentScene,
                                   String takeoverStatus,
                                   String takeoverOperator,
                                   String promiseAuditStatus,
                                   String promiseAuditRemark,
                                   Timestamp createdAt) {
        return new LiveSession(
                JdbcIdCodec.formatLiveSessionId(liveSessionId),
                JdbcIdCodec.formatLivePlanId(livePlanId),
                JdbcIdCodec.formatTenantId(tenantId),
                JdbcIdCodec.formatStoreId(storeId),
                liveAccountId == 0 ? null : JdbcIdCodec.formatChannelAccountId(liveAccountId),
                sessionStatus,
                roomId,
                toOffsetDateTime(actualStartAt),
                toOffsetDateTime(actualEndAt),
                errorMessage,
                controlMode,
                currentScene,
                takeoverStatus,
                takeoverOperator,
                promiseAuditStatus,
                promiseAuditRemark,
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

