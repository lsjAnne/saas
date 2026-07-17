package backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username涓嶈兘涓虹┖")
        String username,
        @NotBlank(message = "password涓嶈兘涓虹┖")
        String password
) {
}

