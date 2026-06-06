package com.dianshang.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username不能为空")
        String username,
        @NotBlank(message = "password不能为空")
        String password
) {
}
