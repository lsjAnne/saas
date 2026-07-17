package backend.auth.infrastructure.persistence;

import backend.auth.domain.repository.AuthRolePermissionRepository;
import backend.auth.model.AuthRolePermission;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcAuthRolePermissionRepository implements AuthRolePermissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthRolePermissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveAll(List<AuthRolePermission> permissions) {
        jdbcTemplate.update("DELETE FROM auth_role_permission");
        for (AuthRolePermission permission : permissions) {
            jdbcTemplate.update(
                    "INSERT INTO auth_role_permission (role_code, permission_code) VALUES (?, ?)",
                    permission.roleCode(),
                    permission.permissionCode()
            );
        }
    }

    @Override
    public List<AuthRolePermission> findByRoleCode(String roleCode) {
        return jdbcTemplate.query(
                """
                SELECT role_code, permission_code
                FROM auth_role_permission
                WHERE role_code = ?
                ORDER BY permission_code
                """,
                (rs, rowNum) -> new AuthRolePermission(
                        rs.getString("role_code"),
                        rs.getString("permission_code")
                ),
                roleCode
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM auth_role_permission");
    }
}

