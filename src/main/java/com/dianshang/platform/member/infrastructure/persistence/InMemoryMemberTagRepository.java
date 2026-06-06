package com.dianshang.platform.member.infrastructure.persistence;

import com.dianshang.platform.member.domain.repository.MemberTagRepository;
import com.dianshang.platform.member.model.MemberTag;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
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
public class InMemoryMemberTagRepository implements MemberTagRepository {

    private final Map<String, MemberTag> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<MemberTag> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(tag -> storeIds.contains(tag.storeId()))
                .sorted(Comparator.comparing(MemberTag::createdAt).reversed()
                        .thenComparing(MemberTag::memberTagId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<MemberTag> findByMemberId(String memberId) {
        return storage.values().stream()
                .filter(tag -> memberId.equals(tag.memberId()))
                .sorted(Comparator.comparing(MemberTag::createdAt).thenComparing(MemberTag::memberTagId))
                .toList();
    }

    @Override
    public Optional<MemberTag> findByMemberTagId(String memberTagId) {
        return Optional.ofNullable(storage.get(memberTagId));
    }

    @Override
    public MemberTag save(MemberTag memberTag) {
        MemberTag stored = memberTag;
        if (memberTag.memberTagId() == null || memberTag.memberTagId().isBlank()) {
            stored = new MemberTag(
                    JdbcIdCodec.formatMemberTagId(sequence.getAndIncrement()),
                    memberTag.storeId(),
                    memberTag.memberId(),
                    memberTag.tagCode(),
                    memberTag.tagName(),
                    memberTag.sourceType(),
                    memberTag.createdAt()
            );
        }
        storage.put(stored.memberTagId(), stored);
        return stored;
    }

    @Override
    public void deleteByMemberTagId(String memberTagId) {
        storage.remove(memberTagId);
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}
