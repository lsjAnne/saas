package backend.member.infrastructure.persistence;

import backend.member.domain.repository.MemberProfileRepository;
import backend.member.model.MemberProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryMemberProfileRepository implements MemberProfileRepository {

    private final Map<String, MemberProfile> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<MemberProfile> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(member -> storeIds.contains(member.storeId()))
                .sorted(Comparator.comparing(MemberProfile::createdAt).reversed()
                        .thenComparing(MemberProfile::memberId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<MemberProfile> findByMemberId(String memberId) {
        return Optional.ofNullable(storage.get(memberId));
    }

    @Override
    public Optional<MemberProfile> findByStoreAndCustomerId(String storeId, String customerId) {
        return storage.values().stream()
                .filter(member -> storeId.equals(member.storeId()) && customerId.equals(member.customerId()))
                .findFirst();
    }

    @Override
    public MemberProfile save(MemberProfile memberProfile) {
        MemberProfile stored = memberProfile;
        if (memberProfile.memberId() == null || memberProfile.memberId().isBlank()) {
            stored = new MemberProfile(
                    "member-" + sequence.getAndIncrement(),
                    memberProfile.storeId(),
                    memberProfile.customerId(),
                    memberProfile.nickname(),
                    memberProfile.levelCode(),
                    memberProfile.totalOrderCount(),
                    memberProfile.totalPaidAmount(),
                    memberProfile.lastOrderAt(),
                    memberProfile.createdAt()
            );
        }
        storage.put(stored.memberId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}

