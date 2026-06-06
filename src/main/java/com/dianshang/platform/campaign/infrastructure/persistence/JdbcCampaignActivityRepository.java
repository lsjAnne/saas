package com.dianshang.platform.campaign.infrastructure.persistence;

import com.dianshang.platform.campaign.domain.repository.CampaignActivityRepository;
import com.dianshang.platform.campaign.model.CampaignActivity;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcCampaignActivityRepository implements CampaignActivityRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCampaignActivityRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CampaignActivity> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
                """
                SELECT id, store_id, activity_type, activity_name, status, start_at, end_at,
                       product_ids_json, rule_json, coupon_template_id, need_approval, created_at
                FROM campaign_activity
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new CampaignActivity(
                        JdbcIdCodec.formatCampaignId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("activity_type"),
                        rs.getString("activity_name"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("start_at")),
                        toOffsetDateTime(rs.getTimestamp("end_at")),
                        fromStringListJson(rs.getString("product_ids_json")),
                        fromMapJson(rs.getString("rule_json")),
                        parseCouponTemplateId(rs.getObject("coupon_template_id")),
                        rs.getBoolean("need_approval"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray()
        );
    }

    @Override
    public Optional<CampaignActivity> findByCampaignId(String campaignId) {
        List<CampaignActivity> results = jdbcTemplate.query(
                """
                SELECT id, store_id, activity_type, activity_name, status, start_at, end_at,
                       product_ids_json, rule_json, coupon_template_id, need_approval, created_at
                FROM campaign_activity
                WHERE id = ?
                """,
                (rs, rowNum) -> new CampaignActivity(
                        JdbcIdCodec.formatCampaignId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("activity_type"),
                        rs.getString("activity_name"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("start_at")),
                        toOffsetDateTime(rs.getTimestamp("end_at")),
                        fromStringListJson(rs.getString("product_ids_json")),
                        fromMapJson(rs.getString("rule_json")),
                        parseCouponTemplateId(rs.getObject("coupon_template_id")),
                        rs.getBoolean("need_approval"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseCampaignId(campaignId)
        );
        return results.stream().findFirst();
    }

    @Override
    public CampaignActivity save(CampaignActivity campaignActivity) {
        if (campaignActivity.campaignId() == null || campaignActivity.campaignId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO campaign_activity (
                            store_id, activity_type, activity_name, status, start_at, end_at,
                            product_ids_json, rule_json, coupon_template_id, need_approval, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(campaignActivity.storeId()));
                statement.setString(2, campaignActivity.activityType());
                statement.setString(3, campaignActivity.activityName());
                statement.setString(4, campaignActivity.status());
                statement.setTimestamp(5, toTimestamp(campaignActivity.startAt()));
                statement.setTimestamp(6, toTimestamp(campaignActivity.endAt()));
                statement.setString(7, toJson(campaignActivity.productIds()));
                statement.setString(8, toJson(campaignActivity.rule()));
                if (campaignActivity.couponTemplateId() == null || campaignActivity.couponTemplateId().isBlank()) {
                    statement.setObject(9, null);
                } else {
                    statement.setLong(9, JdbcIdCodec.parseCouponTemplateId(campaignActivity.couponTemplateId()));
                }
                statement.setBoolean(10, campaignActivity.needApproval());
                statement.setTimestamp(11, toTimestamp(campaignActivity.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("campaign_activity primary key generation failed");
            }
            return new CampaignActivity(
                    JdbcIdCodec.formatCampaignId(key.longValue()),
                    campaignActivity.storeId(),
                    campaignActivity.activityType(),
                    campaignActivity.activityName(),
                    campaignActivity.status(),
                    campaignActivity.startAt(),
                    campaignActivity.endAt(),
                    campaignActivity.productIds(),
                    campaignActivity.rule(),
                    campaignActivity.couponTemplateId(),
                    campaignActivity.needApproval(),
                    campaignActivity.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE campaign_activity
                SET activity_type = ?, activity_name = ?, status = ?, start_at = ?, end_at = ?,
                    product_ids_json = ?, rule_json = ?, coupon_template_id = ?, need_approval = ?
                WHERE id = ?
                """,
                campaignActivity.activityType(),
                campaignActivity.activityName(),
                campaignActivity.status(),
                toTimestamp(campaignActivity.startAt()),
                toTimestamp(campaignActivity.endAt()),
                toJson(campaignActivity.productIds()),
                toJson(campaignActivity.rule()),
                campaignActivity.couponTemplateId() == null || campaignActivity.couponTemplateId().isBlank()
                        ? null
                        : JdbcIdCodec.parseCouponTemplateId(campaignActivity.couponTemplateId()),
                campaignActivity.needApproval(),
                JdbcIdCodec.parseCampaignId(campaignActivity.campaignId())
        );
        return campaignActivity;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM campaign_activity");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("campaign json serialization failed", exception);
        }
    }

    private List<String> fromStringListJson(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("campaign productIds deserialization failed", exception);
        }
    }

    private Map<String, Object> fromMapJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("campaign rule deserialization failed", exception);
        }
    }

    private String parseCouponTemplateId(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        long numericValue = rawValue instanceof Number number
                ? number.longValue()
                : Long.parseLong(rawValue.toString());
        return JdbcIdCodec.formatCouponTemplateId(numericValue);
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
