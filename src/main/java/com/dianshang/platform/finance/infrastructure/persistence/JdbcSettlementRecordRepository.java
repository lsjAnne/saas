package com.dianshang.platform.finance.infrastructure.persistence;

import com.dianshang.platform.finance.domain.repository.SettlementRecordRepository;
import com.dianshang.platform.finance.model.SettlementRecord;
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
public class JdbcSettlementRecordRepository implements SettlementRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSettlementRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SettlementRecord> findByFinanceBillId(String financeBillId) {
        return jdbcTemplate.query(
                """
                SELECT id, finance_bill_id, settlement_type, settlement_amount, settlement_status, settled_at, created_at
                FROM settlement_record
                WHERE finance_bill_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new SettlementRecord(
                        JdbcIdCodec.formatSettlementRecordId(rs.getLong("id")),
                        JdbcIdCodec.formatFinanceBillId(rs.getLong("finance_bill_id")),
                        rs.getString("settlement_type"),
                        rs.getBigDecimal("settlement_amount"),
                        rs.getString("settlement_status"),
                        toOffsetDateTime(rs.getTimestamp("settled_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseFinanceBillId(financeBillId)
        );
    }

    @Override
    public SettlementRecord save(SettlementRecord settlementRecord) {
        if (settlementRecord.settlementRecordId() == null || settlementRecord.settlementRecordId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO settlement_record (
                            finance_bill_id, settlement_type, settlement_amount, settlement_status, settled_at, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseFinanceBillId(settlementRecord.financeBillId()));
                statement.setString(2, settlementRecord.settlementType());
                statement.setBigDecimal(3, settlementRecord.settlementAmount());
                statement.setString(4, settlementRecord.settlementStatus());
                statement.setTimestamp(5, toTimestamp(settlementRecord.settledAt()));
                statement.setTimestamp(6, toTimestamp(settlementRecord.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("settlement_record primary key generation failed");
            }
            return new SettlementRecord(
                    JdbcIdCodec.formatSettlementRecordId(key.longValue()),
                    settlementRecord.financeBillId(),
                    settlementRecord.settlementType(),
                    settlementRecord.settlementAmount(),
                    settlementRecord.settlementStatus(),
                    settlementRecord.settledAt(),
                    settlementRecord.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE settlement_record
                SET settlement_type = ?, settlement_amount = ?, settlement_status = ?, settled_at = ?
                WHERE id = ?
                """,
                settlementRecord.settlementType(),
                settlementRecord.settlementAmount(),
                settlementRecord.settlementStatus(),
                toTimestamp(settlementRecord.settledAt()),
                JdbcIdCodec.parseSettlementRecordId(settlementRecord.settlementRecordId())
        );
        return settlementRecord;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM settlement_record");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
