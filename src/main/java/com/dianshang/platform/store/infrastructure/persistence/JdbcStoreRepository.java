package com.dianshang.platform.store.infrastructure.persistence;

import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcStoreRepository implements StoreRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcStoreRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Store save(Store store) {
        if (store.storeId() == null || store.storeId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO store (
                            tenant_id,
                            organization_id,
                            owner_user_id,
                            platform_type,
                            platform_shop_id,
                            shop_name,
                            auth_status,
                            profit_threshold,
                            risk_threshold,
                            default_ship_config,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(store.tenantId()));
                statement.setLong(2, JdbcIdCodec.parseOrganizationId(store.organizationId()));
                statement.setString(3, store.ownerUserId());
                statement.setString(4, store.platformType());
                statement.setString(5, store.platformShopId());
                statement.setString(6, store.shopName());
                statement.setString(7, store.authStatus());
                statement.setBigDecimal(8, store.profitThreshold());
                statement.setBigDecimal(9, store.riskThreshold());
                statement.setString(10, toJson(store.defaultShipConfig()));
                statement.setTimestamp(11, toTimestamp(store.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("store主键生成失败");
            }
            return new Store(
                    JdbcIdCodec.formatStoreId(key.longValue()),
                    store.tenantId(),
                    store.organizationId(),
                    store.ownerUserId(),
                    store.platformType(),
                    store.platformShopId(),
                    store.shopName(),
                    store.authStatus(),
                    store.profitThreshold(),
                    store.riskThreshold(),
                    store.defaultShipConfig(),
                    store.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE store
                SET profit_threshold = ?, risk_threshold = ?, default_ship_config = CAST(? AS JSON)
                WHERE id = ? AND tenant_id = ?
                """,
                store.profitThreshold(),
                store.riskThreshold(),
                toJson(store.defaultShipConfig()),
                JdbcIdCodec.parseStoreId(store.storeId()),
                JdbcIdCodec.parseTenantId(store.tenantId())
        );
        return store;
    }

    @Override
    public List<Store> findByTenantId(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, owner_user_id, platform_type, platform_shop_id,
                       shop_name, auth_status, profit_threshold, risk_threshold, default_ship_config, created_at
                FROM store
                WHERE tenant_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> mapStore(
                        rs.getLong("id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("organization_id"),
                        rs.getString("owner_user_id"),
                        rs.getString("platform_type"),
                        rs.getString("platform_shop_id"),
                        rs.getString("shop_name"),
                        rs.getString("auth_status"),
                        rs.getBigDecimal("profit_threshold"),
                        rs.getBigDecimal("risk_threshold"),
                        rs.getString("default_ship_config"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<Store> findByStoreId(String storeId) {
        List<Store> stores = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, owner_user_id, platform_type, platform_shop_id,
                       shop_name, auth_status, profit_threshold, risk_threshold, default_ship_config, created_at
                FROM store
                WHERE id = ?
                """,
                (rs, rowNum) -> mapStore(
                        rs.getLong("id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("organization_id"),
                        rs.getString("owner_user_id"),
                        rs.getString("platform_type"),
                        rs.getString("platform_shop_id"),
                        rs.getString("shop_name"),
                        rs.getString("auth_status"),
                        rs.getBigDecimal("profit_threshold"),
                        rs.getBigDecimal("risk_threshold"),
                        rs.getString("default_ship_config"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseStoreId(storeId)
        );
        return stores.stream().findFirst();
    }

    @Override
    public Optional<Store> findByTenantAndPlatformShop(String tenantId, String platformType, String platformShopId) {
        List<Store> stores = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_id, owner_user_id, platform_type, platform_shop_id,
                       shop_name, auth_status, profit_threshold, risk_threshold, default_ship_config, created_at
                FROM store
                WHERE tenant_id = ? AND platform_type = ? AND platform_shop_id = ?
                """,
                (rs, rowNum) -> mapStore(
                        rs.getLong("id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("organization_id"),
                        rs.getString("owner_user_id"),
                        rs.getString("platform_type"),
                        rs.getString("platform_shop_id"),
                        rs.getString("shop_name"),
                        rs.getString("auth_status"),
                        rs.getBigDecimal("profit_threshold"),
                        rs.getBigDecimal("risk_threshold"),
                        rs.getString("default_ship_config"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseTenantId(tenantId),
                platformType,
                platformShopId
        );
        return stores.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM store");
    }

    private Store mapStore(long storeId,
                           long tenantId,
                           long organizationId,
                           String ownerUserId,
                           String platformType,
                           String platformShopId,
                           String shopName,
                           String authStatus,
                           BigDecimal profitThreshold,
                           BigDecimal riskThreshold,
                           String defaultShipConfig,
                           Timestamp createdAt) {
        return new Store(
                JdbcIdCodec.formatStoreId(storeId),
                JdbcIdCodec.formatTenantId(tenantId),
                JdbcIdCodec.formatOrganizationId(organizationId),
                ownerUserId,
                platformType,
                platformShopId,
                shopName,
                authStatus,
                profitThreshold,
                riskThreshold,
                fromJson(defaultShipConfig),
                toOffsetDateTime(createdAt)
        );
    }

    private String toJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Map.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("store配置序列化失败", exception);
        }
    }

    private Map<String, Object> fromJson(String values) {
        if (values == null || values.isBlank()) {
            return Map.of();
        }
        try {
            if (objectMapper.readTree(values).isTextual()) {
                return objectMapper.readValue(objectMapper.readTree(values).asText(), MAP_TYPE);
            }
            return objectMapper.readValue(values, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("store配置反序列化失败", exception);
        }
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
