package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanScriptRepository;
import backend.digitalhuman.model.DigitalHumanScript;
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
public class JdbcDigitalHumanScriptRepository implements DigitalHumanScriptRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalHumanScriptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DigitalHumanScript save(DigitalHumanScript script) {
        if (script.scriptId() == null || script.scriptId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO digital_human_script (
                            tenant_id, organization_id, script_name, scene_type, content_text, version, status, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(script.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(script.organizationId()));
                statement.setString(3, script.scriptName());
                statement.setString(4, script.sceneType());
                statement.setString(5, script.contentText());
                statement.setString(6, script.version());
                statement.setString(7, script.status());
                statement.setTimestamp(8, toTimestamp(script.createdAt()));
                statement.setTimestamp(9, toTimestamp(script.updatedAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("digital_human_script primary key generation failed");
            }
            return new DigitalHumanScript(
                    JdbcIdCodec.formatDigitalHumanScriptId(key.longValue()),
                    script.tenantId(),
                    script.organizationId(),
                    script.scriptName(),
                    script.sceneType(),
                    script.contentText(),
                    script.version(),
                    script.status(),
                    script.createdAt(),
                    script.updatedAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE digital_human_script
                SET script_name = ?, scene_type = ?, content_text = ?, version = ?, status = ?, updated_at = ?
                WHERE id = ?
                """,
                script.scriptName(),
                script.sceneType(),
                script.contentText(),
                script.version(),
                script.status(),
                toTimestamp(script.updatedAt()),
                JdbcIdCodec.parseDigitalHumanScriptId(script.scriptId())
        );
        return script;
    }

    @Override
    public List<DigitalHumanScript> listByTenant(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, script_name, scene_type, content_text, version, status, created_at, updated_at
                FROM digital_human_script
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<DigitalHumanScript> findById(String tenantId, String scriptId) {
        List<DigitalHumanScript> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, script_name, scene_type, content_text, version, status, created_at, updated_at
                FROM digital_human_script
                WHERE tenant_id = ? AND id = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                JdbcIdCodec.parseTenantId(tenantId),
                JdbcIdCodec.parseDigitalHumanScriptId(scriptId)
        );
        return result.stream().findFirst();
    }

    private DigitalHumanScript mapRow(ResultSet rs) throws SQLException {
        return new DigitalHumanScript(
                JdbcIdCodec.formatDigitalHumanScriptId(rs.getLong("id")),
                JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("script_name"),
                rs.getString("scene_type"),
                rs.getString("content_text"),
                rs.getString("version"),
                rs.getString("status"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static Timestamp toTimestamp(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Timestamp.from(offsetDateTime.toInstant());
    }
}
