package backend.member.model;

import java.time.OffsetDateTime;

public record MemberTag(
        String memberTagId,
        String storeId,
        String memberId,
        String tagCode,
        String tagName,
        String sourceType,
        OffsetDateTime createdAt
) {
}

