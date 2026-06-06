package com.dianshang.platform.auth.infrastructure.persistence;

import com.dianshang.platform.auth.domain.repository.AuthUserRepository;
import com.dianshang.platform.auth.model.AuthUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAuthUserRepository implements AuthUserRepository {

    private final Map<String, AuthUser> byUserId = new ConcurrentHashMap<>();
    private final Map<String, String> userIdByUsername = new ConcurrentHashMap<>();

    @Override
    public AuthUser save(AuthUser authUser) {
        byUserId.put(authUser.userId(), authUser);
        userIdByUsername.put(authUser.username(), authUser.userId());
        return authUser;
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        String userId = userIdByUsername.get(username);
        return userId == null ? Optional.empty() : Optional.ofNullable(byUserId.get(userId));
    }

    @Override
    public Optional<AuthUser> findByUserId(String userId) {
        return Optional.ofNullable(byUserId.get(userId));
    }

    @Override
    public List<AuthUser> findAll() {
        return new ArrayList<>(byUserId.values());
    }

    @Override
    public void deleteAll() {
        byUserId.clear();
        userIdByUsername.clear();
    }
}
