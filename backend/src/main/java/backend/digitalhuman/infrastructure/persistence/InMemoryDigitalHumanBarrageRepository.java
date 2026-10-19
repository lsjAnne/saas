package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanBarrageRepository;
import backend.digitalhuman.model.DigitalHumanBarrage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryDigitalHumanBarrageRepository implements DigitalHumanBarrageRepository {

    private final Map<String, DigitalHumanBarrage> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(14000);

    @Override
    public DigitalHumanBarrage save(DigitalHumanBarrage barrage) {
        String barrageId = barrage.barrageId();
        if (barrageId == null || barrageId.isBlank()) {
            barrageId = "dhbarrage-" + sequence.incrementAndGet();
        }
        DigitalHumanBarrage saved = new DigitalHumanBarrage(
                barrageId,
                barrage.tenantId(),
                barrage.organizationId(),
                barrage.sessionId(),
                barrage.platformType(),
                barrage.userNick(),
                barrage.content(),
                barrage.replyStatus(),
                barrage.createdAt()
        );
        storage.put(saved.barrageId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanBarrage> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(barrage -> barrage.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanBarrage::createdAt).reversed())
                .toList();
    }

    @Override
    public List<DigitalHumanBarrage> listBySession(String tenantId, String sessionId) {
        return storage.values().stream()
                .filter(barrage -> barrage.tenantId().equals(tenantId))
                .filter(barrage -> sessionId == null || sessionId.isBlank() || sessionId.equals(barrage.sessionId()))
                .sorted(Comparator.comparing(DigitalHumanBarrage::createdAt).reversed())
                .toList();
    }
}
