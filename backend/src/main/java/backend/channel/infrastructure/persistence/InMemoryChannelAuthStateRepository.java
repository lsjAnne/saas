package backend.channel.infrastructure.persistence;

import backend.channel.domain.repository.ChannelAuthStateRepository;
import backend.channel.model.ChannelAuthState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryChannelAuthStateRepository implements ChannelAuthStateRepository {

    private final Map<String, ChannelAuthState> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(6000);

    @Override
    public ChannelAuthState save(ChannelAuthState authState) {
        String authStateId = authState.authStateId();
        if (authStateId == null || authStateId.isBlank()) {
            authStateId = "channel-state-" + sequence.incrementAndGet();
        }
        ChannelAuthState saved = new ChannelAuthState(
                authStateId,
                authState.tenantId(),
                authState.organizationId(),
                authState.channelType(),
                authState.channelAccountId(),
                authState.state(),
                authState.redirectUri(),
                authState.createdAt(),
                authState.expiresAt()
        );
        storage.put(saved.authStateId(), saved);
        return saved;
    }

    @Override
    public Optional<ChannelAuthState> findByState(String state) {
        return storage.values().stream()
                .filter(item -> item.state().equals(state))
                .findFirst();
    }

    @Override
    public void deleteByState(String state) {
        storage.values().removeIf(item -> item.state().equals(state));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(6000);
    }
}
