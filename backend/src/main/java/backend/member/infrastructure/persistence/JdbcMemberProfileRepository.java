package backend.member.infrastructure.persistence;

import backend.member.domain.repository.MemberProfileRepository;
import backend.member.model.MemberProfile;
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
public class JdbcMemberProfileRepository implements MemberProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMemberProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MemberProfile> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(item -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
                """
                SELECT id, store_id, customer_id, nickname, level_code, total_order_count, total_paid_amount, last_order_at, created_at
                FROM member_profile
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new MemberProfile(
                        JdbcIdCodec.formatMemberId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("customer_id"),
                        rs.getString("nickname"),
                        rs.getString("level_code"),
                        rs.getInt("total_order_count"),
                        rs.getBigDecimal("total_paid_amount"),
                        toOffsetDateTime(rs.getTimestamp("last_order_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray()
        );
    }

    @Override
    public Optional<MemberProfile> findByMemberId(String memberId) {
        List<MemberProfile> result = jdbcTemplate.query(
                """
                SELECT id, store_id, customer_id, nickname, level_code, total_order_count, total_paid_amount, last_order_at, created_at
                FROM member_profile
                WHERE id = ?
                """,
                (rs, rowNum) -> new MemberProfile(
                        JdbcIdCodec.formatMemberId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("customer_id"),
                        rs.getString("nickname"),
                        rs.getString("level_code"),
                        rs.getInt("total_order_count"),
                        rs.getBigDecimal("total_paid_amount"),
                        toOffsetDateTime(rs.getTimestamp("last_order_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseMemberId(memberId)
        );
        return result.stream().findFirst();
    }

    @Override
    public Optional<MemberProfile> findByStoreAndCustomerId(String storeId, String customerId) {
        List<MemberProfile> result = jdbcTemplate.query(
                """
                SELECT id, store_id, customer_id, nickname, level_code, total_order_count, total_paid_amount, last_order_at, created_at
                FROM member_profile
                WHERE store_id = ? AND customer_id = ?
                """,
                (rs, rowNum) -> new MemberProfile(
                        JdbcIdCodec.formatMemberId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        rs.getString("customer_id"),
                        rs.getString("nickname"),
                        rs.getString("level_code"),
                        rs.getInt("total_order_count"),
                        rs.getBigDecimal("total_paid_amount"),
                        toOffsetDateTime(rs.getTimestamp("last_order_at")),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseStoreId(storeId),
                customerId
        );
        return result.stream().findFirst();
    }

    @Override
    public MemberProfile save(MemberProfile memberProfile) {
        if (memberProfile.memberId() == null || memberProfile.memberId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO member_profile (
                            store_id, customer_id, nickname, level_code, total_order_count, total_paid_amount, last_order_at, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(memberProfile.storeId()));
                statement.setString(2, memberProfile.customerId());
                statement.setString(3, memberProfile.nickname());
                statement.setString(4, memberProfile.levelCode());
                statement.setInt(5, memberProfile.totalOrderCount());
                statement.setBigDecimal(6, memberProfile.totalPaidAmount());
                statement.setTimestamp(7, toTimestamp(memberProfile.lastOrderAt()));
                statement.setTimestamp(8, toTimestamp(memberProfile.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("member_profile primary key generation failed");
            }
            return new MemberProfile(
                    JdbcIdCodec.formatMemberId(key.longValue()),
                    memberProfile.storeId(),
                    memberProfile.customerId(),
                    memberProfile.nickname(),
                    memberProfile.levelCode(),
                    memberProfile.totalOrderCount(),
                    memberProfile.totalPaidAmount(),
                    memberProfile.lastOrderAt(),
                    memberProfile.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE member_profile
                SET nickname = ?, level_code = ?, total_order_count = ?, total_paid_amount = ?, last_order_at = ?
                WHERE id = ?
                """,
                memberProfile.nickname(),
                memberProfile.levelCode(),
                memberProfile.totalOrderCount(),
                memberProfile.totalPaidAmount(),
                toTimestamp(memberProfile.lastOrderAt()),
                JdbcIdCodec.parseMemberId(memberProfile.memberId())
        );
        return memberProfile;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM member_profile");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

