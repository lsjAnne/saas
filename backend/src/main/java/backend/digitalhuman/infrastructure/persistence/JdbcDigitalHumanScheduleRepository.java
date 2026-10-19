package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanScheduleRepository;
import backend.digitalhuman.model.DigitalHumanSchedule;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

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
public class JdbcDigitalHumanScheduleRepository implements DigitalHumanScheduleRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanSchedule save(DigitalHumanSchedule schedule) {
        if (schedule.scheduleId() == null || schedule.scheduleId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_schedule (
                            tenant_id, organization_id, schedule_name, avatar_id, script_id, platform_type,
                            repeat_type, start_at, end_at, enabled, status, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(schedule.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(schedule.organizationId()));
                statement.setString(3, schedule.scheduleName());
                statement.setLong(4, JdbcIdCodec.parseAvatarId(schedule.avatarId()));
                statement.setLong(5, JdbcIdCodec.parseDigitalHumanScriptId(schedule.scriptId()));
                statement.setString(6, schedule.platformType());
                statement.setString(7, schedule.repeatType());
                statement.setTimestamp(8, toTimestamp(schedule.startAt()));
                statement.setTimestamp(9, toTimestamp(schedule.endAt()));
                statement.setShort(10, (short) (schedule.enabled() ? 1 : 0));
                statement.setString(11, schedule.status());
                statement.setTimestamp(12, toTimestamp(schedule.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_schedule primary key generation failed");
            }
            return new DigitalHumanSchedule(
                    JdbcIdCodec.formatDigitalHumanScheduleId(key.longValue()),
                    schedule.tenantId(),
                    schedule.organizationId(),
                    schedule.scheduleName(),
                    schedule.avatarId(),
                    schedule.scriptId(),
                    schedule.platformType(),
                    schedule.repeatType(),
                    schedule.startAt(),
                    schedule.endAt(),
                    schedule.enabled(),
                    schedule.status(),
                    schedule.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE digital_human_schedule
                SET schedule_name = ?, avatar_id = ?, script_id = ?, platform_type = ?, repeat_type = ?,
                    start_at = ?, end_at = ?, enabled = ?, status = ?
                WHERE id = ?
                """,
                schedule.scheduleName(),
                JdbcIdCodec.parseAvatarId(schedule.avatarId()),
                JdbcIdCodec.parseDigitalHumanScriptId(schedule.scriptId()),
                schedule.platformType(),
                schedule.repeatType(),
                toTimestamp(schedule.startAt()),
                toTimestamp(schedule.endAt()),
                (short) (schedule.enabled() ? 1 : 0),
                schedule.status(),
                JdbcIdCodec.parseDigitalHumanScheduleId(schedule.scheduleId())
        );
        return schedule;
    }

    @Override
    public List<DigitalHumanSchedule> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, schedule_name, avatar_id, script_id, platform_type,
                       repeat_type, start_at, end_at, enabled, status, created_at
                FROM digital_human_schedule
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<DigitalHumanSchedule> findById(String tenantId, String scheduleId) {
        List<DigitalHumanSchedule> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, schedule_name, avatar_id, script_id, platform_type,
                       repeat_type, start_at, end_at, enabled, status, created_at
                FROM digital_human_schedule
                WHERE tenant_id = ? AND id = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseDigitalHumanScheduleId(scheduleId)
        );
        return result.stream().findFirst();
    }

    private DigitalHumanSchedule mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanSchedule(
                JdbcIdCodec.formatDigitalHumanScheduleId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("schedule_name"),
                JdbcIdCodec.formatAvatarId(rs.getLong("avatar_id")),
                JdbcIdCodec.formatDigitalHumanScriptId(rs.getLong("script_id")),
                rs.getString("platform_type"),
                rs.getString("repeat_type"),
                toOffsetDateTime(rs.getTimestamp("start_at")),
                toOffsetDateTime(rs.getTimestamp("end_at")),
                rs.getShort("enabled") == 1,
                rs.getString("status"),
                toOffsetDateTime(rs.getTimestamp("created_at"))
        );
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static Timestamp toTimestamp(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Timestamp.from(offsetDateTime.toInstant());
    }
}
