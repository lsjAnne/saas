package com.dianshang.platform.openplatform.infrastructure.persistence;

import com.dianshang.platform.openplatform.domain.repository.IntegrationCredentialRepository;
import com.dianshang.platform.openplatform.model.IntegrationCredential;
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

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcIntegrationCredentialRepository implements IntegrationCredentialRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcIntegrationCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public IntegrationCredential save(IntegrationCredential integrationCredential) {
        if (integrationCredential.credentialId() == null || integrationCredential.credentialId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO integration_credential (
                            plugin_app_id, credential_type, access_key, secret_digest,
                            secret_key_mask, expires_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parsePluginAppId(integrationCredential.pluginAppId()));
                statement.setString(2, integrationCredential.credentialType());
                statement.setString(3, integrationCredential.accessKey());
                statement.setString(4, integrationCredential.secretDigest());
                statement.setString(5, integrationCredential.secretKeyMasked());
                statement.setTimestamp(6, toTimestamp(integrationCredential.expiresAt()));
                statement.setTimestamp(7, toTimestamp(integrationCredential.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("integration_credential primary key generate failed");
            }
            return new IntegrationCredential(
                    JdbcIdCodec.formatIntegrationCredentialId(key.longValue()),
                    integrationCredential.pluginAppId(),
                    integrationCredential.credentialType(),
                    integrationCredential.accessKey(),
                    integrationCredential.secretDigest(),
                    integrationCredential.secretKeyMasked(),
                    integrationCredential.expiresAt(),
                    integrationCredential.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE integration_credential
                SET access_key = ?, secret_digest = ?, secret_key_mask = ?,
                    expires_at = ?, created_at = ?
                WHERE id = ?
                """,
                integrationCredential.accessKey(),
                integrationCredential.secretDigest(),
                integrationCredential.secretKeyMasked(),
                toTimestamp(integrationCredential.expiresAt()),
                toTimestamp(integrationCredential.createdAt()),
                JdbcIdCodec.parseIntegrationCredentialId(integrationCredential.credentialId())
        );
        return integrationCredential;
    }

    @Override
    public Optional<IntegrationCredential> findByPluginAppIdAndCredentialType(String pluginAppId, String credentialType) {
        List<IntegrationCredential> credentials = jdbcTemplate.query(
                """
                SELECT id, plugin_app_id, credential_type, access_key, secret_digest,
                       secret_key_mask, expires_at, created_at
                FROM integration_credential
                WHERE plugin_app_id = ? AND credential_type = ?
                """,
                (rs, rowNum) -> new IntegrationCredential(
                        JdbcIdCodec.formatIntegrationCredentialId(rs.getLong("id")),
                        JdbcIdCodec.formatPluginAppId(rs.getLong("plugin_app_id")),
                        rs.getString("credential_type"),
                        rs.getString("access_key"),
                        rs.getString("secret_digest"),
                        rs.getString("secret_key_mask"),
                        toOffsetDateTime(rs.getTimestamp("expires_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parsePluginAppId(pluginAppId),
                credentialType
        );
        return credentials.stream().findFirst();
    }

    @Override
    public Optional<IntegrationCredential> findByAccessKey(String accessKey) {
        List<IntegrationCredential> credentials = jdbcTemplate.query(
                """
                SELECT id, plugin_app_id, credential_type, access_key, secret_digest,
                       secret_key_mask, expires_at, created_at
                FROM integration_credential
                WHERE access_key = ?
                """,
                (rs, rowNum) -> new IntegrationCredential(
                        JdbcIdCodec.formatIntegrationCredentialId(rs.getLong("id")),
                        JdbcIdCodec.formatPluginAppId(rs.getLong("plugin_app_id")),
                        rs.getString("credential_type"),
                        rs.getString("access_key"),
                        rs.getString("secret_digest"),
                        rs.getString("secret_key_mask"),
                        toOffsetDateTime(rs.getTimestamp("expires_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                accessKey
        );
        return credentials.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM integration_credential");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
