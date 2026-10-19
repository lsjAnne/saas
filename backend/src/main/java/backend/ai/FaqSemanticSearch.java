package backend.ai;

import java.util.List;

/**
 * FAQ 语义检索端口：输入问题文本，返回语义最相近的知识库条目（向量检索）。
 *
 * 实现选择：
 *  - MemoryFaqSemanticSearch（默认，app.ai.vector.store=memory）：内置 Java 向量引擎，零外部依赖；
 *  - PgVectorFaqSemanticSearch（app.ai.vector.store=postgres）：PostgreSQL pgvector 原生检索。
 *
 * 下游（问答路由、学习记忆聚类）只依赖本端口，不感知向量来源。
 */
public interface FaqSemanticSearch {

    /**
     * 检索与 query 语义最相近的 FAQ。
     *
     * @param tenantId 租户
     * @param query    问题文本（弹幕内容）
     * @param topK     返回条数
     * @return 按相似度降序的候选（含相似度分数），空列表表示无候选
     */
    List<SemanticHit> searchSimilar(String tenantId, String query, int topK);

    record SemanticHit(String faqId, float score) {
    }
}
