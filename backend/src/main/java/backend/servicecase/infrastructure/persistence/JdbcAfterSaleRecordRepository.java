package backend.servicecase.infrastructure.persistence;

import backend.saas.infrastructure.persistence.JdbcIdCodec;
import backend.servicecase.domain.repository.AfterSaleRecordRepository;
import backend.servicecase.model.AfterSaleRecord;
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
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcAfterSaleRecordRepository implements AfterSaleRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAfterSaleRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AfterSaleRecord save(AfterSaleRecord afterSaleRecord) {
        if (afterSaleRecord.afterSaleId() == null || afterSaleRecord.afterSaleId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO after_sale_record (
                            order_id, after_sale_type, reason_text, status, evidence_blob, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseOrderId(afterSaleRecord.orderId()));
                statement.setString(2, afterSaleRecord.afterSaleType());
                statement.setString(3, afterSaleRecord.reasonText());
                statement.setString(4, afterSaleRecord.status());
                statement.setString(5, afterSaleRecord.evidenceBlob());
                statement.setTimestamp(6, toTimestamp(afterSaleRecord.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("after_sale_record涓婚敭鐢熸垚澶辫触");
            }
            return new AfterSaleRecord(
                    JdbcIdCodec.formatAfterSaleId(key.longValue()),
                    afterSaleRecord.orderId(),
                    afterSaleRecord.afterSaleType(),
                    afterSaleRecord.reasonText(),
                    afterSaleRecord.status(),
                    afterSaleRecord.evidenceBlob(),
                    afterSaleRecord.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE after_sale_record
                SET after_sale_type = ?, reason_text = ?, status = ?, evidence_blob = ?
                WHERE id = ?
                """,
                afterSaleRecord.afterSaleType(),
                afterSaleRecord.reasonText(),
                afterSaleRecord.status(),
                afterSaleRecord.evidenceBlob(),
                JdbcIdCodec.parseAfterSaleId(afterSaleRecord.afterSaleId())
        );
        return afterSaleRecord;
    }

    @Override
    public Optional<AfterSaleRecord> findByAfterSaleId(String afterSaleId) {
        List<AfterSaleRecord> records = jdbcTemplate.query(
                """
                SELECT id, order_id, after_sale_type, reason_text, status, evidence_blob, created_at
                FROM after_sale_record
                WHERE id = ?
                """,
                (rs, rowNum) -> new AfterSaleRecord(
                        JdbcIdCodec.formatAfterSaleId(rs.getLong("id")),
                        JdbcIdCodec.formatOrderId(rs.getLong("order_id")),
                        rs.getString("after_sale_type"),
                        rs.getString("reason_text"),
                        rs.getString("status"),
                        rs.getString("evidence_blob"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseAfterSaleId(afterSaleId)
        );
        return records.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM after_sale_record");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

