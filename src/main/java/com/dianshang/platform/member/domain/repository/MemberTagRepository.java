package com.dianshang.platform.member.domain.repository;

import com.dianshang.platform.member.model.MemberTag;

import java.util.List;
import java.util.Optional;

public interface MemberTagRepository {

    List<MemberTag> findByStoreIds(List<String> storeIds);

    List<MemberTag> findByMemberId(String memberId);

    Optional<MemberTag> findByMemberTagId(String memberTagId);

    MemberTag save(MemberTag memberTag);

    void deleteByMemberTagId(String memberTagId);

    void deleteAll();
}
