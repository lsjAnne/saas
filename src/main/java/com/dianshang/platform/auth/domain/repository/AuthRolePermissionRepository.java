package com.dianshang.platform.auth.domain.repository;

import com.dianshang.platform.auth.model.AuthRolePermission;

import java.util.List;

public interface AuthRolePermissionRepository {

    void saveAll(List<AuthRolePermission> permissions);

    List<AuthRolePermission> findByRoleCode(String roleCode);

    void deleteAll();
}
