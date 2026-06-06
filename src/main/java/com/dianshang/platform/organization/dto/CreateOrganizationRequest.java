package com.dianshang.platform.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(
        @NotBlank(message = "organizationName不能为空")
        String organizationName
) {
}
