package com.dianshang.platform.organization.domain.repository;

import com.dianshang.platform.organization.model.OrganizationMember;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository {

    OrganizationMember save(OrganizationMember member);

    List<OrganizationMember> findByOrganizationId(String organizationId);

    Optional<OrganizationMember> findByMemberId(String memberId);

    void deleteAll();
}
