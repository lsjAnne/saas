package backend.member.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MemberProfile(
        String memberId,
        String storeId,
        String customerId,
        String nickname,
        String levelCode,
        int totalOrderCount,
        BigDecimal totalPaidAmount,
        OffsetDateTime lastOrderAt,
        OffsetDateTime createdAt
) {
}

