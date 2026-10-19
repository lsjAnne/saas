package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanScriptRepository;
import backend.digitalhuman.model.DigitalHumanScript;
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
public class InMemoryDigitalHumanScriptRepository implements DigitalHumanScriptRepository {

    private final Map<String, DigitalHumanScript> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(12000);

    @Override
    public DigitalHumanScript save(DigitalHumanScript script) {
        String scriptId = script.scriptId();
        if (scriptId == null || scriptId.isBlank()) {
            scriptId = "dhscript-" + sequence.incrementAndGet();
        }
        DigitalHumanScript saved = new DigitalHumanScript(
                scriptId,
                script.tenantId(),
                script.organizationId(),
                script.scriptName(),
                script.sceneType(),
                script.contentText(),
                script.version(),
                script.status(),
                script.createdAt(),
                script.updatedAt()
        );
        storage.put(saved.scriptId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanScript> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(script -> script.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanScript::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<DigitalHumanScript> findById(String tenantId, String scriptId) {
        return Optional.ofNullable(storage.get(scriptId))
                .filter(script -> script.tenantId().equals(tenantId));
    }
}
