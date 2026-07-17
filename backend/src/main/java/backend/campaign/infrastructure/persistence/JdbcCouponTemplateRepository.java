package backend.campaign.infrastructure.persistence;

import backend.campaign.domain.repository.CouponTemplateRepository;
import backend.campaign.model.CouponTemplate;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcCouponTemplateRepository implements CouponTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCouponTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CouponTemplate> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
                """
                SELECT id, store_id, template_name, discount_type, discount_value, threshold_amount, status, created_at
                FROM coupon_template
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new CouponTemplate(
                        JdbcIdCodec.formatCouponTemplateId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("template_name"),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("discount_value"),
                        rs.getBigDecimal("threshold_amount"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray()
        );
    }

    @Override
    public Optional<CouponTemplate> findByCouponTemplateId(String couponTemplateId) {
        List<CouponTemplate> results = jdbcTemplate.query(
                """
                SELECT id, store_id, template_name, discount_type, discount_value, threshold_amount, status, created_at
                FROM coupon_template
                WHERE id = ?
                """,
                (rs, rowNum) -> new CouponTemplate(
                        JdbcIdCodec.formatCouponTemplateId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("template_name"),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("discount_value"),
                        rs.getBigDecimal("threshold_amount"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseCouponTemplateId(couponTemplateId)
        );
        return results.stream().findFirst();
    }

    @Override
    public CouponTemplate save(CouponTemplate couponTemplate) {
        if (couponTemplate.couponTemplateId() == null || couponTemplate.couponTemplateId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO coupon_template (
                            store_id, template_name, discount_type, discount_value, threshold_amount, status, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(couponTemplate.storeId()));
                statement.setString(2, couponTemplate.templateName());
                statement.setString(3, couponTemplate.discountType());
                statement.setBigDecimal(4, couponTemplate.discountValue());
                statement.setBigDecimal(5, couponTemplate.thresholdAmount());
                statement.setString(6, couponTemplate.status());
                statement.setTimestamp(7, toTimestamp(couponTemplate.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("coupon_template primary key generation failed");
            }
            return new CouponTemplate(
                    JdbcIdCodec.formatCouponTemplateId(key.longValue()),
                    couponTemplate.storeId(),
                    couponTemplate.templateName(),
                    couponTemplate.discountType(),
                    couponTemplate.discountValue(),
                    couponTemplate.thresholdAmount(),
                    couponTemplate.status(),
                    couponTemplate.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE coupon_template
                SET template_name = ?, discount_type = ?, discount_value = ?, threshold_amount = ?, status = ?
                WHERE id = ?
                """,
                couponTemplate.templateName(),
                couponTemplate.discountType(),
                couponTemplate.discountValue(),
                couponTemplate.thresholdAmount(),
                couponTemplate.status(),
                JdbcIdCodec.parseCouponTemplateId(couponTemplate.couponTemplateId())
        );
        return couponTemplate;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM coupon_template");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

