package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanSessionRepository;
import backend.digitalhuman.model.DigitalHumanSession;
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
public class InMemoryDigitalHumanSessionRepository implements DigitalHumanSessionRepository {

    private final Map<String, DigitalHumanSession> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(12000);

    @Override
    public DigitalHumanSession save(DigitalHumanSession session) {
        String sessionId = session.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "dhsession-" + sequence.incrementAndGet();
        }
        DigitalHumanSession saved = new DigitalHumanSession(
                sessionId,
                session.tenantId(),
                session.organizationId(),
                session.scheduleId(),
                session.avatarId(),
                session.platformRoomId(),
                session.liveStatus(),
                session.startedAt(),
                session.endedAt(),
                session.viewerPeak(),
                session.orderCount(),
                session.gmvAmount(),
                session.heartbeatAt(),
                session.mockFlag(),
                session.createdAt()
        );
        storage.put(saved.sessionId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanSession> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(session -> session.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanSession::createdAt).reversed())
                .toList();
    }

    @Override
    public List<DigitalHumanSession> listLiveByTenant(String tenantId) {
        return storage.values().stream()
                .filter(session -> session.tenantId().equals(tenantId))
                .filter(session -> "live".equals(session.liveStatus()))
                .sorted(Comparator.comparing(DigitalHumanSession::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<DigitalHumanSession> findById(String tenantId, String sessionId) {
        return Optional.ofNullable(storage.get(sessionId))
                .filter(session -> session.tenantId().equals(tenantId));
    }
}
