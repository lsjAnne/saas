package backend.auth.dto;

public record LoginResponse(
        String token,
        LoginUserView user
) {
}

