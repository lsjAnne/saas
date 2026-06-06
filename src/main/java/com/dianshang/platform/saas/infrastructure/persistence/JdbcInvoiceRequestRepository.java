package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.InvoiceRequestRepository;
import com.dianshang.platform.saas.model.InvoiceRequest;
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
public class JdbcInvoiceRequestRepository implements InvoiceRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcInvoiceRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public InvoiceRequest save(InvoiceRequest invoiceRequest) {
        if (invoiceRequest.invoiceRequestId() == null || invoiceRequest.invoiceRequestId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO invoice_request (tenant_id, billing_order_id, invoice_title, invoice_tax_no, invoice_status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(invoiceRequest.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseBillingOrderId(invoiceRequest.billingOrderId()));
                statement.setString(3, invoiceRequest.invoiceTitle());
                statement.setString(4, invoiceRequest.invoiceTaxNo());
                statement.setString(5, invoiceRequest.invoiceStatus());
                statement.setTimestamp(6, toTimestamp(invoiceRequest.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("invoice_request 主键生成失败");
            }
            return new InvoiceRequest(
                    JdbcIdCodec.formatInvoiceRequestId(key.longValue()),
                    invoiceRequest.tenantId(),
                    invoiceRequest.billingOrderId(),
                    invoiceRequest.invoiceTitle(),
                    invoiceRequest.invoiceTaxNo(),
                    invoiceRequest.invoiceStatus(),
                    invoiceRequest.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE invoice_request
                SET invoice_title = ?, invoice_tax_no = ?, invoice_status = ?
                WHERE id = ? AND tenant_id = ?
                """,
                invoiceRequest.invoiceTitle(),
                invoiceRequest.invoiceTaxNo(),
                invoiceRequest.invoiceStatus(),
                JdbcIdCodec.parseInvoiceRequestId(invoiceRequest.invoiceRequestId()),
                JdbcIdCodec.parseTenantId(invoiceRequest.tenantId())
        );
        return invoiceRequest;
    }

    @Override
    public List<InvoiceRequest> findByTenantId(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, billing_order_id, invoice_title, invoice_tax_no, invoice_status, created_at
                FROM invoice_request
                WHERE tenant_id = ?
                ORDER BY created_at DESC, id DESC
                """,
                (rs, rowNum) -> new InvoiceRequest(
                        JdbcIdCodec.formatInvoiceRequestId(rs.getLong("id")),
                        JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                        JdbcIdCodec.formatBillingOrderId(rs.getLong("billing_order_id")),
                        rs.getString("invoice_title"),
                        rs.getString("invoice_tax_no"),
                        rs.getString("invoice_status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM invoice_request");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
