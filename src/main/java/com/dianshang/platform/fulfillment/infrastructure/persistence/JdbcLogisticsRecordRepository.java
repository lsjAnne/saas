package com.dianshang.platform.fulfillment.infrastructure.persistence;

import com.dianshang.platform.fulfillment.domain.repository.LogisticsRecordRepository;
import com.dianshang.platform.fulfillment.model.LogisticsRecord;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
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
public class JdbcLogisticsRecordRepository implements LogisticsRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLogisticsRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LogisticsRecord save(LogisticsRecord logisticsRecord) {
        if (logisticsRecord.logisticsRecordId() == null || logisticsRecord.logisticsRecordId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO logistics_record (
                            fulfillment_task_id, tracking_number, logistics_company, logistics_status, synced_at, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseFulfillmentTaskId(logisticsRecord.fulfillmentTaskId()));
                statement.setString(2, logisticsRecord.trackingNumber());
                statement.setString(3, logisticsRecord.logisticsCompany());
                statement.setString(4, logisticsRecord.logisticsStatus());
                statement.setTimestamp(5, toTimestamp(logisticsRecord.syncedAt()));
                statement.setTimestamp(6, toTimestamp(logisticsRecord.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("logistics_record主键生成失败");
            }
            return new LogisticsRecord(
                    JdbcIdCodec.formatLogisticsRecordId(key.longValue()),
                    logisticsRecord.fulfillmentTaskId(),
                    logisticsRecord.trackingNumber(),
                    logisticsRecord.logisticsCompany(),
                    logisticsRecord.logisticsStatus(),
                    logisticsRecord.syncedAt(),
                    logisticsRecord.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE logistics_record
                SET tracking_number = ?, logistics_company = ?, logistics_status = ?, synced_at = ?
                WHERE id = ?
                """,
                logisticsRecord.trackingNumber(),
                logisticsRecord.logisticsCompany(),
                logisticsRecord.logisticsStatus(),
                toTimestamp(logisticsRecord.syncedAt()),
                JdbcIdCodec.parseLogisticsRecordId(logisticsRecord.logisticsRecordId())
        );
        return logisticsRecord;
    }

    @Override
    public List<LogisticsRecord> findByFulfillmentTaskId(String fulfillmentTaskId) {
        return jdbcTemplate.query(
                """
                SELECT id, fulfillment_task_id, tracking_number, logistics_company, logistics_status, synced_at, created_at
                FROM logistics_record
                WHERE fulfillment_task_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new LogisticsRecord(
                        JdbcIdCodec.formatLogisticsRecordId(rs.getLong("id")),
                        JdbcIdCodec.formatFulfillmentTaskId(rs.getLong("fulfillment_task_id")),
                        rs.getString("tracking_number"),
                        rs.getString("logistics_company"),
                        rs.getString("logistics_status"),
                        toOffsetDateTime(rs.getTimestamp("synced_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseFulfillmentTaskId(fulfillmentTaskId)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM logistics_record");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
