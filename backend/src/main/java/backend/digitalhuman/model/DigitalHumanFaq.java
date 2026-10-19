package backend.digitalhuman.model;

import java.time.OffsetDateTime;

/**
 * 数字人问答知识库条目：按商品（productName）与问题类型（questionType）维护标准答案，
 * 是数字人弹幕问答"不同产品、不同问题对应不同回答"的知识来源。
 */
public record DigitalHumanFaq(
        String faqId,
        String tenantId,
        String organizationId,
        String productId,
        String productName,
        String questionType,
        String keywords,
        String question,
        String answer,
        boolean enabled,
        long hitCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public DigitalHumanFaq withEditableFields(String productId, String productName, String questionType,
                                              String keywords, String question, String answer, boolean enabled) {
        return new DigitalHumanFaq(
                faqId, tenantId, organizationId, productId, productName, questionType,
                keywords, question, answer, enabled, hitCount, createdAt, OffsetDateTime.now()
        );
    }

    public DigitalHumanFaq withHitCount(long newHitCount) {
        return new DigitalHumanFaq(
                faqId, tenantId, organizationId, productId, productName, questionType,
                keywords, question, answer, enabled, newHitCount, createdAt, updatedAt
        );
    }
}
