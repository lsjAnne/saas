package backend.auth.infrastructure.persistence;

import backend.auth.domain.repository.AuthRolePermissionRepository;
import backend.auth.model.AuthRolePermission;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAuthRolePermissionRepository implements AuthRolePermissionRepository {

    private final Map<String, List<AuthRolePermission>> permissionsByRole = new ConcurrentHashMap<>();

    @Override
    public void saveAll(List<AuthRolePermission> permissions) {
        permissionsByRole.clear();
        for (AuthRolePermission permission : permissions) {
            permissionsByRole.computeIfAbsent(permission.roleCode(), key -> new ArrayList<>()).add(permission);
        }
    }

    @Override
    public List<AuthRolePermission> findByRoleCode(String roleCode) {
        return new ArrayList<>(permissionsByRole.getOrDefault(roleCode, List.of()));
    }

    @Override
    public void deleteAll() {
        permissionsByRole.clear();
    }
}

