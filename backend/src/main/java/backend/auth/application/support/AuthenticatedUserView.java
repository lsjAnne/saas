package backend.auth.application.support;

import backend.auth.model.AuthUser;

import java.util.Set;

public record AuthenticatedUserView(
        AuthUser authUser,
        Set<String> permissionCodes
) {
}

