package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanAvatarRepository;
import backend.digitalhuman.model.DigitalHumanAvatar;
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
public class InMemoryDigitalHumanAvatarRepository implements DigitalHumanAvatarRepository {

    private final Map<String, DigitalHumanAvatar> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(12000);

    @Override
    public DigitalHumanAvatar save(DigitalHumanAvatar avatar) {
        String avatarId = avatar.avatarId();
        if (avatarId == null || avatarId.isBlank()) {
            avatarId = "avatar-" + sequence.incrementAndGet();
        }
        DigitalHumanAvatar saved = new DigitalHumanAvatar(
                avatarId,
                avatar.tenantId(),
                avatar.organizationId(),
                avatar.avatarName(),
                avatar.avatarType(),
                avatar.voiceName(),
                avatar.previewUrl(),
                avatar.enabled(),
                avatar.createdAt()
        );
        storage.put(saved.avatarId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanAvatar> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(avatar -> avatar.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanAvatar::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<DigitalHumanAvatar> findById(String tenantId, String avatarId) {
        return Optional.ofNullable(storage.get(avatarId))
                .filter(avatar -> avatar.tenantId().equals(tenantId));
    }

    @Override
    public void deleteById(String tenantId, String avatarId) {
        storage.entrySet().removeIf(entry ->
                entry.getKey().equals(avatarId) && entry.getValue().tenantId().equals(tenantId));
    }
}
