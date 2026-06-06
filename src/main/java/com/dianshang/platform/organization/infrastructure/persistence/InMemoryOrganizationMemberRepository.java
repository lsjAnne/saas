package com.dianshang.platform.organization.infrastructure.persistence;

import com.dianshang.platform.organization.domain.repository.OrganizationMemberRepository;
import com.dianshang.platform.organization.model.OrganizationMember;
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
public class InMemoryOrganizationMemberRepository implements OrganizationMemberRepository {

    private final Map<String, OrganizationMember> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public OrganizationMember save(OrganizationMember member) {
        String memberId = member.memberId();
        if (memberId == null || memberId.isBlank()) {
            memberId = "member-" + sequence.incrementAndGet();
        }
        OrganizationMember saved = new OrganizationMember(
                memberId,
                member.organizationId(),
                member.userId(),
                member.userName(),
                member.mobile(),
                member.roleCode(),
                member.status(),
                member.joinedAt()
        );
        storage.put(saved.memberId(), saved);
        return saved;
    }

    @Override
    public List<OrganizationMember> findByOrganizationId(String organizationId) {
        return storage.values().stream()
                .filter(member -> organizationId.equals(member.organizationId()))
                .sorted(Comparator.comparing(OrganizationMember::joinedAt))
                .toList();
    }

    @Override
    public Optional<OrganizationMember> findByMemberId(String memberId) {
        return Optional.ofNullable(storage.get(memberId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1000);
    }
}
