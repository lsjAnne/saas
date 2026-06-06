package com.dianshang.platform.finance.infrastructure.persistence;

import com.dianshang.platform.finance.domain.repository.FinanceBillRepository;
import com.dianshang.platform.finance.model.FinanceBill;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcFinanceBillRepository implements FinanceBillRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFinanceBillRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FinanceBill> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(item -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
                """
                SELECT id, store_id, bill_type, period_start, period_end, income_amount, cost_amount, gross_profit, bill_status, created_at
                FROM finance_bill
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new FinanceBill(
                        JdbcIdCodec.formatFinanceBillId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("bill_type"),
                        rs.getDate("period_start").toLocalDate(),
                        rs.getDate("period_end").toLocalDate(),
                        rs.getBigDecimal("income_amount"),
                        rs.getBigDecimal("cost_amount"),
                        rs.getBigDecimal("gross_profit"),
                        rs.getString("bill_status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray()
        );
    }

    @Override
    public Optional<FinanceBill> findByFinanceBillId(String financeBillId) {
        List<FinanceBill> result = jdbcTemplate.query(
                """
                SELECT id, store_id, bill_type, period_start, period_end, income_amount, cost_amount, gross_profit, bill_status, created_at
                FROM finance_bill
                WHERE id = ?
                """,
                (rs, rowNum) -> new FinanceBill(
                        JdbcIdCodec.formatFinanceBillId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("bill_type"),
                        rs.getDate("period_start").toLocalDate(),
                        rs.getDate("period_end").toLocalDate(),
                        rs.getBigDecimal("income_amount"),
                        rs.getBigDecimal("cost_amount"),
                        rs.getBigDecimal("gross_profit"),
                        rs.getString("bill_status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseFinanceBillId(financeBillId)
        );
        return result.stream().findFirst();
    }

    @Override
    public Optional<FinanceBill> findByStoreAndPeriod(String storeId, String billType, LocalDate periodStart, LocalDate periodEnd) {
        List<FinanceBill> result = jdbcTemplate.query(
                """
                SELECT id, store_id, bill_type, period_start, period_end, income_amount, cost_amount, gross_profit, bill_status, created_at
                FROM finance_bill
                WHERE store_id = ? AND bill_type = ? AND period_start = ? AND period_end = ?
                """,
                (rs, rowNum) -> new FinanceBill(
                        JdbcIdCodec.formatFinanceBillId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("bill_type"),
                        rs.getDate("period_start").toLocalDate(),
                        rs.getDate("period_end").toLocalDate(),
                        rs.getBigDecimal("income_amount"),
                        rs.getBigDecimal("cost_amount"),
                        rs.getBigDecimal("gross_profit"),
                        rs.getString("bill_status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseStoreId(storeId),
                billType,
                periodStart,
                periodEnd
        );
        return result.stream().findFirst();
    }

    @Override
    public FinanceBill save(FinanceBill financeBill) {
        if (financeBill.financeBillId() == null || financeBill.financeBillId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO finance_bill (
                            store_id, bill_type, period_start, period_end, income_amount, cost_amount, gross_profit, bill_status, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(financeBill.storeId()));
                statement.setString(2, financeBill.billType());
                statement.setDate(3, java.sql.Date.valueOf(financeBill.periodStart()));
                statement.setDate(4, java.sql.Date.valueOf(financeBill.periodEnd()));
                statement.setBigDecimal(5, financeBill.incomeAmount());
                statement.setBigDecimal(6, financeBill.costAmount());
                statement.setBigDecimal(7, financeBill.grossProfit());
                statement.setString(8, financeBill.billStatus());
                statement.setTimestamp(9, toTimestamp(financeBill.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("finance_bill primary key generation failed");
            }
            return new FinanceBill(
                    JdbcIdCodec.formatFinanceBillId(key.longValue()),
                    financeBill.storeId(),
                    financeBill.billType(),
                    financeBill.periodStart(),
                    financeBill.periodEnd(),
                    financeBill.incomeAmount(),
                    financeBill.costAmount(),
                    financeBill.grossProfit(),
                    financeBill.billStatus(),
                    financeBill.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE finance_bill
                SET income_amount = ?, cost_amount = ?, gross_profit = ?, bill_status = ?
                WHERE id = ?
                """,
                financeBill.incomeAmount(),
                financeBill.costAmount(),
                financeBill.grossProfit(),
                financeBill.billStatus(),
                JdbcIdCodec.parseFinanceBillId(financeBill.financeBillId())
        );
        return financeBill;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM finance_bill");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
