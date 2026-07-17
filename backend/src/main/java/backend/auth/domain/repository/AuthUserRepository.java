package backend.auth.domain.repository;

import backend.auth.model.AuthUser;

import java.util.List;
import java.util.Optional;

public interface AuthUserRepository {

    AuthUser save(AuthUser authUser);

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findByUserId(String userId);

    List<AuthUser> findAll();

    void deleteAll();
}

