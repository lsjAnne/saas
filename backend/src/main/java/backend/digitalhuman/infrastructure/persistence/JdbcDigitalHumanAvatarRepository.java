package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanAvatarRepository;
import backend.digitalhuman.model.DigitalHumanAvatar;
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
public class JdbcDigitalHumanAvatarRepository implements DigitalHumanAvatarRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanAvatarRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanAvatar save(DigitalHumanAvatar avatar) {
        if (avatar.avatarId() == null || avatar.avatarId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_avatar (
                            tenant_id, organization_id, avatar_name, avatar_type, voice_name, preview_url, enabled, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(avatar.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(avatar.organizationId()));
                statement.setString(3, avatar.avatarName());
                statement.setString(4, avatar.avatarType());
                statement.setString(5, avatar.voiceName());
                statement.setString(6, avatar.previewUrl());
                statement.setShort(7, (short) (avatar.enabled() ? 1 : 0));
                statement.setTimestamp(8, toTimestamp(avatar.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_avatar primary key generation failed");
            }
            return new DigitalHumanAvatar(
                    JdbcIdCodec.formatAvatarId(key.longValue()),
                    avatar.tenantId(),
                    avatar.organizationId(),
                    avatar.avatarName(),
                    avatar.avatarType(),
                    avatar.voiceName(),
                    avatar.previewUrl(),
                    avatar.enabled(),
                    avatar.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE digital_human_avatar
                SET avatar_name = ?, avatar_type = ?, voice_name = ?, preview_url = ?, enabled = ?
                WHERE id = ?
                """,
                avatar.avatarName(),
                avatar.avatarType(),
                avatar.voiceName(),
                avatar.previewUrl(),
                (short) (avatar.enabled() ? 1 : 0),
                JdbcIdCodec.parseAvatarId(avatar.avatarId())
        );
        return avatar;
    }

    @Override
    public List<DigitalHumanAvatar> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, avatar_name, avatar_type, voice_name, preview_url, enabled, created_at
                FROM digital_human_avatar
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<DigitalHumanAvatar> findById(String tenantId, String avatarId) {
        List<DigitalHumanAvatar> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, avatar_name, avatar_type, voice_name, preview_url, enabled, created_at
                FROM digital_human_avatar
                WHERE tenant_id = ? AND id = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseAvatarId(avatarId)
        );
        return result.stream().findFirst();
    }

    @Override
    public void deleteById(String tenantId, String avatarId) {
        jdbcTemplate.update(
                "DELETE FROM digital_human_avatar WHERE tenant_id = ? AND id = ?",
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseAvatarId(avatarId)
        );
    }

    private DigitalHumanAvatar mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanAvatar(
                JdbcIdCodec.formatAvatarId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("avatar_name"),
                rs.getString("avatar_type"),
                rs.getString("voice_name"),
                rs.getString("preview_url"),
                rs.getShort("enabled") == 1,
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
