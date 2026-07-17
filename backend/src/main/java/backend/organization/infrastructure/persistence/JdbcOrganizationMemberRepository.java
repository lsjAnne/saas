package backend.organization.infrastructure.persistence;

import backend.organization.domain.repository.OrganizationMemberRepository;
import backend.organization.model.OrganizationMember;
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

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcOrganizationMemberRepository implements OrganizationMemberRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrganizationMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OrganizationMember save(OrganizationMember member) {
        if (member.memberId() == null || member.memberId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO organization_member (organization_id, user_id, user_name, mobile, role_code, status, joined_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseOrganizationId(member.organizationId()));
                statement.setString(2, member.userId());
                statement.setString(3, member.userName());
                statement.setString(4, member.mobile());
                statement.setString(5, member.roleCode());
                statement.setString(6, member.status());
                statement.setTimestamp(7, toTimestamp(member.joinedAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("organization_member涓婚敭鐢熸垚澶辫触");
            }
            return new OrganizationMember(
                    JdbcIdCodec.formatMemberId(key.longValue()),
                    member.organizationId(),
                    member.userId(),
                    member.userName(),
                    member.mobile(),
                    member.roleCode(),
                    member.status(),
                    member.joinedAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE organization_member
                SET user_id = ?, user_name = ?, mobile = ?, role_code = ?, status = ?
                WHERE id = ? AND organization_id = ?
                """,
                member.userId(),
                member.userName(),
                member.mobile(),
                member.roleCode(),
                member.status(),
                JdbcIdCodec.parseMemberId(member.memberId()),
                JdbcIdCodec.parseOrganizationId(member.organizationId())
        );
        return member;
    }

    @Override
    public List<OrganizationMember> findByOrganizationId(String organizationId) {
        return jdbcTemplate.query(
                """
                SELECT id, organization_id, user_id, user_name, mobile, role_code, status, joined_at
                FROM organization_member
                WHERE organization_id = ?
                ORDER BY joined_at, id
                """,
                (rs, rowNum) -> new OrganizationMember(
                        JdbcIdCodec.formatMemberId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("user_id"),
                        rs.getString("user_name"),
                        rs.getString("mobile"),
                        rs.getString("role_code"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("joined_at"))
                ),
                JdbcIdCodec.parseOrganizationId(organizationId)
        );
    }

    @Override
    public Optional<OrganizationMember> findByMemberId(String memberId) {
        List<OrganizationMember> members = jdbcTemplate.query(
                """
                SELECT id, organization_id, user_id, user_name, mobile, role_code, status, joined_at
                FROM organization_member
                WHERE id = ?
                """,
                (rs, rowNum) -> new OrganizationMember(
                        JdbcIdCodec.formatMemberId(rs.getLong("id")),
                        JdbcIdCodec.formatOrganizationId(rs.getLong("organization_id")),
                        rs.getString("user_id"),
                        rs.getString("user_name"),
                        rs.getString("mobile"),
                        rs.getString("role_code"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("joined_at"))
                ),
                JdbcIdCodec.parseMemberId(memberId)
        );
        return members.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM organization_member");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

