package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanBarrageRepository;
import backend.digitalhuman.model.DigitalHumanBarrage;
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

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcDigitalHumanBarrageRepository implements DigitalHumanBarrageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanBarrageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanBarrage save(DigitalHumanBarrage barrage) {
        if (barrage.barrageId() == null || barrage.barrageId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_barrage (
                            tenant_id, organization_id, session_id, platform_type,
                            user_nick, content, reply_status, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(barrage.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(barrage.organizationId()));
                statement.setString(3, barrage.sessionId());
                statement.setString(4, barrage.platformType());
                statement.setString(5, barrage.userNick());
                statement.setString(6, barrage.content());
                statement.setString(7, barrage.replyStatus());
                statement.setTimestamp(8, toTimestamp(barrage.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_barrage primary key generation failed");
            }
            return new DigitalHumanBarrage(
                    JdbcIdCodec.formatDigitalHumanBarrageId(key.longValue()),
                    barrage.tenantId(),
                    barrage.organizationId(),
                    barrage.sessionId(),
                    barrage.platformType(),
                    barrage.userNick(),
                    barrage.content(),
                    barrage.replyStatus(),
                    barrage.createdAt()
            );
        }

        jdbcTemplate.update(
                "UPDATE digital_human_barrage SET reply_status = ? WHERE id = ?",
                barrage.replyStatus(),
                JdbcIdCodec.parseDigitalHumanBarrageId(barrage.barrageId())
        );
        return barrage;
    }

    @Override
    public List<DigitalHumanBarrage> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, session_id, platform_type,
                       user_nick, content, reply_status, created_at
                FROM digital_human_barrage
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public List<DigitalHumanBarrage> listBySession(String tenantId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return listByTenant(tenantId);
        }
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, session_id, platform_type,
                       user_nick, content, reply_status, created_at
                FROM digital_human_barrage
                WHERE tenant_id = ? AND session_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                sessionId
        );
    }

    private DigitalHumanBarrage mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanBarrage(
                JdbcIdCodec.formatDigitalHumanBarrageId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("session_id"),
                rs.getString("platform_type"),
                rs.getString("user_nick"),
                rs.getString("content"),
                rs.getString("reply_status"),
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
