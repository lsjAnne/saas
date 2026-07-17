package backend.live.infrastructure.persistence;

import backend.live.domain.repository.LiveScriptRepository;
import backend.live.model.LiveScript;
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

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcLiveScriptRepository implements LiveScriptRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLiveScriptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LiveScript save(LiveScript liveScript) {
        if (liveScript.liveScriptId() == null || liveScript.liveScriptId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO live_script (
                            live_plan_id, product_id, script_version, script_content, is_active, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseLivePlanId(liveScript.livePlanId()));
                if (liveScript.productId() == null || liveScript.productId().isBlank()) {
                    statement.setNull(2, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(2, JdbcIdCodec.parseProductId(liveScript.productId()));
                }
                statement.setString(3, liveScript.scriptVersion());
                statement.setString(4, liveScript.scriptContent());
                statement.setBoolean(5, liveScript.active());
                statement.setTimestamp(6, toTimestamp(liveScript.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("live_script涓婚敭鐢熸垚澶辫触");
            }
            return new LiveScript(
                    JdbcIdCodec.formatLiveScriptId(key.longValue()),
                    liveScript.livePlanId(),
                    liveScript.productId(),
                    liveScript.scriptVersion(),
                    liveScript.scriptContent(),
                    liveScript.active(),
                    liveScript.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE live_script
                SET product_id = ?, script_version = ?, script_content = ?, is_active = ?
                WHERE id = ?
                """,
                liveScript.productId() == null || liveScript.productId().isBlank()
                        ? null : JdbcIdCodec.parseProductId(liveScript.productId()),
                liveScript.scriptVersion(),
                liveScript.scriptContent(),
                liveScript.active(),
                JdbcIdCodec.parseLiveScriptId(liveScript.liveScriptId())
        );
        return liveScript;
    }

    @Override
    public List<LiveScript> findByLivePlanId(String livePlanId) {
        return jdbcTemplate.query(
                """
                SELECT id, live_plan_id, product_id, script_version, script_content, is_active, created_at
                FROM live_script
                WHERE live_plan_id = ?
                ORDER BY created_at DESC, id DESC
                """,
                (rs, rowNum) -> new LiveScript(
                        JdbcIdCodec.formatLiveScriptId(rs.getLong("id")),
                        JdbcIdCodec.formatLivePlanId(rs.getLong("live_plan_id")),
                        rs.getLong("product_id") == 0 ? null : JdbcIdCodec.formatProductId(rs.getLong("product_id")),
                        rs.getString("script_version"),
                        rs.getString("script_content"),
                        rs.getBoolean("is_active"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseLivePlanId(livePlanId)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM live_script");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

