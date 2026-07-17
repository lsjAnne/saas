package backend.saas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record QuotaConsumeRequest(
        @NotBlank(message = "quotaCode涓嶈兘涓虹┖")
        String quotaCode,
        @Min(value = 1, message = "amount鏈€灏忎负1")
        int amount
) {
}

