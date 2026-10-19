package backend.channel.infrastructure.persistence;

import backend.channel.domain.repository.ChannelTokenRepository;
import backend.channel.model.ChannelToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * channel_token 的 JDBC 实现。extra_config 用 TEXT 列存储 JSON 文本（达梦/H2 兼容），不复用 JdbcIdCodec。
 */
@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcChannelTokenRepository implements ChannelTokenRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcChannelTokenRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChannelToken save(ChannelToken token) {
        if (token.channelTokenId() == null || token.channelTokenId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO channel_token (
                            tenant_id,
                            organization_id,
                            channel_type,
                            account_name,
                            access_token,
                            refresh_token,
                            token_type,
                            scope,
                            expires_at,
                            refresh_expires_at,
                            extra_config,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, parseTenantId(token.tenantId()));
                statement.setLong(2, parseOrganizationId(token.organizationId()));
                statement.setString(3, token.channelType());
                statement.setString(4, token.accountName());
                statement.setString(5, token.accessToken());
                statement.setString(6, token.refreshToken());
                statement.setString(7, token.tokenType());
                statement.setString(8, token.scope());
                statement.setTimestamp(9, toTimestamp(token.expiresAt()));
                statement.setTimestamp(10, toTimestamp(token.refreshExpiresAt()));
                statement.setString(11, toJson(token.extraConfig()));
                statement.setTimestamp(12, toTimestamp(token.createdAt()));
                statement.setTimestamp(13, toTimestamp(token.updatedAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("failed to create channel_token id");
            }
            return new ChannelToken(
                    formatTokenId(key.longValue()),
                    token.tenantId(),
                    token.organizationId(),
                    token.channelType(),
                    token.accountName(),
                    token.accessToken(),
                    token.refreshToken(),
                    token.tokenType(),
                    token.scope(),
                    token.expiresAt(),
                    token.refreshExpiresAt(),
                    token.extraConfig(),
                    token.createdAt(),
                    token.updatedAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE channel_token
                SET access_token = ?, refresh_token = ?, token_type = ?, scope = ?,
                    expires_at = ?, refresh_expires_at = ?, extra_config = ?, updated_at = ?
                WHERE id = ?
                """,
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.scope(),
                toTimestamp(token.expiresAt()),
                toTimestamp(token.refreshExpiresAt()),
                toJson(token.extraConfig()),
                toTimestamp(token.updatedAt()),
                parseTokenId(token.channelTokenId())
        );
        return token;
    }

    @Override
    public Optional<ChannelToken> findByChannelTokenId(String channelTokenId) {
        List<ChannelToken> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, channel_type, account_name, access_token, refresh_token,
                       token_type, scope, expires_at, refresh_expires_at, extra_config, created_at, updated_at
                FROM channel_token
                WHERE id = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                parseTokenId(channelTokenId)
        );
        return result.stream().findFirst();
    }

    @Override
    public Optional<ChannelToken> findByOrganizationAndTypeAndAccount(String organizationId, String channelType, String accountName) {
        List<ChannelToken> result = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, channel_type, account_name, access_token, refresh_token,
                       token_type, scope, expires_at, refresh_expires_at, extra_config, created_at, updated_at
                FROM channel_token
                WHERE organization_id = ? AND channel_type = ? AND account_name = ?
                """,
                (rs, rowNum) -> mapRow(rs),
                parseOrganizationId(organizationId),
                channelType,
                accountName
        );
        return result.stream().findFirst();
    }

    @Override
    public List<ChannelToken> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        String placeholders = organizationIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = organizationIds.stream().map(this::parseOrganizationId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, channel_type, account_name, access_token, refresh_token,
                       token_type, scope, expires_at, refresh_expires_at, extra_config, created_at, updated_at
                FROM channel_token
                WHERE organization_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> mapRow(rs),
                arguments
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM channel_token");
    }

    private ChannelToken mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ChannelToken(
                formatTokenId(rs.getLong("id")),
                formatTenantId(rs.getLong("tenant_id")),
                formatOrganizationId(rs.getLong("organization_id")),
                rs.getString("channel_type"),
                rs.getString("account_name"),
                rs.getString("access_token"),
                rs.getString("refresh_token"),
                rs.getString("token_type"),
                rs.getString("scope"),
                toOffsetDateTime(rs.getTimestamp("expires_at")),
                toOffsetDateTime(rs.getTimestamp("refresh_expires_at")),
                fromJson(rs.getString("extra_config")),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
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

    private long parseTokenId(String id) {
        return Long.parseLong(id.substring("channel-token-".length()));
    }

    private String formatTokenId(long id) {
        return "channel-token-" + id;
    }

    private String toJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Map.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("channel token config serialization failed", exception);
        }
    }

    private Map<String, Object> fromJson(String values) {
        if (values == null || values.isBlank()) {
            return Map.of();
        }
        try {
            if (objectMapper.readTree(values).isTextual()) {
                return objectMapper.readValue(objectMapper.readTree(values).asText(), MAP_TYPE);
            }
            return objectMapper.readValue(values, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("channel token config deserialization failed", exception);
        }
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
