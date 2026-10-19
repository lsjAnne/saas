package backend.ai;

import backend.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL pgvector 实现：FAQ 语义向量持久化在 faq_embedding 表，
 * 检索使用 pgvector 原生余弦距离运算符（<=>），由数据库完成近邻搜索。
 *
 * 启用条件（application.yml）：
 *   app.ai.vector.store: postgres
 * 前置要求：PostgreSQL 数据库已启用 pgvector 扩展并执行 V029 注释中的 DDL：
 *   CREATE EXTENSION IF NOT EXISTS vector;
 *   CREATE TABLE IF NOT EXISTS faq_embedding (faq_id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, embedding vector(256) NOT NULL);
 *
 * FAQ 增删改时需同步维护 faq_embedding（由上层 FaqVectorSync 负责，未启用本实现时无副作用）。
 */
@Component
@ConditionalOnProperty(name = "app.ai.vector.store", havingValue = "postgres")
public class PgVectorFaqSemanticSearch implements FaqSemanticSearch {

    private final JdbcTemplate jdbcTemplate;

    public PgVectorFaqSemanticSearch(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SemanticHit> searchSimilar(String tenantId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String vectorLiteral = VectorFeature.toPgVectorString(VectorFeature.encode(query));
        return jdbcTemplate.query(
                """
                SELECT e.faq_id, (e.embedding <=> ?::vector) AS distance
                FROM faq_embedding e
                WHERE e.tenant_id = ?
                ORDER BY distance ASC
                LIMIT ?
                """,
                (rs, rowNum) -> {
                    float distance = rs.getFloat("distance");
                    return new SemanticHit(
                            JdbcIdCodec.formatDigitalHumanFaqId(rs.getLong("faq_id")),
                            Math.max(0f, 1f - distance)
                    );
                },
                vectorLiteral,
                JdbcIdCodec.parseTenantId(tenantId),
                topK
        );
    }
}
