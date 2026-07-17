package backend.openplatform.infrastructure.persistence;

import backend.openplatform.domain.repository.OpenPlatformCallLogRepository;
import backend.openplatform.model.OpenPlatformCallLog;
import backend.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcOpenPlatformCallLogRepository implements OpenPlatformCallLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOpenPlatformCallLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OpenPlatformCallLog save(OpenPlatformCallLog callLog) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO openplatform_call_log (
                        tenant_id, organization_id, app_id, subscription_id, request_id,
                        endpoint, direction, source_module, result_status, signature_verified,
                        replayed, trace_id, message, created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[]{"id"}
            );
            statement.setString(1, callLog.tenantId());
            statement.setString(2, callLog.organizationId());
            statement.setString(3, callLog.appId());
            statement.setString(4, callLog.subscriptionId());
            statement.setString(5, callLog.requestId());
            statement.setString(6, callLog.endpoint());
            statement.setString(7, callLog.direction());
            statement.setString(8, callLog.sourceModule());
            statement.setString(9, callLog.resultStatus());
            statement.setBoolean(10, callLog.signatureVerified());
            statement.setBoolean(11, callLog.replayed());
            statement.setString(12, callLog.traceId());
            statement.setString(13, callLog.message());
            statement.setTimestamp(14, toTimestamp(callLog.createdAt()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("openplatform_call_log primary key generate failed");
        }
        return new OpenPlatformCallLog(
                JdbcIdCodec.formatOpenPlatformCallLogId(key.longValue()),
                callLog.tenantId(),
                callLog.organizationId(),
                callLog.appId(),
                callLog.subscriptionId(),
                callLog.requestId(),
                callLog.endpoint(),
                callLog.direction(),
                callLog.sourceModule(),
                callLog.resultStatus(),
                callLog.signatureVerified(),
                callLog.replayed(),
                callLog.traceId(),
                callLog.message(),
                callLog.createdAt()
        );
    }

    @Override
    public List<OpenPlatformCallLog> findByTenantId(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, app_id, subscription_id, request_id,
                       endpoint, direction, source_module, result_status, signature_verified,
                       replayed, trace_id, message, created_at
                FROM openplatform_call_log
                WHERE tenant_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new OpenPlatformCallLog(
                        JdbcIdCodec.formatOpenPlatformCallLogId(rs.getLong("id")),
                        rs.getString("tenant_id"),
                        rs.getString("organization_id"),
                        rs.getString("app_id"),
                        rs.getString("subscription_id"),
                        rs.getString("request_id"),
                        rs.getString("endpoint"),
                        rs.getString("direction"),
                        rs.getString("source_module"),
                        rs.getString("result_status"),
                        rs.getBoolean("signature_verified"),
                        rs.getBoolean("replayed"),
                        rs.getString("trace_id"),
                        rs.getString("message"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                tenantId
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM openplatform_call_log");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

