package com.dianshang.platform.auth;

import com.dianshang.platform.auth.model.AuthUser;

import java.util.Set;

public record AuthenticatedUserView(
        AuthUser authUser,
        Set<String> permissionCodes
) {
}
