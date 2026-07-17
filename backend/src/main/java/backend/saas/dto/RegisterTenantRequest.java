package backend.saas.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterTenantRequest(
        @NotBlank(message = "tenantName涓嶈兘涓虹┖")
        String tenantName,
        @NotBlank(message = "ownerName涓嶈兘涓虹┖")
        String ownerName,
        @NotBlank(message = "mobile涓嶈兘涓虹┖")
        String mobile
) {
}

