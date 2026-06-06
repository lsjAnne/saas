package com.dianshang.platform.auth.infrastructure.persistence;

import com.dianshang.platform.auth.domain.repository.AuthUserRepository;
import com.dianshang.platform.auth.model.AuthUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcAuthUserRepository implements AuthUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AuthUser save(AuthUser authUser) {
        int updated = jdbcTemplate.update(
                """
                UPDATE auth_user
                SET username = ?, password_hash = ?, display_name = ?, role_code = ?, operator_type = ?,
                    tenant_id = ?, organization_id = ?, status = ?, created_at = ?, password_updated_at = ?, last_login_at = ?
                WHERE user_id = ?
                """,
                authUser.username(),
                authUser.passwordHash(),
                authUser.displayName(),
                authUser.roleCode(),
                authUser.operatorType(),
                authUser.tenantId(),
                authUser.organizationId(),
                authUser.status(),
                toTimestamp(authUser.createdAt()),
                toTimestamp(authUser.passwordUpdatedAt()),
                toTimestamp(authUser.lastLoginAt()),
                authUser.userId()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO auth_user (
                        user_id, username, password_hash, display_name, role_code, operator_type,
                        tenant_id, organization_id, status, created_at, password_updated_at, last_login_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    authUser.userId(),
                    authUser.username(),
                    authUser.passwordHash(),
                    authUser.displayName(),
                    authUser.roleCode(),
                    authUser.operatorType(),
                    authUser.tenantId(),
                    authUser.organizationId(),
                    authUser.status(),
                    toTimestamp(authUser.createdAt()),
                    toTimestamp(authUser.passwordUpdatedAt()),
                    toTimestamp(authUser.lastLoginAt())
            );
        }
        return authUser;
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        List<AuthUser> users = jdbcTemplate.query(
                """
                SELECT user_id, username, password_hash, display_name, role_code, operator_type,
                       tenant_id, organization_id, status, created_at, password_updated_at, last_login_at
                FROM auth_user
                WHERE username = ?
                """,
                (rs, rowNum) -> mapAuthUser(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("role_code"),
                        rs.getString("operator_type"),
                        rs.getString("tenant_id"),
                        rs.getString("organization_id"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("password_updated_at"),
                        rs.getTimestamp("last_login_at")
                ),
                username
        );
        return users.stream().findFirst();
    }

    @Override
    public Optional<AuthUser> findByUserId(String userId) {
        List<AuthUser> users = jdbcTemplate.query(
                """
                SELECT user_id, username, password_hash, display_name, role_code, operator_type,
                       tenant_id, organization_id, status, created_at, password_updated_at, last_login_at
                FROM auth_user
                WHERE user_id = ?
                """,
                (rs, rowNum) -> mapAuthUser(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("role_code"),
                        rs.getString("operator_type"),
                        rs.getString("tenant_id"),
                        rs.getString("organization_id"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("password_updated_at"),
                        rs.getTimestamp("last_login_at")
                ),
                userId
        );
        return users.stream().findFirst();
    }

    @Override
    public List<AuthUser> findAll() {
        return jdbcTemplate.query(
                """
                SELECT user_id, username, password_hash, display_name, role_code, operator_type,
                       tenant_id, organization_id, status, created_at, password_updated_at, last_login_at
                FROM auth_user
                ORDER BY created_at, user_id
                """,
                (rs, rowNum) -> mapAuthUser(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("role_code"),
                        rs.getString("operator_type"),
                        rs.getString("tenant_id"),
                        rs.getString("organization_id"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("password_updated_at"),
                        rs.getTimestamp("last_login_at")
                )
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM auth_user");
    }

    private AuthUser mapAuthUser(String userId,
                                 String username,
                                 String passwordHash,
                                 String displayName,
                                 String roleCode,
                                 String operatorType,
                                 String tenantId,
                                 String organizationId,
                                 String status,
                                 Timestamp createdAt,
                                 Timestamp passwordUpdatedAt,
                                 Timestamp lastLoginAt) {
        return new AuthUser(
                userId,
                username,
                passwordHash,
                displayName,
                roleCode,
                operatorType,
                tenantId,
                organizationId,
                status,
                toOffsetDateTime(createdAt),
                toOffsetDateTime(passwordUpdatedAt),
                toOffsetDateTime(lastLoginAt)
        );
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
