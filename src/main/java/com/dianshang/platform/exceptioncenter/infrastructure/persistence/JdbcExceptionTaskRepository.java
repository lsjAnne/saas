package com.dianshang.platform.exceptioncenter.infrastructure.persistence;

import com.dianshang.platform.exceptioncenter.domain.repository.ExceptionTaskRepository;
import com.dianshang.platform.exceptioncenter.model.ExceptionTask;
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
public class JdbcExceptionTaskRepository implements ExceptionTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcExceptionTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ExceptionTask save(ExceptionTask exceptionTask) {
        if (exceptionTask.exceptionTaskId() == null || exceptionTask.exceptionTaskId().isBlank()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO exception_task (
                            store_id, related_type, related_id, exception_type, severity, status, suggestion_text, owner_user_id, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setLong(1, JdbcIdCodec.parseStoreId(exceptionTask.storeId()));
                statement.setString(2, exceptionTask.relatedType());
                statement.setString(3, exceptionTask.relatedId());
                statement.setString(4, exceptionTask.exceptionType());
                statement.setString(5, exceptionTask.severity());
                statement.setString(6, exceptionTask.status());
                statement.setString(7, exceptionTask.suggestionText());
                statement.setString(8, exceptionTask.ownerUserId());
                statement.setTimestamp(9, toTimestamp(exceptionTask.createdAt()));
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("exception_task主键生成失败");
            }
            return new ExceptionTask(
                    JdbcIdCodec.formatExceptionTaskId(key.longValue()),
                    exceptionTask.storeId(),
                    exceptionTask.relatedType(),
                    exceptionTask.relatedId(),
                    exceptionTask.exceptionType(),
                    exceptionTask.severity(),
                    exceptionTask.status(),
                    exceptionTask.suggestionText(),
                    exceptionTask.ownerUserId(),
                    exceptionTask.createdAt()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE exception_task
                SET severity = ?, status = ?, suggestion_text = ?, owner_user_id = ?
                WHERE id = ?
                """,
                exceptionTask.severity(),
                exceptionTask.status(),
                exceptionTask.suggestionText(),
                exceptionTask.ownerUserId(),
                JdbcIdCodec.parseExceptionTaskId(exceptionTask.exceptionTaskId())
        );
        return exceptionTask;
    }

    @Override
    public List<ExceptionTask> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = storeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] arguments = storeIds.stream().map(JdbcIdCodec::parseStoreId).toArray();
        return jdbcTemplate.query(
                """
                SELECT id, store_id, related_type, related_id, exception_type, severity, status, suggestion_text, owner_user_id, created_at
                FROM exception_task
                WHERE store_id IN (%s)
                ORDER BY created_at DESC, id DESC
                """.formatted(placeholders),
                (rs, rowNum) -> mapTask(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("related_type"),
                        rs.getString("related_id"),
                        rs.getString("exception_type"),
                        rs.getString("severity"),
                        rs.getString("status"),
                        rs.getString("suggestion_text"),
                        rs.getString("owner_user_id"),
                        rs.getTimestamp("created_at")
                ),
                arguments
        );
    }

    @Override
    public Optional<ExceptionTask> findByExceptionTaskId(String exceptionTaskId) {
        List<ExceptionTask> tasks = jdbcTemplate.query(
                """
                SELECT id, store_id, related_type, related_id, exception_type, severity, status, suggestion_text, owner_user_id, created_at
                FROM exception_task
                WHERE id = ?
                """,
                (rs, rowNum) -> mapTask(
                        rs.getLong("id"),
                        rs.getLong("store_id"),
                        rs.getString("related_type"),
                        rs.getString("related_id"),
                        rs.getString("exception_type"),
                        rs.getString("severity"),
                        rs.getString("status"),
                        rs.getString("suggestion_text"),
                        rs.getString("owner_user_id"),
                        rs.getTimestamp("created_at")
                ),
                JdbcIdCodec.parseExceptionTaskId(exceptionTaskId)
        );
        return tasks.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM exception_task");
    }

    private ExceptionTask mapTask(long exceptionTaskId,
                                  long storeId,
                                  String relatedType,
                                  String relatedId,
                                  String exceptionType,
                                  String severity,
                                  String status,
                                  String suggestionText,
                                  String ownerUserId,
                                  Timestamp createdAt) {
        return new ExceptionTask(
                JdbcIdCodec.formatExceptionTaskId(exceptionTaskId),
                JdbcIdCodec.formatStoreId(storeId),
                relatedType,
                relatedId,
                exceptionType,
                severity,
                status,
                suggestionText,
                ownerUserId,
                toOffsetDateTime(createdAt)
        );
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
