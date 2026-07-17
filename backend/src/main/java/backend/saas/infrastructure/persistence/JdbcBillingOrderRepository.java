package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.BillingOrderRepository;
import backend.saas.model.BillingOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcBillingOrderRepository implements BillingOrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBillingOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BillingOrder save(BillingOrder billingOrder) {
        if (billingOrder.billingOrderId() == null || billingOrder.billingOrderId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO billing_order (tenant_id, plan_id, order_type, payable_amount, payment_status, external_order_no, paid_at, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(billingOrder.tenantId()));
                setNullableLong(statement, 2, mapPlanId(billingOrder.planCode()));
                statement.setString(3, billingOrder.orderType());
                statement.setBigDecimal(4, billingOrder.payableAmount());
                statement.setString(5, billingOrder.paymentStatus());
                statement.setString(6, billingOrder.externalOrderNo());
                statement.setTimestamp(7, toTimestamp(billingOrder.paidAt()));
                statement.setTimestamp(8, toTimestamp(billingOrder.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("billing_order 涓婚敭鐢熸垚澶辫触");
            }
            return new BillingOrder(
                    JdbcIdCodec.formatBillingOrderId(key.longValue()),
                    billingOrder.tenantId(),
                    billingOrder.planCode(),
                    billingOrder.planName(),
                    billingOrder.orderType(),
                    billingOrder.payableAmount(),
                    billingOrder.paymentStatus(),
                    billingOrder.externalOrderNo(),
                    billingOrder.paidAt(),
                    billingOrder.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE billing_order
                SET plan_id = ?, order_type = ?, payable_amount = ?, payment_status = ?, external_order_no = ?, paid_at = ?
                WHERE id = ? AND tenant_id = ?
                """,
                mapPlanId(billingOrder.planCode()),
                billingOrder.orderType(),
                billingOrder.payableAmount(),
                billingOrder.paymentStatus(),
                billingOrder.externalOrderNo(),
                toTimestamp(billingOrder.paidAt()),
                JdbcIdCodec.parseBillingOrderId(billingOrder.billingOrderId()),
                JdbcIdCodec.parseTenantId(billingOrder.tenantId())
        );
        return billingOrder;
    }

    @Override
    public List<BillingOrder> findByTenantId(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT bo.id, bo.tenant_id, bo.order_type, bo.payable_amount, bo.payment_status, bo.external_order_no, bo.paid_at, bo.created_at,
                       sp.plan_code, sp.plan_name
                FROM billing_order bo
                LEFT JOIN subscription_plan sp ON sp.id = bo.plan_id
                WHERE bo.tenant_id = ?
                ORDER BY bo.created_at DESC, bo.id DESC
                """,
                (rs, rowNum) -> new BillingOrder(
                        JdbcIdCodec.formatBillingOrderId(rs.getLong("id")),
                        JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                        rs.getString("plan_code"),
                        rs.getString("plan_name"),
                        rs.getString("order_type"),
                        rs.getBigDecimal("payable_amount"),
                        rs.getString("payment_status"),
                        rs.getString("external_order_no"),
                        toOffsetDateTime(rs.getTimestamp("paid_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<BillingOrder> findByBillingOrderId(String billingOrderId) {
        List<BillingOrder> results = jdbcTemplate.query(
                """
                SELECT bo.id, bo.tenant_id, bo.order_type, bo.payable_amount, bo.payment_status, bo.external_order_no, bo.paid_at, bo.created_at,
                       sp.plan_code, sp.plan_name
                FROM billing_order bo
                LEFT JOIN subscription_plan sp ON sp.id = bo.plan_id
                WHERE bo.id = ?
                """,
                (rs, rowNum) -> new BillingOrder(
                        JdbcIdCodec.formatBillingOrderId(rs.getLong("id")),
                        JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                        rs.getString("plan_code"),
                        rs.getString("plan_name"),
                        rs.getString("order_type"),
                        rs.getBigDecimal("payable_amount"),
                        rs.getString("payment_status"),
                        rs.getString("external_order_no"),
                        toOffsetDateTime(rs.getTimestamp("paid_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseBillingOrderId(billingOrderId)
        );
        return results.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM billing_order");
    }

    private Long mapPlanId(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return null;
        }
        return switch (planCode) {
            case "trial" -> 1L;
            case "basic" -> 2L;
            case "pro" -> 3L;
            default -> 100L + Math.abs(planCode.hashCode());
        };
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value);
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

