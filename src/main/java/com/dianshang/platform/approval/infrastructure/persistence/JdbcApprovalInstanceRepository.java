package com.dianshang.platform.approval.infrastructure.persistence;

import com.dianshang.platform.approval.domain.repository.ApprovalInstanceRepository;
import com.dianshang.platform.approval.model.ApprovalInstance;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcApprovalInstanceRepository implements ApprovalInstanceRepository {

    private static final RowMapper<ApprovalInstance> ROW_MAPPER = (rs, rowNum) -> new ApprovalInstance(
            JdbcIdCodec.formatApprovalId(rs.getLong("id")),
            JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
            rs.getString("approval_type"),
            rs.getString("related_type"),
            rs.getString("related_id"),
            rs.getString("status"),
            rs.getString("current_handler_id"),
            rs.getString("remark"),
            rs.getString("result_remark"),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcApprovalInstanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ApprovalInstance> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT id, tenant_id, approval_type, related_type, related_id, status, current_handler_id, remark, result_remark, created_at
                        FROM approval_instance
                        WHERE tenant_id = ?
                        ORDER BY created_at DESC, id DESC
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<ApprovalInstance> findByApprovalId(String approvalId) {
        List<ApprovalInstance> matches = jdbcTemplate.query("""
                        SELECT id, tenant_id, approval_type, related_type, related_id, status, current_handler_id, remark, result_remark, created_at
                        FROM approval_instance
                        WHERE id = ?
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseApprovalId(approvalId)
        );
        return matches.stream().findFirst();
    }

    @Override
    public Optional<ApprovalInstance> findPendingByRelated(String tenantId, String relatedType, String relatedId) {
        List<ApprovalInstance> matches = jdbcTemplate.query("""
                        SELECT id, tenant_id, approval_type, related_type, related_id, status, current_handler_id, remark, result_remark, created_at
                        FROM approval_instance
                        WHERE tenant_id = ? AND related_type = ? AND related_id = ? AND status = 'pending'
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                ROW_MAPPER,
                JdbcIdCodec.parseTenantId(tenantId),
                relatedType,
                relatedId
        );
        return matches.stream().findFirst();
    }

    @Override
    public ApprovalInstance save(ApprovalInstance approvalInstance) {
        if (approvalInstance.approvalId() == null || approvalInstance.approvalId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO approval_instance (
                            tenant_id, approval_type, related_type, related_id, status, current_handler_id, remark, result_remark, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, new String[]{"id"});
                statement.setLong(1, JdbcIdCodec.parseTenantId(approvalInstance.tenantId()));
                statement.setString(2, approvalInstance.approvalType());
                statement.setString(3, approvalInstance.relatedType());
                statement.setString(4, approvalInstance.relatedId());
                statement.setString(5, approvalInstance.status());
                statement.setString(6, approvalInstance.currentHandlerId());
                statement.setString(7, approvalInstance.remark());
                statement.setString(8, approvalInstance.resultRemark());
                statement.setTimestamp(9, toTimestamp(approvalInstance.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("approval_instance primary key generation failed");
            }
            return new ApprovalInstance(
                    JdbcIdCodec.formatApprovalId(key.longValue()),
                    approvalInstance.tenantId(),
                    approvalInstance.approvalType(),
                    approvalInstance.relatedType(),
                    approvalInstance.relatedId(),
                    approvalInstance.status(),
                    approvalInstance.currentHandlerId(),
                    approvalInstance.remark(),
                    approvalInstance.resultRemark(),
                    approvalInstance.createdAt()
            );
        }

        jdbcTemplate.update("""
                        UPDATE approval_instance
                        SET approval_type = ?, related_type = ?, related_id = ?, status = ?, current_handler_id = ?, remark = ?, result_remark = ?
                        WHERE id = ?
                        """,
                approvalInstance.approvalType(),
                approvalInstance.relatedType(),
                approvalInstance.relatedId(),
                approvalInstance.status(),
                approvalInstance.currentHandlerId(),
                approvalInstance.remark(),
                approvalInstance.resultRemark(),
                JdbcIdCodec.parseApprovalId(approvalInstance.approvalId())
        );
        return approvalInstance;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM approval_instance");
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
