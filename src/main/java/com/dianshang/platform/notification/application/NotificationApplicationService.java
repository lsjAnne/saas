package com.dianshang.platform.notification.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.notification.application.NotificationService.BatchSendNotificationRequest;
import com.dianshang.platform.notification.application.NotificationService.NotificationTemplateSeed;
import com.dianshang.platform.notification.application.NotificationService.ReplayDeadLetterNotificationRequest;
import com.dianshang.platform.notification.application.NotificationService.SendNotificationRequest;
import com.dianshang.platform.notification.application.NotificationService.UpdateNotificationTemplateRequest;
import com.dianshang.platform.notification.domain.repository.NotificationTaskRepository;
import com.dianshang.platform.notification.domain.repository.NotificationTemplateRepository;
import com.dianshang.platform.notification.model.NotificationTask;
import com.dianshang.platform.notification.model.NotificationTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class NotificationApplicationService {

    private static final String DEFAULT_PRIORITY = "normal";
    private static final int DEAD_LETTER_RETRY_LIMIT = 3;
    private static final Set<String> SUPPORTED_NOTIFY_TYPES = Set.of(
            "site_message",
            "sms",
            "email",
            "feishu_bot"
    );
    private static final Set<String> SUPPORTED_PRIORITIES = Set.of(
            "low",
            "normal",
            "high",
            "urgent"
    );
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final List<NotificationTemplateSeed> DEFAULT_TEMPLATES = List.of(
            new NotificationTemplateSeed("manual_notice", "Manual Notice", "site_message", "Business Notice", "You have a new business notice."),
            new NotificationTemplateSeed("manual_notice_sms", "Manual SMS Notice", "sms", "SMS Notice", "You have a new SMS notice."),
            new NotificationTemplateSeed("manual_notice_email", "Manual Email Notice", "email", "Email Notice", "You have a new email notice."),
            new NotificationTemplateSeed("manual_notice_feishu", "Manual Feishu Notice", "feishu_bot", "Feishu Notice", "You have a new Feishu notice."),
            new NotificationTemplateSeed("approval_pending", "Approval Pending", "site_message", "New Approval Pending", "A new approval item is waiting for you."),
            new NotificationTemplateSeed("approval_approved", "Approval Approved", "site_message", "Approval Approved", "The approval item has been approved."),
            new NotificationTemplateSeed("approval_rejected", "Approval Rejected", "site_message", "Approval Rejected", "The approval item has been rejected."),
            new NotificationTemplateSeed("approval_transferred", "Approval Transferred", "site_message", "Approval Transferred", "The approval item has been transferred.")
    );

    private final AuditLogService auditLogService;
    private final NotificationGatewayProperties notificationGatewayProperties;
    private final NotificationTaskRepository notificationTaskRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final ObjectMapper objectMapper;

    public NotificationApplicationService(AuditLogService auditLogService,
                                          NotificationGatewayProperties notificationGatewayProperties,
                                          NotificationTaskRepository notificationTaskRepository,
                                          NotificationTemplateRepository notificationTemplateRepository,
                                          ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.notificationGatewayProperties = notificationGatewayProperties;
        this.notificationTaskRepository = notificationTaskRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.objectMapper = objectMapper;
    }

    public List<NotificationTask> listNotifications(String tenantId) {
        ensureDefaultTemplates(tenantId);
        return notificationTaskRepository.findByTenantId(tenantId);
    }

    public NotificationGatewayOverviewView getGatewayOverview(String tenantId) {
        List<NotificationTask> tasks = listNotifications(tenantId);
        List<NotificationChannelGatewayStatView> channelStats = tasks.stream()
                .collect(java.util.stream.Collectors.groupingBy(NotificationTask::notifyType, java.util.stream.Collectors.toList()))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> new NotificationChannelGatewayStatView(
                        entry.getKey(),
                        entry.getValue().size(),
                        (int) entry.getValue().stream().filter(task -> "delivered".equals(task.sendStatus())).count(),
                        (int) entry.getValue().stream().filter(task -> "sent".equals(task.sendStatus())).count(),
                        (int) entry.getValue().stream().filter(task -> "scheduled".equals(task.sendStatus())).count(),
                        (int) entry.getValue().stream().filter(task -> "failed".equals(task.sendStatus())).count(),
                        (int) entry.getValue().stream().filter(task -> "dead_letter".equals(task.sendStatus())).count()
                ))
                .toList();
        List<NotificationGatewayProviderView> providerStats = notificationGatewayProperties.listProviders().stream()
                .map(provider -> new NotificationGatewayProviderView(
                        provider.gatewayCode(),
                        provider.notifyType(),
                        provider.enabled(),
                        provider.mockMode(),
                        provider.receiptSupported(),
                        provider.endpoint(),
                        (int) tasks.stream().filter(task -> provider.gatewayCode().equals(resolveGatewayCode(task))).count(),
                        (int) tasks.stream().filter(task -> provider.gatewayCode().equals(resolveGatewayCode(task)) && "delivered".equals(task.sendStatus())).count(),
                        (int) tasks.stream().filter(task -> provider.gatewayCode().equals(resolveGatewayCode(task)) && "failed".equals(task.sendStatus())).count(),
                        provider.description()
                ))
                .toList();
        return new NotificationGatewayOverviewView(
                tasks.size(),
                (int) tasks.stream().filter(task -> "sent".equals(task.sendStatus())).count(),
                (int) tasks.stream().filter(task -> "delivered".equals(task.sendStatus())).count(),
                (int) tasks.stream().filter(task -> "scheduled".equals(task.sendStatus())).count(),
                (int) tasks.stream().filter(task -> "failed".equals(task.sendStatus())).count(),
                (int) tasks.stream().filter(task -> "dead_letter".equals(task.sendStatus())).count(),
                (int) tasks.stream().filter(task -> "urgent".equals(task.priority())).count(),
                (int) tasks.stream().filter(this::isReceiptPending).count(),
                notificationGatewayProperties.listProviders().size(),
                notificationGatewayProperties.enabledProviderCount(),
                notificationGatewayProperties.mockProviderCount(),
                providerStats,
                channelStats
        );
    }

    public NotificationTask sendNotification(String tenantId, SendNotificationRequest request) {
        ensureDefaultTemplates(tenantId);
        NotificationTemplate template = requireEnabledTemplate(tenantId, request.templateCode());
        validateNotifyType(request.notifyType(), template.notifyType());
        validateTargetReceiver(request.notifyType(), request.targetReceiver());
        String priority = normalizePriority(request.priority());
        OffsetDateTime scheduledAt = normalizeScheduledAt(request.scheduledAt());
        NotificationGatewayProperties.ResolvedGateway gateway = notificationGatewayProperties.resolveProvider(request.notifyType());
        DispatchOutcome dispatchOutcome = scheduledAt == null
                ? dispatch(request.notifyType(), request.targetReceiver(), request.payloadJson())
                : DispatchOutcome.scheduled(gateway);
        NotificationTask saved = notificationTaskRepository.save(new NotificationTask(
                null,
                tenantId,
                request.notifyType(),
                template.templateCode(),
                request.targetReceiver(),
                dispatchOutcome.sendStatus(),
                0,
                encodePayload(request.payloadJson(), priority, scheduledAt, null, null, dispatchOutcome),
                priority,
                scheduledAt,
                null,
                null,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "SEND_NOTIFICATION", "notification_task", saved.notificationTaskId());
        return saved;
    }

    public List<NotificationTask> batchSendNotifications(String tenantId, BatchSendNotificationRequest request) {
        ensureDefaultTemplates(tenantId);
        NotificationTemplate template = requireEnabledTemplate(tenantId, request.templateCode());
        validateNotifyType(request.notifyType(), template.notifyType());
        if (request.targetReceivers() == null || request.targetReceivers().isEmpty()) {
            throw new BusinessException("1004", "targetReceivers is required", HttpStatus.BAD_REQUEST);
        }
        String priority = normalizePriority(request.priority());
        OffsetDateTime scheduledAt = normalizeScheduledAt(request.scheduledAt());
        String batchId = request.batchId() == null || request.batchId().isBlank()
                ? "batch-" + System.currentTimeMillis()
                : request.batchId();
        return request.targetReceivers().stream()
                .map(targetReceiver -> {
                    validateTargetReceiver(request.notifyType(), targetReceiver);
                    NotificationGatewayProperties.ResolvedGateway gateway = notificationGatewayProperties.resolveProvider(request.notifyType());
                    DispatchOutcome dispatchOutcome = scheduledAt == null
                            ? dispatch(request.notifyType(), targetReceiver, request.payloadJson())
                            : DispatchOutcome.scheduled(gateway);
                    NotificationTask saved = notificationTaskRepository.save(new NotificationTask(
                            null,
                            tenantId,
                            request.notifyType(),
                            template.templateCode(),
                            targetReceiver,
                            dispatchOutcome.sendStatus(),
                            0,
                            encodePayload(request.payloadJson(), priority, scheduledAt, batchId, null, dispatchOutcome),
                            priority,
                            scheduledAt,
                            batchId,
                            null,
                            OffsetDateTime.now()
                    ));
                    auditLogService.recordForTenant(tenantId, "BATCH_SEND_NOTIFICATION", "notification_task", saved.notificationTaskId());
                    return saved;
                })
                .toList();
    }

    public NotificationTask retryNotification(String tenantId, String notificationTaskId) {
        NotificationTask current = requireOwnedNotificationTask(tenantId, notificationTaskId);
        if (!"failed".equals(current.sendStatus())) {
            throw new BusinessException("1008", "notification status does not allow retry", HttpStatus.BAD_REQUEST);
        }
        NotificationGatewayProperties.ResolvedGateway gateway = notificationGatewayProperties.resolveProvider(current.notifyType());
        DispatchOutcome dispatchOutcome = DispatchOutcome.sent(
                gateway,
                "manual-retry-" + gateway.gatewayCode() + "-" + System.currentTimeMillis()
        );
        NotificationTask updated = notificationTaskRepository.save(new NotificationTask(
                current.notificationTaskId(),
                current.tenantId(),
                current.notifyType(),
                current.templateCode(),
                current.targetReceiver(),
                dispatchOutcome.sendStatus(),
                current.retryCount() + 1,
                encodePayload(current.payloadJson(), current.priority(), current.scheduledAt(), current.batchId(), null, dispatchOutcome),
                current.priority(),
                null,
                current.batchId(),
                null,
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "RETRY_NOTIFICATION", "notification_task", notificationTaskId);
        return updated;
    }

    public NotificationTask recordDeliveryReceipt(String tenantId,
                                                  String notificationTaskId,
                                                  DeliveryReceiptCommand command) {
        NotificationTask current = requireOwnedNotificationTask(tenantId, notificationTaskId);
        validateGatewayReceiptCommand(current, command);
        String normalizedStatus = normalizeDeliveryStatus(command.deliveryStatus());
        String nextTaskStatus = switch (normalizedStatus) {
            case "delivered" -> "delivered";
            case "failed" -> "failed";
            default -> "sent";
        };
        String deadLetterReason = "failed".equals(normalizedStatus)
                ? defaultIfBlank(command.failureReason(), "gateway delivery failed")
                : null;
        NotificationTask updated = notificationTaskRepository.save(new NotificationTask(
                current.notificationTaskId(),
                current.tenantId(),
                current.notifyType(),
                current.templateCode(),
                current.targetReceiver(),
                nextTaskStatus,
                current.retryCount(),
                appendDeliveryReceiptMeta(current.payloadJson(), command),
                current.priority(),
                current.scheduledAt(),
                current.batchId(),
                deadLetterReason,
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "RECORD_NOTIFICATION_DELIVERY_RECEIPT", "notification_task", notificationTaskId);
        return updated;
    }

    public NotificationTask replayDeadLetterNotification(String tenantId,
                                                         String notificationTaskId,
                                                         ReplayDeadLetterNotificationRequest request) {
        NotificationTask current = requireOwnedNotificationTask(tenantId, notificationTaskId);
        if (!"dead_letter".equals(current.sendStatus())) {
            throw new BusinessException("1008", "notification status does not allow dead letter replay", HttpStatus.BAD_REQUEST);
        }
        String targetReceiver = resolveReplayTargetReceiver(current, request);
        String payloadJson = resolveReplayPayloadJson(current, request);
        OffsetDateTime scheduledAt = normalizeScheduledAt(request == null ? null : request.scheduledAt());
        NotificationGatewayProperties.ResolvedGateway gateway = notificationGatewayProperties.resolveProvider(current.notifyType());
        DispatchOutcome dispatchOutcome = scheduledAt == null
                ? dispatch(current.notifyType(), targetReceiver, payloadJson)
                : DispatchOutcome.scheduled(gateway);
        NotificationTask updated = notificationTaskRepository.save(new NotificationTask(
                current.notificationTaskId(),
                current.tenantId(),
                current.notifyType(),
                current.templateCode(),
                targetReceiver,
                dispatchOutcome.sendStatus(),
                current.retryCount() + 1,
                encodePayload(payloadJson, current.priority(), scheduledAt, current.batchId(), null, dispatchOutcome),
                current.priority(),
                scheduledAt,
                current.batchId(),
                null,
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "REPLAY_NOTIFICATION_DEAD_LETTER", "notification_task", notificationTaskId);
        return updated;
    }

    public List<NotificationTask> runDueNotifications(String tenantId) {
        List<NotificationTask> dueTasks = notificationTaskRepository.findByTenantId(tenantId).stream()
                .filter(task -> "scheduled".equals(task.sendStatus()) || "failed".equals(task.sendStatus()))
                .filter(task -> task.scheduledAt() != null)
                .filter(task -> !task.scheduledAt().isAfter(OffsetDateTime.now()))
                .sorted(Comparator
                        .comparingInt((NotificationTask task) -> priorityWeight(task.priority())).reversed()
                        .thenComparing(NotificationTask::createdAt))
                .toList();
        List<NotificationTask> updatedTasks = dueTasks.stream()
                .map(task -> {
                    int nextRetryCount = task.retryCount();
                    DispatchOutcome dispatchOutcome = dispatch(task.notifyType(), task.targetReceiver(), task.payloadJson());
                    String nextStatus = dispatchOutcome.sendStatus();
                    String deadLetterReason = null;
                    if ("failed".equals(nextStatus)) {
                        nextRetryCount = task.retryCount() + 1;
                        if (nextRetryCount >= DEAD_LETTER_RETRY_LIMIT) {
                            nextStatus = "dead_letter";
                            deadLetterReason = "retry limit exceeded";
                        }
                    }
                    NotificationTask updated = new NotificationTask(
                            task.notificationTaskId(),
                            task.tenantId(),
                            task.notifyType(),
                            task.templateCode(),
                            task.targetReceiver(),
                            nextStatus,
                            nextRetryCount,
                            encodePayload(task.payloadJson(), task.priority(), task.scheduledAt(), task.batchId(), deadLetterReason, dispatchOutcome),
                            task.priority(),
                            task.scheduledAt(),
                            task.batchId(),
                            deadLetterReason,
                            task.createdAt()
                    );
                    notificationTaskRepository.save(updated);
                    auditLogService.recordForTenant(tenantId, "RUN_DUE_NOTIFICATION", "notification_task", updated.notificationTaskId());
                    return updated;
                })
                .toList();
        auditLogService.recordForTenant(tenantId, "RUN_DUE_NOTIFICATIONS", "notification_task", "batch");
        return updatedTasks;
    }

    public List<NotificationTemplate> listTemplates(String tenantId) {
        ensureDefaultTemplates(tenantId);
        return notificationTemplateRepository.findByTenantId(tenantId);
    }

    public NotificationTemplate updateTemplate(String tenantId,
                                               String notificationTemplateId,
                                               UpdateNotificationTemplateRequest request) {
        ensureDefaultTemplates(tenantId);
        NotificationTemplate current = requireOwnedTemplate(tenantId, notificationTemplateId);
        NotificationTemplate updated = notificationTemplateRepository.save(current.withEditableFields(
                request.templateName(),
                request.titleTemplate(),
                request.contentTemplate(),
                request.enabled()
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_NOTIFICATION_TEMPLATE", "notification_template", notificationTemplateId);
        return updated;
    }

    public NotificationTask createSystemNotification(String tenantId,
                                                     String notifyType,
                                                     String templateCode,
                                                     String targetReceiver,
                                                     String payloadJson) {
        ensureDefaultTemplates(tenantId);
        NotificationTemplate template = requireEnabledTemplate(tenantId, templateCode);
        validateNotifyType(notifyType, template.notifyType());
        validateTargetReceiver(notifyType, targetReceiver);
        DispatchOutcome dispatchOutcome = dispatch(notifyType, targetReceiver, payloadJson);
        NotificationTask saved = notificationTaskRepository.save(new NotificationTask(
                null,
                tenantId,
                notifyType,
                template.templateCode(),
                targetReceiver,
                dispatchOutcome.sendStatus(),
                0,
                encodePayload(payloadJson, DEFAULT_PRIORITY, null, null, null, dispatchOutcome),
                DEFAULT_PRIORITY,
                null,
                null,
                null,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_SYSTEM_NOTIFICATION", "notification_task", saved.notificationTaskId());
        return saved;
    }

    public void clear() {
        notificationTaskRepository.deleteAll();
        notificationTemplateRepository.deleteAll();
    }

    private NotificationTask requireOwnedNotificationTask(String tenantId, String notificationTaskId) {
        NotificationTask task = notificationTaskRepository.findByNotificationTaskId(notificationTaskId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(task.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return task;
    }

    private NotificationTemplate requireOwnedTemplate(String tenantId, String notificationTemplateId) {
        NotificationTemplate template = notificationTemplateRepository.findByNotificationTemplateId(notificationTemplateId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(template.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return template;
    }

    private NotificationTemplate requireEnabledTemplate(String tenantId, String templateCode) {
        NotificationTemplate template = notificationTemplateRepository.findByTenantIdAndTemplateCode(tenantId, templateCode)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!template.enabled()) {
            throw new BusinessException("1008", "notification template disabled", HttpStatus.BAD_REQUEST);
        }
        return template;
    }

    private void ensureDefaultTemplates(String tenantId) {
        for (NotificationTemplateSeed seed : DEFAULT_TEMPLATES) {
            notificationTemplateRepository.findByTenantIdAndTemplateCode(tenantId, seed.templateCode())
                    .orElseGet(() -> notificationTemplateRepository.save(new NotificationTemplate(
                            null,
                            tenantId,
                            seed.templateCode(),
                            seed.templateName(),
                            seed.notifyType(),
                            seed.titleTemplate(),
                            seed.contentTemplate(),
                            true,
                            OffsetDateTime.now()
                    )));
        }
    }

    private DispatchOutcome dispatch(String notifyType, String targetReceiver, String payloadJson) {
        if (!SUPPORTED_NOTIFY_TYPES.contains(notifyType)) {
            throw new BusinessException("1002", "unsupported notification channel", HttpStatus.BAD_REQUEST);
        }
        NotificationGatewayProperties.ResolvedGateway gateway = notificationGatewayProperties.resolveProvider(notifyType);
        if (!gateway.enabled()) {
            return DispatchOutcome.failed(gateway, "gateway disabled");
        }
        if ((targetReceiver != null && targetReceiver.toLowerCase().contains("fail"))
                || (payloadJson != null && payloadJson.contains("\"forceFail\":true"))) {
            return DispatchOutcome.failed(gateway, "gateway delivery failed");
        }
        String providerMessageId = gateway.mockMode()
                ? "mock-" + gateway.gatewayCode() + "-" + System.currentTimeMillis()
                : gateway.gatewayCode() + "-" + System.currentTimeMillis();
        return DispatchOutcome.sent(gateway, providerMessageId);
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return DEFAULT_PRIORITY;
        }
        if (!SUPPORTED_PRIORITIES.contains(priority)) {
            throw new BusinessException("1002", "unsupported notification priority", HttpStatus.BAD_REQUEST);
        }
        return priority;
    }

    private OffsetDateTime normalizeScheduledAt(OffsetDateTime scheduledAt) {
        if (scheduledAt == null) {
            return null;
        }
        if (scheduledAt.isBefore(OffsetDateTime.now())) {
            throw new BusinessException("1002", "scheduledAt must be in the future", HttpStatus.BAD_REQUEST);
        }
        return scheduledAt;
    }

    private String resolveReplayTargetReceiver(NotificationTask current,
                                               ReplayDeadLetterNotificationRequest request) {
        String targetReceiver = request == null || request.targetReceiver() == null || request.targetReceiver().isBlank()
                ? current.targetReceiver()
                : request.targetReceiver().trim();
        validateTargetReceiver(current.notifyType(), targetReceiver);
        return targetReceiver;
    }

    private String resolveReplayPayloadJson(NotificationTask current,
                                            ReplayDeadLetterNotificationRequest request) {
        if (request == null || request.payloadJson() == null) {
            return current.payloadJson();
        }
        return request.payloadJson();
    }

    private void validateGatewayReceiptCommand(NotificationTask current,
                                               DeliveryReceiptCommand command) {
        String gatewayCode = command.gatewayCode() == null ? "" : command.gatewayCode().trim();
        if (!notificationGatewayProperties.supportsReceipt(gatewayCode)) {
            throw new BusinessException("1002", "unsupported gatewayCode", HttpStatus.BAD_REQUEST);
        }
    }

    private String appendDeliveryReceiptMeta(String payloadJson, DeliveryReceiptCommand command) {
        ObjectNode root = parsePayloadObject(payloadJson);
        ObjectNode meta = root.has("_meta") && root.get("_meta").isObject()
                ? (ObjectNode) root.get("_meta")
                : root.putObject("_meta");
        meta.put("gatewayCode", command.gatewayCode().trim());
        meta.put("deliveryStatus", normalizeDeliveryStatus(command.deliveryStatus()));
        if (command.providerMessageId() != null && !command.providerMessageId().isBlank()) {
            meta.put("providerMessageId", command.providerMessageId().trim());
        } else {
            meta.remove("providerMessageId");
        }
        if (command.receiptTraceId() != null && !command.receiptTraceId().isBlank()) {
            meta.put("receiptTraceId", command.receiptTraceId().trim());
        } else {
            meta.remove("receiptTraceId");
        }
        if (command.failureReason() != null && !command.failureReason().isBlank()) {
            meta.put("failureReason", command.failureReason().trim());
        } else {
            meta.remove("failureReason");
        }
        meta.put("receiptAt", OffsetDateTime.now().toString());
        return writePayload(root);
    }

    private String encodePayload(String payloadJson,
                                 String priority,
                                 OffsetDateTime scheduledAt,
                                 String batchId,
                                 String deadLetterReason,
                                 DispatchOutcome dispatchOutcome) {
        ObjectNode root = parsePayloadObject(payloadJson);
        ObjectNode meta = root.has("_meta") && root.get("_meta").isObject()
                ? (ObjectNode) root.get("_meta")
                : root.putObject("_meta");
        meta.put("priority", priority);
        if (scheduledAt != null) {
            meta.put("scheduledAt", scheduledAt.toString());
        } else {
            meta.remove("scheduledAt");
        }
        if (batchId != null && !batchId.isBlank()) {
            meta.put("batchId", batchId);
        } else {
            meta.remove("batchId");
        }
        if (deadLetterReason != null && !deadLetterReason.isBlank()) {
            meta.put("deadLetterReason", deadLetterReason);
        } else {
            meta.remove("deadLetterReason");
        }
        if (dispatchOutcome != null) {
            meta.put("gatewayCode", dispatchOutcome.gatewayCode());
            meta.put("gatewayMockMode", dispatchOutcome.mockMode());
            meta.put("receiptSupported", dispatchOutcome.receiptSupported());
            meta.put("gatewayEndpoint", dispatchOutcome.endpoint());
            if (dispatchOutcome.providerMessageId() != null && !dispatchOutcome.providerMessageId().isBlank()) {
                meta.put("providerMessageId", dispatchOutcome.providerMessageId());
            }
            if (dispatchOutcome.failureReason() != null && !dispatchOutcome.failureReason().isBlank()) {
                meta.put("failureReason", dispatchOutcome.failureReason());
            } else if (!"failed".equals(dispatchOutcome.sendStatus())) {
                meta.remove("failureReason");
            }
        }
        return writePayload(root);
    }

    private String resolveGatewayCode(NotificationTask task) {
        ObjectNode payload = parsePayloadObject(task.payloadJson());
        JsonNode metaNode = payload.get("_meta");
        if (metaNode != null && metaNode.isObject()) {
            JsonNode gatewayCodeNode = metaNode.get("gatewayCode");
            if (gatewayCodeNode != null && !gatewayCodeNode.asText().isBlank()) {
                return gatewayCodeNode.asText();
            }
        }
        return notificationGatewayProperties.resolveProvider(task.notifyType()).gatewayCode();
    }

    private ObjectNode parsePayloadObject(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode parsed = objectMapper.readTree(payloadJson);
            if (parsed != null && parsed.isObject()) {
                return ((ObjectNode) parsed).deepCopy();
            }
        } catch (JsonProcessingException ignored) {
            // Fallback to wrap raw payload below.
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("_rawPayload", payloadJson);
        return root;
    }

    private String writePayload(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("1002", "notification payload serialize failed", HttpStatus.BAD_REQUEST);
        }
    }

    private int priorityWeight(String priority) {
        return switch (priority == null ? DEFAULT_PRIORITY : priority) {
            case "urgent" -> 4;
            case "high" -> 3;
            case "normal" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private void validateNotifyType(String requestNotifyType, String templateNotifyType) {
        if (!SUPPORTED_NOTIFY_TYPES.contains(requestNotifyType)) {
            throw new BusinessException("1002", "unsupported notification channel", HttpStatus.BAD_REQUEST);
        }
        if (!requestNotifyType.equals(templateNotifyType)) {
            throw new BusinessException("1004", "notifyType does not match template channel", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTargetReceiver(String notifyType, String targetReceiver) {
        switch (notifyType) {
            case "site_message" -> {
                if (targetReceiver == null || targetReceiver.isBlank()) {
                    throw new BusinessException("1004", "targetReceiver is required", HttpStatus.BAD_REQUEST);
                }
            }
            case "sms" -> {
                if (targetReceiver == null || !MOBILE_PATTERN.matcher(targetReceiver).matches()) {
                    throw new BusinessException("1004", "sms targetReceiver must be a valid mobile number", HttpStatus.BAD_REQUEST);
                }
            }
            case "email" -> {
                if (targetReceiver == null || !EMAIL_PATTERN.matcher(targetReceiver).matches()) {
                    throw new BusinessException("1004", "email targetReceiver must be a valid email address", HttpStatus.BAD_REQUEST);
                }
            }
            case "feishu_bot" -> {
                if (targetReceiver == null
                        || (!targetReceiver.startsWith("hook:")
                        && !targetReceiver.startsWith("open_id:")
                        && !targetReceiver.startsWith("https://open.feishu.cn/"))) {
                    throw new BusinessException("1004", "feishu_bot targetReceiver must be a hook or open_id target", HttpStatus.BAD_REQUEST);
                }
            }
            default -> throw new BusinessException("1002", "unsupported notification channel", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isReceiptPending(NotificationTask task) {
        return List.of("sent", "scheduled").contains(task.sendStatus());
    }

    private String normalizeDeliveryStatus(String deliveryStatus) {
        String normalized = deliveryStatus == null ? "" : deliveryStatus.trim().toLowerCase();
        if (!Set.of("accepted", "delivered", "failed").contains(normalized)) {
            throw new BusinessException("1002", "unsupported deliveryStatus", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record DeliveryReceiptCommand(
            String gatewayCode,
            String deliveryStatus,
            String providerMessageId,
            String receiptTraceId,
            String failureReason
    ) {
    }

    public record NotificationGatewayOverviewView(
            int totalTaskCount,
            int sentTaskCount,
            int deliveredTaskCount,
            int scheduledTaskCount,
            int failedTaskCount,
            int deadLetterTaskCount,
            int urgentTaskCount,
            int receiptPendingTaskCount,
            int configuredGatewayCount,
            int enabledGatewayCount,
            int mockGatewayCount,
            List<NotificationGatewayProviderView> providerStats,
            List<NotificationChannelGatewayStatView> channelStats
    ) {
    }

    public record NotificationGatewayProviderView(
            String gatewayCode,
            String notifyType,
            boolean enabled,
            boolean mockMode,
            boolean receiptSupported,
            String endpoint,
            int routedTaskCount,
            int deliveredTaskCount,
            int failedTaskCount,
            String description
    ) {
    }

    public record NotificationChannelGatewayStatView(
            String notifyType,
            int totalTaskCount,
            int deliveredTaskCount,
            int sentTaskCount,
            int scheduledTaskCount,
            int failedTaskCount,
            int deadLetterTaskCount
    ) {
    }

    private record DispatchOutcome(
            String sendStatus,
            String gatewayCode,
            boolean mockMode,
            boolean receiptSupported,
            String endpoint,
            String providerMessageId,
            String failureReason
    ) {
        private static DispatchOutcome sent(NotificationGatewayProperties.ResolvedGateway gateway, String providerMessageId) {
            return new DispatchOutcome(
                    "sent",
                    gateway.gatewayCode(),
                    gateway.mockMode(),
                    gateway.receiptSupported(),
                    gateway.endpoint(),
                    providerMessageId,
                    null
            );
        }

        private static DispatchOutcome failed(NotificationGatewayProperties.ResolvedGateway gateway, String failureReason) {
            return new DispatchOutcome(
                    "failed",
                    gateway.gatewayCode(),
                    gateway.mockMode(),
                    gateway.receiptSupported(),
                    gateway.endpoint(),
                    null,
                    failureReason
            );
        }

        private static DispatchOutcome scheduled(NotificationGatewayProperties.ResolvedGateway gateway) {
            return new DispatchOutcome(
                    "scheduled",
                    gateway.gatewayCode(),
                    gateway.mockMode(),
                    gateway.receiptSupported(),
                    gateway.endpoint(),
                    null,
                    null
            );
        }
    }
}
