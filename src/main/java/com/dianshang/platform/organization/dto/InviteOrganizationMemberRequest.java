package com.dianshang.platform.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteOrganizationMemberRequest(
        @NotBlank(message = "userId不能为空")
        String userId,
        @NotBlank(message = "userName不能为空")
        String userName,
        String mobile,
        @NotBlank(message = "roleCode不能为空")
        String roleCode
) {
}
