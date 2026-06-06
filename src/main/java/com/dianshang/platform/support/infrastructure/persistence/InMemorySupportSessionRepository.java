package com.dianshang.platform.support.infrastructure.persistence;

import com.dianshang.platform.support.domain.repository.SupportSessionRepository;
import com.dianshang.platform.support.model.SupportSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemorySupportSessionRepository implements SupportSessionRepository {

    private final Map<String, SupportSession> storage = new ConcurrentHashMap<>();

    @Override
    public SupportSession save(SupportSession supportSession) {
        storage.put(supportSession.id(), supportSession);
        return supportSession;
    }

    @Override
    public List<SupportSession> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(SupportSession::createdAt))
                .toList();
    }

    @Override
    public Optional<SupportSession> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
