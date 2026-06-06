package com.dianshang.platform.support.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSupportSessionRequest(
        @NotBlank(message = "tenantId不能为空")
        String tenantId,
        @NotBlank(message = "reason不能为空")
        String reason,
        @NotBlank(message = "approver不能为空")
        String approver,
        @NotBlank(message = "expiresAt不能为空")
        String expiresAt
) {
}
