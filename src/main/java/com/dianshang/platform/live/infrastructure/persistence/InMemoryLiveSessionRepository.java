package com.dianshang.platform.live.infrastructure.persistence;

import com.dianshang.platform.live.domain.repository.LiveSessionRepository;
import com.dianshang.platform.live.model.LiveSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryLiveSessionRepository implements LiveSessionRepository {

    private final Map<String, LiveSession> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(11200);

    @Override
    public LiveSession save(LiveSession liveSession) {
        String liveSessionId = liveSession.liveSessionId();
        if (liveSessionId == null || liveSessionId.isBlank()) {
            liveSessionId = "live-session-" + sequence.incrementAndGet();
        }
        LiveSession saved = new LiveSession(
                liveSessionId,
                liveSession.livePlanId(),
                liveSession.tenantId(),
                liveSession.storeId(),
                liveSession.liveAccountId(),
                liveSession.sessionStatus(),
                liveSession.roomId(),
                liveSession.actualStartAt(),
                liveSession.actualEndAt(),
                liveSession.errorMessage(),
                liveSession.controlMode(),
                liveSession.currentScene(),
                liveSession.takeoverStatus(),
                liveSession.takeoverOperator(),
                liveSession.promiseAuditStatus(),
                liveSession.promiseAuditRemark(),
                liveSession.createdAt()
        );
        storage.put(saved.liveSessionId(), saved);
        return saved;
    }

    @Override
    public Optional<LiveSession> findRunningByLiveAccountId(String liveAccountId) {
        return storage.values().stream()
                .filter(session -> liveAccountId.equals(session.liveAccountId()))
                .filter(session -> "running".equals(session.sessionStatus()))
                .findFirst();
    }

    @Override
    public List<LiveSession> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(session -> storeIds.contains(session.storeId()))
                .sorted(Comparator.comparing(LiveSession::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<LiveSession> findLatestByLivePlanId(String livePlanId) {
        return storage.values().stream()
                .filter(session -> livePlanId.equals(session.livePlanId()))
                .max(Comparator.comparing(LiveSession::createdAt));
    }

    @Override
    public int countRunningByTenantId(String tenantId) {
        return (int) storage.values().stream()
                .filter(session -> tenantId.equals(session.tenantId()))
                .filter(session -> "running".equals(session.sessionStatus()))
                .count();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(11200);
    }
}
