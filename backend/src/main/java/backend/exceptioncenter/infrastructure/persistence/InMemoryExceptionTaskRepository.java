package backend.exceptioncenter.infrastructure.persistence;

import backend.exceptioncenter.domain.repository.ExceptionTaskRepository;
import backend.exceptioncenter.model.ExceptionTask;
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
public class InMemoryExceptionTaskRepository implements ExceptionTaskRepository {

    private final Map<String, ExceptionTask> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9400);

    @Override
    public ExceptionTask save(ExceptionTask exceptionTask) {
        String exceptionTaskId = exceptionTask.exceptionTaskId();
        if (exceptionTaskId == null || exceptionTaskId.isBlank()) {
            exceptionTaskId = "exception-" + sequence.incrementAndGet();
        }
        ExceptionTask saved = new ExceptionTask(
                exceptionTaskId,
                exceptionTask.storeId(),
                exceptionTask.relatedType(),
                exceptionTask.relatedId(),
                exceptionTask.exceptionType(),
                exceptionTask.severity(),
                exceptionTask.status(),
                exceptionTask.suggestionText(),
                exceptionTask.ownerUserId(),
                exceptionTask.createdAt()
        );
        storage.put(saved.exceptionTaskId(), saved);
        return saved;
    }

    @Override
    public List<ExceptionTask> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(task -> storeIds.contains(task.storeId()))
                .sorted(Comparator.comparing(ExceptionTask::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<ExceptionTask> findByExceptionTaskId(String exceptionTaskId) {
        return Optional.ofNullable(storage.get(exceptionTaskId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9400);
    }
}

