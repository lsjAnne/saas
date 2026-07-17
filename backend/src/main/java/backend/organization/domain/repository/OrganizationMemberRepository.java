package backend.organization.domain.repository;

import backend.organization.model.OrganizationMember;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository {

    OrganizationMember save(OrganizationMember member);

    List<OrganizationMember> findByOrganizationId(String organizationId);

    Optional<OrganizationMember> findByMemberId(String memberId);

    void deleteAll();
}

