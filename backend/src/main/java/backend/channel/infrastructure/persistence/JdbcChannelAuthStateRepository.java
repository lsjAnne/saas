package backend.channel.infrastructure.persistence;

import backend.channel.domain.repository.ChannelAuthStateRepository;
import backend.channel.model.ChannelAuthState;
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

/**
 * channel_auth_state 的 JDBC 实现。本模块 ID 前缀 channel-state-，租户/组织 ID 在类内做数值后缀解析，不复用 JdbcIdCodec。
 */
@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcChannelAuthStateRepository implements ChannelAuthStateRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcChannelAuthStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ChannelAuthState save(ChannelAuthState authState) {
        if (authState.authStateId() == null || authState.authStateId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO channel_auth_state (
                            tenant_id,
                            organization_id,
                            channel_type,
                            channel_account_id,
                            state,
                            redirect_uri,
                            created_at,
                            expires_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, parseTenantId(authState.tenantId()));
                statement.setLong(2, parseOrganizationId(authState.organizationId()));
                statement.setString(3, authState.channelType());
                Long channelAccountId = parseChannelAccountId(authState.channelAccountId());
                if (channelAccountId == null) {
                    statement.setObject(4, null);
                } else {
                    statement.setLong(4, channelAccountId);
                }
                statement.setString(5, authState.state());
                statement.setString(6, authState.redirectUri());
                statement.setTimestamp(7, toTimestamp(authState.createdAt()));
                statement.setTimestamp(8, toTimestamp(authState.expiresAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("failed to create channel_auth_state id");
            }
            return new ChannelAuthState(
                    formatAuthStateId(key.longValue()),
                    authState.tenantId(),
                    authState.organizationId(),
                    authState.channelType(),
                    authState.channelAccountId(),
                    authState.state(),
                    authState.redirectUri(),
                    authState.createdAt(),
                    authState.expiresAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE channel_auth_state
                SET channel_type = ?, channel_account_id = ?, state = ?, redirect_uri = ?, expires_at = ?
                WHERE id = ?
                """,
                authState.channelType(),
                parseChannelAccountId(authState.channelAccountId()),
                authState.state(),
                authState.redirectUri(),
                toTimestamp(authState.expiresAt()),
                parseAuthStateId(authState.authStateId())
        );
        return authState;
    }

    @Override
    public Optional<ChannelAuthState> findByState(String state) {
        List<ChannelAuthState> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, channel_type, channel_account_id, state, redirect_uri, created_at, expires_at
                FROM channel_auth_state
                WHERE state = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                state
        );
        return result.stream().findFirst();
    }

    @Override
    public void deleteByState(String state) {
        jdbcTemplate.update("DELETE FROM channel_auth_state WHERE state = ?", state);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM channel_auth_state");
    }

    private ChannelAuthState mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Long channelAccountId = (Long) rs.getObject("channel_account_id");
        return new ChannelAuthState(
                formatAuthStateId(rs.getLong("id")),
                formatTenantId(rs.getLong("tenant_id")),
                formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("channel_type"),
                formatChannelAccountId(channelAccountId),
                rs.getString("state"),
                rs.getString("redirect_uri"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("expires_at"))
        );
    }

    private long parseTenantId(String id) {
        return Long.parseLong(id.substring("tenant-".length()));
    }

    private String formatTenantId(long id) {
        return "tenant-" + id;
    }

    private long parseOrganizationId(String id) {
        return Long.parseLong(id.substring("org-".length()));
    }

    private String formatOrganizationId(long id) {
        return "org-" + id;
    }

    private long parseAuthStateId(String id) {
        return Long.parseLong(id.substring("channel-state-".length()));
    }

    private String formatAuthStateId(long id) {
        return "channel-state-" + id;
    }

    private Long parseChannelAccountId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return Long.parseLong(id.substring("channel-".length()));
    }

    private String formatChannelAccountId(Long id) {
        return id == null ? null : "channel-" + id;
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
