package com.dianshang.platform.organization.infrastructure.persistence;

import com.dianshang.platform.organization.domain.repository.OrganizationRepository;
import com.dianshang.platform.organization.model.Organization;
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
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcOrganizationRepository implements OrganizationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrganizationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Organization save(Organization organization) {
        if (organization.id() == null || organization.id().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO organization (tenant_id, organization_name, status, created_at)
                        VALUES (?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseTenantId(organization.tenantId()));
                statement.setString(2, organization.organizationName());
                statement.setString(3, organization.status());
                statement.setTimestamp(4, toTimestamp(organization.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("organization主键生成失败");
            }
            return new Organization(
                    JdbcIdCodec.formatOrganizationId(key.longValue()),
                    organization.tenantId(),
                    organization.organizationName(),
                    organization.status(),
                    organization.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE organization
                SET organization_name = ?, status = ?
                WHERE id = ? AND tenant_id = ?
                """,
                organization.organizationName(),
                organization.status(),
                JdbcIdCodec.parseOrganizationId(organization.id()),
                JdbcIdCodec.parseTenantId(organization.tenantId())
        );
        return organization;
    }

    @Override
    public List<Organization> findByTenantId(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_name, status, created_at
                FROM organization
                WHERE tenant_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new Organization(
                        JdbcIdCodec.formatOrganizationId(rs.getLong("id")),
                        JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                        rs.getString("organization_name"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseTenantId(tenantId)
        );
    }

    @Override
    public Optional<Organization> findById(String organizationId) {
        List<Organization> organizations = jdbcTemplate.query(
                """
                SELECT id, tenant_id, organization_name, status, created_at
                FROM organization
                WHERE id = ?
                """,
                (rs, rowNum) -> new Organization(
                        JdbcIdCodec.formatOrganizationId(rs.getLong("id")),
                        JdbcIdCodec.formatTenantId(rs.getLong("tenant_id")),
                        rs.getString("organization_name"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseOrganizationId(organizationId)
        );
        return organizations.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM organization");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
