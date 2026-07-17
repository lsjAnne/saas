package backend.member.domain.repository;

import backend.member.model.MemberProfile;

import java.util.List;
import java.util.Optional;

public interface MemberProfileRepository {

    List<MemberProfile> findByStoreIds(List<String> storeIds);

    Optional<MemberProfile> findByMemberId(String memberId);

    Optional<MemberProfile> findByStoreAndCustomerId(String storeId, String customerId);

    MemberProfile save(MemberProfile memberProfile);

    void deleteAll();
}

