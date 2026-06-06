package com.dianshang.platform.saas.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterTenantRequest(
        @NotBlank(message = "tenantName不能为空")
        String tenantName,
        @NotBlank(message = "ownerName不能为空")
        String ownerName,
        @NotBlank(message = "mobile不能为空")
        String mobile
) {
}
