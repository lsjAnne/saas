package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.TenantDataExportTaskRepository;
import backend.saas.model.TenantDataExportTaskRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcTenantDataExportTaskRepository implements TenantDataExportTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantDataExportTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantDataExportTaskRecord save(TenantDataExportTaskRecord task) {
        int updated = jdbcTemplate.update("""
                UPDATE tenant_data_export_task
                SET tenant_id = ?, scope_code = ?, scope_name = ?, requested_by = ?, status = ?, file_name = ?,
                    content_type = ?, export_content = ?, masking_strategy = ?, time_range_start = ?, time_range_end = ?,
                    download_expires_at = ?, created_at = ?, completed_at = ?
                WHERE export_task_id = ?
                """,
                task.tenantId(),
                task.scopeCode(),
                task.scopeName(),
                task.requestedBy(),
                task.status(),
                task.fileName(),
                task.contentType(),
                task.exportContent(),
                task.maskingStrategy(),
                toTimestamp(task.timeRangeStart()),
                toTimestamp(task.timeRangeEnd()),
                toTimestamp(task.downloadExpiresAt()),
                toTimestamp(task.createdAt()),
                toTimestamp(task.completedAt()),
                task.exportTaskId()
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO tenant_data_export_task (
                        export_task_id, tenant_id, scope_code, scope_name, requested_by, status, file_name,
                        content_type, export_content, masking_strategy, time_range_start, time_range_end,
                        download_expires_at, created_at, completed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    task.exportTaskId(),
                    task.tenantId(),
                    task.scopeCode(),
                    task.scopeName(),
                    task.requestedBy(),
                    task.status(),
                    task.fileName(),
                    task.contentType(),
                    task.exportContent(),
                    task.maskingStrategy(),
                    toTimestamp(task.timeRangeStart()),
                    toTimestamp(task.timeRangeEnd()),
                    toTimestamp(task.downloadExpiresAt()),
                    toTimestamp(task.createdAt()),
                    toTimestamp(task.completedAt())
            );
        }
        return task;
    }

    @Override
    public List<TenantDataExportTaskRecord> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT export_task_id, tenant_id, scope_code, scope_name, requested_by, status, file_name,
                       content_type, export_content, masking_strategy, time_range_start, time_range_end,
                       download_expires_at, created_at, completed_at
                FROM tenant_data_export_task
                WHERE tenant_id = ?
                ORDER BY created_at DESC, export_task_id DESC
                """, (rs, rowNum) -> new TenantDataExportTaskRecord(
                rs.getString("export_task_id"),
                rs.getString("tenant_id"),
                rs.getString("scope_code"),
                rs.getString("scope_name"),
                rs.getString("requested_by"),
                rs.getString("status"),
                rs.getString("file_name"),
                rs.getString("content_type"),
                rs.getString("export_content"),
                rs.getString("masking_strategy"),
                fromTimestamp(rs.getTimestamp("time_range_start")),
                fromTimestamp(rs.getTimestamp("time_range_end")),
                fromTimestamp(rs.getTimestamp("download_expires_at")),
                fromTimestamp(rs.getTimestamp("created_at")),
                fromTimestamp(rs.getTimestamp("completed_at"))
        ), tenantId);
    }

    @Override
    public Optional<TenantDataExportTaskRecord> findByTaskId(String exportTaskId) {
        List<TenantDataExportTaskRecord> tasks = jdbcTemplate.query("""
                SELECT export_task_id, tenant_id, scope_code, scope_name, requested_by, status, file_name,
                       content_type, export_content, masking_strategy, time_range_start, time_range_end,
                       download_expires_at, created_at, completed_at
                FROM tenant_data_export_task
                WHERE export_task_id = ?
                """, (rs, rowNum) -> new TenantDataExportTaskRecord(
                rs.getString("export_task_id"),
                rs.getString("tenant_id"),
                rs.getString("scope_code"),
                rs.getString("scope_name"),
                rs.getString("requested_by"),
                rs.getString("status"),
                rs.getString("file_name"),
                rs.getString("content_type"),
                rs.getString("export_content"),
                rs.getString("masking_strategy"),
                fromTimestamp(rs.getTimestamp("time_range_start")),
                fromTimestamp(rs.getTimestamp("time_range_end")),
                fromTimestamp(rs.getTimestamp("download_expires_at")),
                fromTimestamp(rs.getTimestamp("created_at")),
                fromTimestamp(rs.getTimestamp("completed_at"))
        ), exportTaskId);
        return tasks.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tenant_data_export_task");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime fromTimestamp(Timestamp value) {
        return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}

