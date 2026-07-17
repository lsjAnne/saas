package backend.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrganizationMemberRoleRequest(
        @NotBlank(message = "roleCode涓嶈兘涓虹┖")
        String roleCode
) {
}

