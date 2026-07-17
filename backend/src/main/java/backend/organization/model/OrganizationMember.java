package backend.organization.model;

import java.time.OffsetDateTime;

public record OrganizationMember(
        String memberId,
        String organizationId,
        String userId,
        String userName,
        String mobile,
        String roleCode,
        String status,
        OffsetDateTime joinedAt
) {
    public OrganizationMember withRole(String roleCode) {
        return new OrganizationMember(
                memberId,
                organizationId,
                userId,
                userName,
                mobile,
                roleCode,
                status,
                joinedAt
        );
    }
}

