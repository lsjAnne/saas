package backend.auth.domain.repository;

import backend.auth.model.AuthRolePermission;

import java.util.List;

public interface AuthRolePermissionRepository {

    void saveAll(List<AuthRolePermission> permissions);

    List<AuthRolePermission> findByRoleCode(String roleCode);

    void deleteAll();
}

