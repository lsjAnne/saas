package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanInteractionRepository;
import backend.digitalhuman.model.DigitalHumanInteraction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryDigitalHumanInteractionRepository implements DigitalHumanInteractionRepository {

    private final Map<String, DigitalHumanInteraction> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(15000);

    @Override
    public DigitalHumanInteraction save(DigitalHumanInteraction interaction) {
        String interactionId = interaction.interactionId();
        if (interactionId == null || interactionId.isBlank()) {
            interactionId = "dhinteraction-" + sequence.incrementAndGet();
        }
        DigitalHumanInteraction saved = new DigitalHumanInteraction(
                interactionId,
                interaction.tenantId(),
                interaction.organizationId(),
                interaction.sessionId(),
                interaction.barrageId(),
                interaction.hitType(),
                interaction.faqId(),
                interaction.question(),
                interaction.answer(),
                interaction.createdAt()
        );
        storage.put(saved.interactionId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanInteraction> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(interaction -> interaction.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanInteraction::createdAt).reversed())
                .toList();
    }

    @Override
    public List<DigitalHumanInteraction> listBySession(String tenantId, String sessionId) {
        return storage.values().stream()
                .filter(interaction -> interaction.tenantId().equals(tenantId))
                .filter(interaction -> sessionId == null || sessionId.isBlank() || sessionId.equals(interaction.sessionId()))
                .sorted(Comparator.comparing(DigitalHumanInteraction::createdAt).reversed())
                .toList();
    }
}
