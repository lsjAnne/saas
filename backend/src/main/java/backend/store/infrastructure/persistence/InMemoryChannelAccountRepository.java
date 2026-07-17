package backend.store.infrastructure.persistence;

import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.model.ChannelAccount;
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
public class InMemoryChannelAccountRepository implements ChannelAccountRepository {

    private final Map<String, ChannelAccount> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(4000);

    @Override
    public ChannelAccount save(ChannelAccount channelAccount) {
        String channelAccountId = channelAccount.channelAccountId();
        if (channelAccountId == null || channelAccountId.isBlank()) {
            channelAccountId = "channel-" + sequence.incrementAndGet();
        }
        ChannelAccount saved = new ChannelAccount(
                channelAccountId,
                channelAccount.organizationId(),
                channelAccount.channelType(),
                channelAccount.accountName(),
                channelAccount.authStatus(),
                channelAccount.expiresAt(),
                channelAccount.extraConfig(),
                channelAccount.createdAt()
        );
        storage.put(saved.channelAccountId(), saved);
        return saved;
    }

    @Override
    public List<ChannelAccount> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        Set<String> organizationIdSet = Set.copyOf(organizationIds);
        return storage.values().stream()
                .filter(account -> organizationIdSet.contains(account.organizationId()))
                .sorted(Comparator.comparing(ChannelAccount::createdAt))
                .toList();
    }

    @Override
    public Optional<ChannelAccount> findByChannelAccountId(String channelAccountId) {
        return Optional.ofNullable(storage.get(channelAccountId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(4000);
    }
}

