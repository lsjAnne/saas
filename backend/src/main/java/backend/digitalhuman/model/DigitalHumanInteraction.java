package backend.digitalhuman.model;

import java.time.OffsetDateTime;

/**
 * 数字人对一条弹幕的回答记录。hitType 标识回答来源：
 * faq（命中问答库）/ fallback（兜底话术）/ llm（预留大模型生成）。
 */
public record DigitalHumanInteraction(
        String interactionId,
        String tenantId,
        String organizationId,
        String sessionId,
        String barrageId,
        String hitType,
        String faqId,
        String question,
        String answer,
        OffsetDateTime createdAt
) {
}
