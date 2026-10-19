package backend.digitalhuman.model;

import java.time.OffsetDateTime;

/**
 * 数字人的学习记忆条目：每条"未命中知识库"的观众问题都会被记忆，
 * 同类问题出现次数达到阈值后自动提炼进问答知识库，实现数字人自我成长。
 * 状态机：new（刚记住）→ learning（高频，即将转正）→ confirmed（已转正式知识）→ ignored（运营忽略）。
 */
public record DigitalHumanLearningItem(
        String itemId,
        String tenantId,
        String organizationId,
        String sessionId,
        String question,
        String questionType,
        String productName,
        long occurrenceCount,
        String status,
        String faqId,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt
) {
    public DigitalHumanLearningItem withOccurrence(long count, OffsetDateTime seenAt) {
        return new DigitalHumanLearningItem(
                itemId, tenantId, organizationId, sessionId, question, questionType, productName,
                count, status, faqId, seenAt, createdAt
        );
    }

    public DigitalHumanLearningItem withStatus(String newStatus) {
        return new DigitalHumanLearningItem(
                itemId, tenantId, organizationId, sessionId, question, questionType, productName,
                occurrenceCount, newStatus, faqId, lastSeenAt, createdAt
        );
    }

    public DigitalHumanLearningItem withFaq(String newFaqId) {
        return new DigitalHumanLearningItem(
                itemId, tenantId, organizationId, sessionId, question, questionType, productName,
                occurrenceCount, status, newFaqId, lastSeenAt, createdAt
        );
    }
}
