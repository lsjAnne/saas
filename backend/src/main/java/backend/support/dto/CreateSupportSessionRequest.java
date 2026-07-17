package backend.support.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSupportSessionRequest(
        @NotBlank(message = "tenantId涓嶈兘涓虹┖")
        String tenantId,
        @NotBlank(message = "reason涓嶈兘涓虹┖")
        String reason,
        @NotBlank(message = "approver涓嶈兘涓虹┖")
        String approver,
        @NotBlank(message = "expiresAt涓嶈兘涓虹┖")
        String expiresAt
) {
}

