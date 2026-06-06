package com.dianshang.platform.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrganizationMemberRoleRequest(
        @NotBlank(message = "roleCode不能为空")
        String roleCode
) {
}
