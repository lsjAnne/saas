package backend.channel.infrastructure.persistence;

import backend.channel.domain.repository.ChannelTokenRepository;
import backend.channel.model.ChannelToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryChannelTokenRepository implements ChannelTokenRepository {

    private final Map<String, ChannelToken> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(6000);

    @Override
    public ChannelToken save(ChannelToken token) {
        String channelTokenId = token.channelTokenId();
        if (channelTokenId == null || channelTokenId.isBlank()) {
            channelTokenId = "channel-token-" + sequence.incrementAndGet();
        }
        ChannelToken saved = new ChannelToken(
                channelTokenId,
                token.tenantId(),
                token.organizationId(),
                token.channelType(),
                token.accountName(),
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.scope(),
                token.expiresAt(),
                token.refreshExpiresAt(),
                token.extraConfig(),
                token.createdAt(),
                token.updatedAt()
        );
        storage.put(saved.channelTokenId(), saved);
        return saved;
    }

    @Override
    public Optional<ChannelToken> findByChannelTokenId(String channelTokenId) {
        return Optional.ofNullable(storage.get(channelTokenId));
    }

    @Override
    public Optional<ChannelToken> findByOrganizationAndTypeAndAccount(String organizationId, String channelType, String accountName) {
        return storage.values().stream()
                .filter(item -> item.organizationId().equals(organizationId))
                .filter(item -> item.channelType().equals(channelType))
                .filter(item -> item.accountName().equals(accountName))
                .findFirst();
    }

    @Override
    public List<ChannelToken> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        Set<String> organizationIdSet = Set.copyOf(organizationIds);
        return storage.values().stream()
                .filter(item -> organizationIdSet.contains(item.organizationId()))
                .sorted(Comparator.comparing(ChannelToken::createdAt))
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(6000);
    }
}
