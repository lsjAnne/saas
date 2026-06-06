package com.dianshang.platform.support.infrastructure.persistence;

import com.dianshang.platform.support.domain.repository.SupportSessionRepository;
import com.dianshang.platform.support.model.SupportSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcSupportSessionRepository implements SupportSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSupportSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SupportSession save(SupportSession supportSession) {
        int updated = jdbcTemplate.update(
                """
                UPDATE support_session
                SET tenant_id = ?, requester_id = ?, reason = ?, approver = ?, expires_at = ?, status = ?, approval_remark = ?, active = ?, created_at = ?
                WHERE id = ?
                """,
                supportSession.tenantId(),
                supportSession.requesterId(),
                supportSession.reason(),
                supportSession.approver(),
                toTimestamp(supportSession.expiresAt()),
                supportSession.status(),
                supportSession.approvalRemark(),
                supportSession.active(),
                toTimestamp(supportSession.createdAt()),
                supportSession.id()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO support_session (id, tenant_id, requester_id, reason, approver, expires_at, status, approval_remark, active, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    supportSession.id(),
                    supportSession.tenantId(),
                    supportSession.requesterId(),
                    supportSession.reason(),
                    supportSession.approver(),
                    toTimestamp(supportSession.expiresAt()),
                    supportSession.status(),
                    supportSession.approvalRemark(),
                    supportSession.active(),
                    toTimestamp(supportSession.createdAt())
            );
        }
        return supportSession;
    }

    @Override
    public List<SupportSession> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, requester_id, reason, approver, expires_at, status, approval_remark, active, created_at
                FROM support_session
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new SupportSession(
                        rs.getString("id"),
                        rs.getString("tenant_id"),
                        rs.getString("requester_id"),
                        rs.getString("reason"),
                        rs.getString("approver"),
                        toOffsetDateTime(rs.getTimestamp("expires_at")),
                        rs.getString("status"),
                        rs.getString("approval_remark"),
                        rs.getBoolean("active"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                )
        );
    }

    @Override
    public Optional<SupportSession> findById(String id) {
        List<SupportSession> sessions = jdbcTemplate.query(
                """
                SELECT id, tenant_id, requester_id, reason, approver, expires_at, status, approval_remark, active, created_at
                FROM support_session
                WHERE id = ?
                """,
                (rs, rowNum) -> new SupportSession(
                        rs.getString("id"),
                        rs.getString("tenant_id"),
                        rs.getString("requester_id"),
                        rs.getString("reason"),
                        rs.getString("approver"),
                        toOffsetDateTime(rs.getTimestamp("expires_at")),
                        rs.getString("status"),
                        rs.getString("approval_remark"),
                        rs.getBoolean("active"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                id
        );
        return sessions.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM support_session");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
