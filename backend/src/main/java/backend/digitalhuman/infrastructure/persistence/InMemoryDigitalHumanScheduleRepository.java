package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanScheduleRepository;
import backend.digitalhuman.model.DigitalHumanSchedule;
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
public class InMemoryDigitalHumanScheduleRepository implements DigitalHumanScheduleRepository {

    private final Map<String, DigitalHumanSchedule> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(12000);

    @Override
    public DigitalHumanSchedule save(DigitalHumanSchedule schedule) {
        String scheduleId = schedule.scheduleId();
        if (scheduleId == null || scheduleId.isBlank()) {
            scheduleId = "dhschedule-" + sequence.incrementAndGet();
        }
        DigitalHumanSchedule saved = new DigitalHumanSchedule(
                scheduleId,
                schedule.tenantId(),
                schedule.organizationId(),
                schedule.scheduleName(),
                schedule.avatarId(),
                schedule.scriptId(),
                schedule.platformType(),
                schedule.repeatType(),
                schedule.startAt(),
                schedule.endAt(),
                schedule.enabled(),
                schedule.status(),
                schedule.createdAt()
        );
        storage.put(saved.scheduleId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanSchedule> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(schedule -> schedule.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanSchedule::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<DigitalHumanSchedule> findById(String tenantId, String scheduleId) {
        return Optional.ofNullable(storage.get(scheduleId))
                .filter(schedule -> schedule.tenantId().equals(tenantId));
    }
}
