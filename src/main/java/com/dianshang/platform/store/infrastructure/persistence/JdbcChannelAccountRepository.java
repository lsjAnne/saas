package com.dianshang.platform.store.infrastructure.persistence;

import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import com.dianshang.platform.store.domain.repository.ChannelAccountRepository;
import com.dianshang.platform.store.model.ChannelAccount;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcChannelAccountRepository implements ChannelAccountRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcChannelAccountRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChannelAccount save(ChannelAccount channelAccount) {
        if (channelAccount.channelAccountId() == null || channelAccount.channelAccountId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO channel_account (
                            organization_id,
                            channel_type,
                            account_name,
                            auth_status,
                            expires_at,
                            extra_config,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseOrganizationId(channelAccount.organizationId()));
                statement.setString(2, channelAccount.channelType());
                statement.setString(3, channelAccount.accountName());
                statement.setString(4, channelAccount.authStatus());
                statement.setTimestamp(5, toTimestamp(channelAccount.expiresAt()));
                statement.setString(6, toJson(channelAccount.extraConfig()));
                statement.setTimestamp(7, toTimestamp(channelAccount.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("channel_account主键生成失败");
            }
            return new ChannelAccount(
                    JdbcIdCodec.formatChannelAccountId(key.longValue()),
                    channelAccount.organizationId(),
                    channelAccount.channelType(),
                    channelAccount.accountName(),
                    channelAccount.authStatus(),
                    channelAccount.expiresAt(),
                    channelAccount.extraConfig(),
                    channelAccount.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE channel_account
                SET auth_status = ?, expires_at = ?, extra_config = CAST(? AS JSON)
                WHERE id = ?
                """,
                channelAccount.authStatus(),
                toTimestamp(channelAccount.expiresAt()),
                toJson(channelAccount.extraConfig()),
                JdbcIdCodec.parseChannelAccountId(channelAccount.channelAccountId())
        );
        return channelAccount;
    }

    @Override
    public List<ChannelAccount> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        String placeholders = organizationIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = organizationIds.stream()
                .map(JdbcIdCodec::parseOrganizationId)
                .toArray();
        return jdbcTemplate.query(
                """
                SELECT id, organization_id, channel_type, account_name, auth_status, expires_at, extra_config, created_at
                FROM channel_account
                WHERE organization_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> new ChannelAccount(
                        JdbcIdCodec.formatChannelAccountId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("channel_type"),
                        rs.getString("account_name"),
                        rs.getString("auth_status"),
                        toOffsetDateTime(rs.getTimestamp("expires_at")),
                        fromJson(rs.getString("extra_config")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                arguments
        );
    }

    @Override
    public Optional<ChannelAccount> findByChannelAccountId(String channelAccountId) {
        List<ChannelAccount> channelAccounts = jdbcTemplate.query(
                """
                SELECT id, organization_id, channel_type, account_name, auth_status, expires_at, extra_config, created_at
                FROM channel_account
                WHERE id = ?
                """,
                (rs, rowNum) -> new ChannelAccount(
                        JdbcIdCodec.formatChannelAccountId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("channel_type"),
                        rs.getString("account_name"),
                        rs.getString("auth_status"),
                        toOffsetDateTime(rs.getTimestamp("expires_at")),
                        fromJson(rs.getString("extra_config")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseChannelAccountId(channelAccountId)
        );
        return channelAccounts.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM channel_account");
    }

    private String toJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Map.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("channel配置序列化失败", exception);
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
            throw new IllegalArgumentException("channel配置反序列化失败", exception);
        }
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
