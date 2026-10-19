package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanSessionRepository;
import backend.digitalhuman.model.DigitalHumanSession;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcDigitalHumanSessionRepository implements DigitalHumanSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanSession save(DigitalHumanSession session) {
        if (session.sessionId() == null || session.sessionId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_session (
                            tenant_id, organization_id, schedule_id, avatar_id, platform_room_id, live_status,
                            started_at, ended_at, viewer_peak, order_count, gmv_amount, heartbeat_at, mock_flag, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(session.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(session.organizationId()));
                setNullableId(statement, 3, session.scheduleId(), true);
                setNullableId(statement, 4, session.avatarId(), false);
                statement.setString(5, session.platformRoomId());
                statement.setString(6, session.liveStatus());
                statement.setTimestamp(7, toTimestamp(session.startedAt()));
                statement.setTimestamp(8, toTimestamp(session.endedAt()));
                statement.setLong(9, session.viewerPeak());
                statement.setLong(10, session.orderCount());
                statement.setBigDecimal(11, session.gmvAmount());
                statement.setTimestamp(12, toTimestamp(session.heartbeatAt()));
                statement.setShort(13, (short) (session.mockFlag() ? 1 : 0));
                statement.setTimestamp(14, toTimestamp(session.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_session primary key generation failed");
            }
            return new DigitalHumanSession(
                    JdbcIdCodec.formatDigitalHumanSessionId(key.longValue()),
                    session.tenantId(),
                    session.organizationId(),
                    session.scheduleId(),
                    session.avatarId(),
                    session.platformRoomId(),
                    session.liveStatus(),
                    session.startedAt(),
                    session.endedAt(),
                    session.viewerPeak(),
                    session.orderCount(),
                    session.gmvAmount(),
                    session.heartbeatAt(),
                    session.mockFlag(),
                    session.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE digital_human_session
                SET platform_room_id = ?, live_status = ?, started_at = ?, ended_at = ?,
                    viewer_peak = ?, order_count = ?, gmv_amount = ?, heartbeat_at = ?
                WHERE id = ?
                """,
                session.platformRoomId(),
                session.liveStatus(),
                toTimestamp(session.startedAt()),
                toTimestamp(session.endedAt()),
                session.viewerPeak(),
                session.orderCount(),
                session.gmvAmount(),
                toTimestamp(session.heartbeatAt()),
                JdbcIdCodec.parseDigitalHumanSessionId(session.sessionId())
        );
        return session;
    }

    @Override
    public List<DigitalHumanSession> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, schedule_id, avatar_id, platform_room_id, live_status,
                       started_at, ended_at, viewer_peak, order_count, gmv_amount, heartbeat_at, mock_flag, created_at
                FROM digital_human_session
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public List<DigitalHumanSession> listLiveByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, schedule_id, avatar_id, platform_room_id, live_status,
                       started_at, ended_at, viewer_peak, order_count, gmv_amount, heartbeat_at, mock_flag, created_at
                FROM digital_human_session
                WHERE tenant_id = ? AND live_status = 'live'
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<DigitalHumanSession> findById(String tenantId, String sessionId) {
        List<DigitalHumanSession> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, schedule_id, avatar_id, platform_room_id, live_status,
                       started_at, ended_at, viewer_peak, order_count, gmv_amount, heartbeat_at, mock_flag, created_at
                FROM digital_human_session
                WHERE tenant_id = ? AND id = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseDigitalHumanSessionId(sessionId)
        );
        return result.stream().findFirst();
    }

    private DigitalHumanSession mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanSession(
                JdbcIdCodec.formatDigitalHumanSessionId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getObject("schedule_id") == null ? null : JdbcIdCodec.formatDigitalHumanScheduleId(rs.getLong("schedule_id")),
                rs.getObject("avatar_id") == null ? null : JdbcIdCodec.formatAvatarId(rs.getLong("avatar_id")),
                rs.getString("platform_room_id"),
                rs.getString("live_status"),
                toOffsetDateTime(rs.getTimestamp("started_at")),
                toOffsetDateTime(rs.getTimestamp("ended_at")),
                rs.getLong("viewer_peak"),
                rs.getLong("order_count"),
                rs.getBigDecimal("gmv_amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("gmv_amount"),
                toOffsetDateTime(rs.getTimestamp("heartbeat_at")),
                rs.getShort("mock_flag") == 1,
                toOffsetDateTime(rs.getTimestamp("created_at"))
        );
    }

    private void setNullableId(PreparedStatement statement, int index, String code, boolean schedule) throws SQLException {
        if (code == null || code.isBlank()) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else if (schedule) {
            statement.setLong(index, JdbcIdCodec.parseDigitalHumanScheduleId(code));
        } else {
            statement.setLong(index, JdbcIdCodec.parseAvatarId(code));
        }
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static Timestamp toTimestamp(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Timestamp.from(offsetDateTime.toInstant());
    }
}
