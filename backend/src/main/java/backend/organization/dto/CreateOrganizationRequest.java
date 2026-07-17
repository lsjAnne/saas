package backend.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(
        @NotBlank(message = "organizationName涓嶈兘涓虹┖")
        String organizationName
) {
}

