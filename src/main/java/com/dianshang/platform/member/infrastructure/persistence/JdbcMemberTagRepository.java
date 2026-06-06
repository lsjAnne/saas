package com.dianshang.platform.member.infrastructure.persistence;

import com.dianshang.platform.member.domain.repository.MemberTagRepository;
import com.dianshang.platform.member.model.MemberTag;
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
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcMemberTagRepository implements MemberTagRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMemberTagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MemberTag> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(item -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
                """
                SELECT id, store_id, member_profile_id, tag_code, tag_name, source_type, created_at
                FROM member_tag
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> new MemberTag(
                        JdbcIdCodec.formatMemberTagId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        JdbcIdCodec.formatMemberId(rs.getLong("member_profile_id")),
                        rs.getString("tag_code"),
                        rs.getString("tag_name"),
                        rs.getString("source_type"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray()
        );
    }

    @Override
    public List<MemberTag> findByMemberId(String memberId) {
        return jdbcTemplate.query(
                """
                SELECT id, store_id, member_profile_id, tag_code, tag_name, source_type, created_at
                FROM member_tag
                WHERE member_profile_id = ?
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> new MemberTag(
                        JdbcIdCodec.formatMemberTagId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        JdbcIdCodec.formatMemberId(rs.getLong("member_profile_id")),
                        rs.getString("tag_code"),
                        rs.getString("tag_name"),
                        rs.getString("source_type"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseMemberId(memberId)
        );
    }

    @Override
    public Optional<MemberTag> findByMemberTagId(String memberTagId) {
        List<MemberTag> result = jdbcTemplate.query(
                """
                SELECT id, store_id, member_profile_id, tag_code, tag_name, source_type, created_at
                FROM member_tag
                WHERE id = ?
                """,
                (rs, rowNum) -> new MemberTag(
                        JdbcIdCodec.formatMemberTagId(rs.getLong("id")),
                        JdbcIdCodec.formatStoreId(rs.getLong("store_id")),
                        JdbcIdCodec.formatMemberId(rs.getLong("member_profile_id")),
                        rs.getString("tag_code"),
                        rs.getString("tag_name"),
                        rs.getString("source_type"),
                        toOffsetDateTime(rs.getTimestamp("created_at"))
                ),
                JdbcIdCodec.parseMemberTagId(memberTagId)
        );
        return result.stream().findFirst();
    }

    @Override
    public MemberTag save(MemberTag memberTag) {
        if (memberTag.memberTagId() == null || memberTag.memberTagId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO member_tag (
                            store_id, member_profile_id, tag_code, tag_name, source_type, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(memberTag.storeId()));
                statement.setLong(2, JdbcIdCodec.parseMemberId(memberTag.memberId()));
                statement.setString(3, memberTag.tagCode());
                statement.setString(4, memberTag.tagName());
                statement.setString(5, memberTag.sourceType());
                statement.setTimestamp(6, toTimestamp(memberTag.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("member_tag primary key generation failed");
            }
            return new MemberTag(
                    JdbcIdCodec.formatMemberTagId(key.longValue()),
                    memberTag.storeId(),
                    memberTag.memberId(),
                    memberTag.tagCode(),
                    memberTag.tagName(),
                    memberTag.sourceType(),
                    memberTag.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE member_tag
                SET tag_name = ?, source_type = ?
                WHERE id = ?
                """,
                memberTag.tagName(),
                memberTag.sourceType(),
                JdbcIdCodec.parseMemberTagId(memberTag.memberTagId())
        );
        return memberTag;
    }

    @Override
    public void deleteByMemberTagId(String memberTagId) {
        jdbcTemplate.update("DELETE FROM member_tag WHERE id = ?", JdbcIdCodec.parseMemberTagId(memberTagId));
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM member_tag");
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
