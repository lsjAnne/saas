package com.dianshang.platform.exceptioncenter.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.exceptioncenter.domain.repository.ExceptionTaskRepository;
import com.dianshang.platform.exceptioncenter.dto.UpdateExceptionTaskRequest;
import com.dianshang.platform.exceptioncenter.model.ExceptionTask;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ExceptionService {

    private static final List<String> PROCESSABLE_STATUSES = List.of("new", "todo", "processing", "escalated");
    private static final List<String> IGNORABLE_STATUSES = List.of("new", "todo", "processing");
    private static final List<String> ESCALATABLE_STATUSES = List.of("new", "todo", "processing");

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final ExceptionTaskRepository exceptionTaskRepository;

    public ExceptionService(AuditLogService auditLogService,
                            StoreRepository storeRepository,
                            ExceptionTaskRepository exceptionTaskRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.exceptionTaskRepository = exceptionTaskRepository;
    }

    public List<ExceptionTask> listExceptions(String tenantId) {
        return exceptionTaskRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public ExceptionTask getException(String tenantId, String exceptionTaskId) {
        ExceptionTask exceptionTask = exceptionTaskRepository.findByExceptionTaskId(exceptionTaskId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, exceptionTask.storeId());
        return exceptionTask;
    }

    public ExceptionTask process(String tenantId, String exceptionTaskId, UpdateExceptionTaskRequest request) {
        ExceptionTask current = getException(tenantId, exceptionTaskId);
        requireStatus(current.status(), PROCESSABLE_STATUSES, "current status does not allow process");
        ExceptionTask updated = exceptionTaskRepository.save(current.withStatus(
                "resolved",
                fallback(request.operatorId(), current.ownerUserId()),
                buildSuggestionText(current.suggestionText(), request.action(), request.remark(), "processed")
        ));
        auditLogService.recordForTenant(tenantId, "PROCESS_EXCEPTION_TASK", "exception_task", exceptionTaskId);
        return updated;
    }

    public ExceptionTask ignore(String tenantId, String exceptionTaskId, UpdateExceptionTaskRequest request) {
        ExceptionTask current = getException(tenantId, exceptionTaskId);
        requireStatus(current.status(), IGNORABLE_STATUSES, "current status does not allow ignore");
        ExceptionTask updated = exceptionTaskRepository.save(current.withStatus(
                "ignored",
                fallback(request.operatorId(), current.ownerUserId()),
                buildSuggestionText(current.suggestionText(), null, request.remark(), "ignored")
        ));
        auditLogService.recordForTenant(tenantId, "IGNORE_EXCEPTION_TASK", "exception_task", exceptionTaskId);
        return updated;
    }

    public ExceptionTask escalate(String tenantId, String exceptionTaskId, UpdateExceptionTaskRequest request) {
        ExceptionTask current = getException(tenantId, exceptionTaskId);
        requireStatus(current.status(), ESCALATABLE_STATUSES, "current status does not allow escalate");
        ExceptionTask updated = exceptionTaskRepository.save(current.withStatus(
                "escalated",
                fallback(request.operatorId(), current.ownerUserId()),
                buildSuggestionText(current.suggestionText(), null, request.remark(), "escalated")
        ));
        auditLogService.recordForTenant(tenantId, "ESCALATE_EXCEPTION_TASK", "exception_task", exceptionTaskId);
        return updated;
    }

    public ExceptionTask createAutoException(String tenantId,
                                             String storeId,
                                             String relatedType,
                                             String relatedId,
                                             String exceptionType,
                                             String severity,
                                             String suggestionText) {
        requireOwnedStore(tenantId, storeId);
        ExceptionTask exceptionTask = exceptionTaskRepository.save(new ExceptionTask(
                null,
                storeId,
                relatedType,
                relatedId,
                exceptionType,
                severity,
                "new",
                suggestionText,
                null,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_EXCEPTION_TASK", "exception_task", exceptionTask.exceptionTaskId());
        return exceptionTask;
    }

    public void clear() {
        exceptionTaskRepository.deleteAll();
    }

    private void requireStatus(String currentStatus, List<String> allowed, String message) {
        if (!allowed.contains(currentStatus)) {
            throw new BusinessException("1004", message, HttpStatus.BAD_REQUEST);
        }
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "租户上下文非法切换", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private String buildSuggestionText(String current, String action, String remark, String suffix) {
        StringBuilder builder = new StringBuilder();
        if (current != null && !current.isBlank()) {
            builder.append(current);
        }
        if (action != null && !action.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("action=").append(action);
        }
        if (remark != null && !remark.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("remark=").append(remark);
        }
        if (!builder.isEmpty()) {
            builder.append(" | ");
        }
        builder.append("status=").append(suffix);
        return builder.toString();
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
