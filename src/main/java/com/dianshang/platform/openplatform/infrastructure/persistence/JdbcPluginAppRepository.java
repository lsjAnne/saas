package com.dianshang.platform.openplatform.infrastructure.persistence;

import com.dianshang.platform.openplatform.domain.repository.PluginAppRepository;
import com.dianshang.platform.openplatform.model.PluginApp;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
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
public class JdbcPluginAppRepository implements PluginAppRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPluginAppRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PluginApp save(PluginApp pluginApp) {
        if (pluginApp.appId() == null || pluginApp.appId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO plugin_app (
                            organization_id, app_name, app_type, permission_scope,
                            access_key, secret_masked, status, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseOrganizationId(pluginApp.organizationId()));
                statement.setString(2, pluginApp.appName());
                statement.setString(3, pluginApp.appType());
                statement.setString(4, serializePermissionScope(pluginApp.permissionScope()));
                statement.setString(5, pluginApp.accessKey());
                statement.setString(6, pluginApp.secretMasked());
                statement.setString(7, pluginApp.status());
                statement.setTimestamp(8, toTimestamp(pluginApp.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("plugin_app primary key generate failed");
            }
            return new PluginApp(
                    JdbcIdCodec.formatPluginAppId(key.longValue()),
                    pluginApp.organizationId(),
                    pluginApp.appName(),
                    pluginApp.appType(),
                    pluginApp.permissionScope(),
                    pluginApp.accessKey(),
                    pluginApp.secretMasked(),
                    pluginApp.status(),
                    pluginApp.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE plugin_app
                SET app_name = ?, app_type = ?, permission_scope = ?, access_key = ?,
                    secret_masked = ?, status = ?
                WHERE id = ?
                """,
                pluginApp.appName(),
                pluginApp.appType(),
                serializePermissionScope(pluginApp.permissionScope()),
                pluginApp.accessKey(),
                pluginApp.secretMasked(),
                pluginApp.status(),
                JdbcIdCodec.parsePluginAppId(pluginApp.appId())
        );
        return pluginApp;
    }

    @Override
    public List<PluginApp> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        String placeholders = organizationIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = organizationIds.stream().map(JdbcIdCodec::parseOrganizationId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, organization_id, app_name, app_type, permission_scope,
                       access_key, secret_masked, status, created_at
                FROM plugin_app
                WHERE organization_id IN (%s)
                ORDER BY created_at, id
                """.formatted(placeholders),
                (rs, rowNum) -> new PluginApp(
                        JdbcIdCodec.formatPluginAppId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("app_name"),
                        rs.getString("app_type"),
                        deserializePermissionScope(rs.getString("permission_scope")),
                        rs.getString("access_key"),
                        rs.getString("secret_masked"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                arguments
        );
    }

    @Override
    public Optional<PluginApp> findByAppId(String appId) {
        List<PluginApp> pluginApps = jdbcTemplate.query(
                """
                SELECT id, organization_id, app_name, app_type, permission_scope,
                       access_key, secret_masked, status, created_at
                FROM plugin_app
                WHERE id = ?
                """,
                (rs, rowNum) -> new PluginApp(
                        JdbcIdCodec.formatPluginAppId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("app_name"),
                        rs.getString("app_type"),
                        deserializePermissionScope(rs.getString("permission_scope")),
                        rs.getString("access_key"),
                        rs.getString("secret_masked"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parsePluginAppId(appId)
        );
        return pluginApps.stream().findFirst();
    }

    @Override
    public Optional<PluginApp> findByOrganizationIdAndAppName(String organizationId, String appName) {
        List<PluginApp> pluginApps = jdbcTemplate.query(
                """
                SELECT id, organization_id, app_name, app_type, permission_scope,
                       access_key, secret_masked, status, created_at
                FROM plugin_app
                WHERE organization_id = ? AND app_name = ?
                """,
                (rs, rowNum) -> new PluginApp(
                        JdbcIdCodec.formatPluginAppId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("app_name"),
                        rs.getString("app_type"),
                        deserializePermissionScope(rs.getString("permission_scope")),
                        rs.getString("access_key"),
                        rs.getString("secret_masked"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseOrganizationId(organizationId),
                appName
        );
        return pluginApps.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM plugin_app");
    }

    private String serializePermissionScope(List<String> permissionScope) {
        return permissionScope == null ? "" : String.join(",", permissionScope);
    }

    private List<String> deserializePermissionScope(String permissionScope) {
        if (permissionScope == null || permissionScope.isBlank()) {
            return List.of();
        }
        return List.of(permissionScope.split(","));
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
