package com.dianshang.platform.auth.dto;

public record LoginResponse(
        String token,
        LoginUserView user
) {
}
