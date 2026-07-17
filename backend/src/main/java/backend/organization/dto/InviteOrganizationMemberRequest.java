package backend.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteOrganizationMemberRequest(
        @NotBlank(message = "userId涓嶈兘涓虹┖")
        String userId,
        @NotBlank(message = "userName涓嶈兘涓虹┖")
        String userName,
        String mobile,
        @NotBlank(message = "roleCode涓嶈兘涓虹┖")
        String roleCode
) {
}

