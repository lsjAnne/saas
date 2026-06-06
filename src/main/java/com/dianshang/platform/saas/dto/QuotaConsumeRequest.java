package com.dianshang.platform.saas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record QuotaConsumeRequest(
        @NotBlank(message = "quotaCode不能为空")
        String quotaCode,
        @Min(value = 1, message = "amount最小为1")
        int amount
) {
}
