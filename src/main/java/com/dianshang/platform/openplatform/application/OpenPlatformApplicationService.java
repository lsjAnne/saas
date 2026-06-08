package com.dianshang.platform.openplatform.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.openplatform.domain.repository.IntegrationCredentialRepository;
import com.dianshang.platform.openplatform.domain.repository.OpenCallbackReplayRepository;
import com.dianshang.platform.openplatform.domain.repository.OpenPlatformCallLogRepository;
import com.dianshang.platform.openplatform.domain.repository.PluginAppRepository;
import com.dianshang.platform.openplatform.domain.repository.WebhookSubscriptionRepository;
import com.dianshang.platform.openplatform.model.IntegrationCredential;
import com.dianshang.platform.openplatform.model.OpenCallbackReplayRecord;
import com.dianshang.platform.openplatform.model.OpenCallbackReceiveResult;
import com.dianshang.platform.openplatform.model.OpenPlatformCallLog;
import com.dianshang.platform.openplatform.model.PluginApp;
import com.dianshang.platform.openplatform.model.WebhookSubscription;
import com.dianshang.platform.organization.domain.repository.OrganizationRepository;
import com.dianshang.platform.organization.model.Organization;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OpenPlatformApplicationService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Duration CALLBACK_FRESHNESS_WINDOW = Duration.ofMinutes(5);
    private static final Duration CREDENTIAL_EXPIRE_DURATION = Duration.ofDays(90);
    private static final String DEFAULT_CREDENTIAL_TYPE = "default";
    private static final String ERP_FINANCE_READ_SCOPE = "finance.read";
    private static final String ERP_MASTER_DATA_READ_SCOPE = "erp.master_data.read";
    private static final String ERP_ACCOUNT_MAPPING_READ_SCOPE = "erp.account_mapping.read";
    private static final String ERP_INTEGRATION_BASELINE_READ_SCOPE = "erp.integration_baseline.read";
    private static final String SOURCE_MODULE = "openplatform";

    private final AuditLogService auditLogService;
    private final OrganizationRepository organizationRepository;
    private final PluginAppRepository pluginAppRepository;
    private final IntegrationCredentialRepository integrationCredentialRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final OpenPlatformCallLogRepository openPlatformCallLogRepository;
    private final OpenCallbackReplayRepository openCallbackReplayRepository;

    public OpenPlatformApplicationService(AuditLogService auditLogService,
                                          OrganizationRepository organizationRepository,
                                          PluginAppRepository pluginAppRepository,
                                          IntegrationCredentialRepository integrationCredentialRepository,
                                          WebhookSubscriptionRepository webhookSubscriptionRepository,
                                          OpenPlatformCallLogRepository openPlatformCallLogRepository,
                                          OpenCallbackReplayRepository openCallbackReplayRepository) {
        this.auditLogService = auditLogService;
        this.organizationRepository = organizationRepository;
        this.pluginAppRepository = pluginAppRepository;
        this.integrationCredentialRepository = integrationCredentialRepository;
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.openPlatformCallLogRepository = openPlatformCallLogRepository;
        this.openCallbackReplayRepository = openCallbackReplayRepository;
    }

    public List<PluginApp> listApps(String tenantId) {
        List<String> organizationIds = ownedOrganizationIds(tenantId);
        if (organizationIds.isEmpty()) {
            return List.of();
        }
        return pluginAppRepository.findByOrganizationIds(organizationIds);
    }

    @Transactional
    public PluginApp createApp(String tenantId, CreatePluginAppCommand command) {
        requireOwnedOrganization(tenantId, command.organizationId());
        List<String> permissionScope = normalizePermissionScope(command.permissionScope());
        pluginAppRepository.findByOrganizationIdAndAppName(command.organizationId(), command.appName().trim())
                .ifPresent(existing -> {
                    throw new BusinessException("1008", "plugin app already exists", HttpStatus.BAD_REQUEST);
                });

        GeneratedCredential generatedCredential = generateCredential();
        PluginApp saved = pluginAppRepository.save(new PluginApp(
                null,
                command.organizationId(),
                command.appName().trim(),
                command.appType().trim(),
                permissionScope,
                generatedCredential.accessKey(),
                generatedCredential.secretMasked(),
                "active",
                OffsetDateTime.now()
        ));
        integrationCredentialRepository.save(new IntegrationCredential(
                null,
                saved.appId(),
                DEFAULT_CREDENTIAL_TYPE,
                generatedCredential.accessKey(),
                generatedCredential.secretDigest(),
                generatedCredential.secretMasked(),
                generatedCredential.expiresAt(),
                generatedCredential.issuedAt()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_PLUGIN_APP", "plugin_app", saved.appId());
        recordCallLog(
                tenantId,
                command.organizationId(),
                saved.appId(),
                null,
                null,
                "/api/open/apps",
                "management",
                "created",
                true,
                false,
                "plugin app created"
        );
        return saved;
    }

    @Transactional
    public PluginApp disableApp(String tenantId, String appId) {
        PluginApp current = requireOwnedApp(tenantId, appId);
        if ("disabled".equals(current.status())) {
            return current;
        }
        PluginApp updated = pluginAppRepository.save(new PluginApp(
                current.appId(),
                current.organizationId(),
                current.appName(),
                current.appType(),
                current.permissionScope(),
                current.accessKey(),
                current.secretMasked(),
                "disabled",
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "DISABLE_PLUGIN_APP", "plugin_app", appId);
        recordCallLog(
                tenantId,
                current.organizationId(),
                current.appId(),
                null,
                null,
                "/api/open/apps/" + appId + "/disable",
                "management",
                "disabled",
                true,
                false,
                "plugin app disabled"
        );
        return updated;
    }

    @Transactional
    public PluginApp enableApp(String tenantId, String appId) {
        PluginApp current = requireOwnedApp(tenantId, appId);
        if ("active".equals(current.status())) {
            return current;
        }
        PluginApp updated = pluginAppRepository.save(new PluginApp(
                current.appId(),
                current.organizationId(),
                current.appName(),
                current.appType(),
                current.permissionScope(),
                current.accessKey(),
                current.secretMasked(),
                "active",
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "ENABLE_PLUGIN_APP", "plugin_app", appId);
        recordCallLog(
                tenantId,
                current.organizationId(),
                current.appId(),
                null,
                null,
                "/api/open/apps/" + appId + "/enable",
                "management",
                "enabled",
                true,
                false,
                "plugin app enabled"
        );
        return updated;
    }

    @Transactional
    public IssuedIntegrationCredential refreshCredential(String tenantId, String appId) {
        PluginApp current = requireOwnedApp(tenantId, appId);
        GeneratedCredential generatedCredential = generateCredential();
        IntegrationCredential currentCredential = integrationCredentialRepository
                .findByPluginAppIdAndCredentialType(appId, DEFAULT_CREDENTIAL_TYPE)
                .orElse(null);
        integrationCredentialRepository.save(new IntegrationCredential(
                currentCredential == null ? null : currentCredential.credentialId(),
                appId,
                DEFAULT_CREDENTIAL_TYPE,
                generatedCredential.accessKey(),
                generatedCredential.secretDigest(),
                generatedCredential.secretMasked(),
                generatedCredential.expiresAt(),
                generatedCredential.issuedAt()
        ));
        pluginAppRepository.save(new PluginApp(
                current.appId(),
                current.organizationId(),
                current.appName(),
                current.appType(),
                current.permissionScope(),
                generatedCredential.accessKey(),
                generatedCredential.secretMasked(),
                current.status(),
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "REFRESH_INTEGRATION_CREDENTIAL", "plugin_app", appId);
        recordCallLog(
                tenantId,
                current.organizationId(),
                current.appId(),
                null,
                null,
                "/api/open/apps/" + appId + "/credentials/refresh",
                "management",
                "credential_refreshed",
                true,
                false,
                "integration credential refreshed"
        );
        return new IssuedIntegrationCredential(
                current.appId(),
                DEFAULT_CREDENTIAL_TYPE,
                generatedCredential.accessKey(),
                generatedCredential.secret(),
                generatedCredential.secretMasked(),
                generatedCredential.expiresAt()
        );
    }

    public List<IntegrationCredentialView> listCredentials(String tenantId, String appId) {
        requireOwnedApp(tenantId, appId);
        return integrationCredentialRepository.findByPluginAppIdAndCredentialType(appId, DEFAULT_CREDENTIAL_TYPE)
                .map(credential -> List.of(toCredentialView(credential)))
                .orElseGet(List::of);
    }

    @Transactional
    public IntegrationCredentialView revokeCredential(String tenantId, String appId) {
        PluginApp current = requireOwnedApp(tenantId, appId);
        IntegrationCredential credential = integrationCredentialRepository.findByPluginAppIdAndCredentialType(appId, DEFAULT_CREDENTIAL_TYPE)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        IntegrationCredential revoked = integrationCredentialRepository.save(new IntegrationCredential(
                credential.credentialId(),
                credential.pluginAppId(),
                credential.credentialType(),
                credential.accessKey(),
                credential.secretDigest(),
                credential.secretKeyMasked(),
                OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1),
                credential.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "REVOKE_INTEGRATION_CREDENTIAL", "plugin_app", appId);
        recordCallLog(
                tenantId,
                current.organizationId(),
                current.appId(),
                null,
                null,
                "/api/open/apps/" + appId + "/credentials/revoke",
                "management",
                "credential_revoked",
                true,
                false,
                "integration credential revoked"
        );
        return toCredentialView(revoked);
    }

    public List<WebhookSubscription> listWebhooks(String tenantId) {
        List<String> organizationIds = ownedOrganizationIds(tenantId);
        if (organizationIds.isEmpty()) {
            return List.of();
        }
        return webhookSubscriptionRepository.findByOrganizationIds(organizationIds);
    }

    public WebhookSubscription createWebhook(String tenantId, CreateWebhookSubscriptionCommand command) {
        requireOwnedOrganization(tenantId, command.organizationId());
        validateCallbackUrl(command.callbackUrl());
        webhookSubscriptionRepository.findByOrganizationIdAndEventCodeAndCallbackUrl(
                        command.organizationId(),
                        command.eventCode().trim(),
                        command.callbackUrl().trim()
                )
                .ifPresent(existing -> {
                    throw new BusinessException("1008", "webhook subscription already exists", HttpStatus.BAD_REQUEST);
                });

        WebhookSubscription saved = webhookSubscriptionRepository.save(new WebhookSubscription(
                null,
                command.organizationId(),
                command.eventCode().trim(),
                command.callbackUrl().trim(),
                generateToken("whsec", 28),
                "enabled",
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_WEBHOOK_SUBSCRIPTION", "webhook_subscription", saved.subscriptionId());
        recordCallLog(
                tenantId,
                command.organizationId(),
                null,
                saved.subscriptionId(),
                null,
                "/api/open/webhooks",
                "management",
                "created",
                true,
                false,
                "webhook subscription created"
        );
        return saved;
    }

    public WebhookSubscription disableWebhook(String tenantId, String subscriptionId) {
        WebhookSubscription current = requireOwnedWebhook(tenantId, subscriptionId);
        if ("disabled".equals(current.status())) {
            return current;
        }
        WebhookSubscription updated = webhookSubscriptionRepository.save(new WebhookSubscription(
                current.subscriptionId(),
                current.organizationId(),
                current.eventCode(),
                current.callbackUrl(),
                current.secretToken(),
                "disabled",
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "DISABLE_WEBHOOK_SUBSCRIPTION", "webhook_subscription", subscriptionId);
        recordCallLog(
                tenantId,
                current.organizationId(),
                null,
                current.subscriptionId(),
                null,
                "/api/open/webhooks/" + subscriptionId + "/disable",
                "management",
                "disabled",
                true,
                false,
                "webhook subscription disabled"
        );
        return updated;
    }

    public WebhookSubscription enableWebhook(String tenantId, String subscriptionId) {
        WebhookSubscription current = requireOwnedWebhook(tenantId, subscriptionId);
        if ("enabled".equals(current.status())) {
            return current;
        }
        WebhookSubscription updated = webhookSubscriptionRepository.save(new WebhookSubscription(
                current.subscriptionId(),
                current.organizationId(),
                current.eventCode(),
                current.callbackUrl(),
                current.secretToken(),
                "enabled",
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "ENABLE_WEBHOOK_SUBSCRIPTION", "webhook_subscription", subscriptionId);
        recordCallLog(
                tenantId,
                current.organizationId(),
                null,
                current.subscriptionId(),
                null,
                "/api/open/webhooks/" + subscriptionId + "/enable",
                "management",
                "enabled",
                true,
                false,
                "webhook subscription enabled"
        );
        return updated;
    }

    public WebhookSubscription rotateWebhookSecret(String tenantId, String subscriptionId) {
        WebhookSubscription current = requireOwnedWebhook(tenantId, subscriptionId);
        WebhookSubscription updated = webhookSubscriptionRepository.save(new WebhookSubscription(
                current.subscriptionId(),
                current.organizationId(),
                current.eventCode(),
                current.callbackUrl(),
                generateToken("whsec", 28),
                current.status(),
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "ROTATE_WEBHOOK_SECRET", "webhook_subscription", subscriptionId);
        recordCallLog(
                tenantId,
                current.organizationId(),
                null,
                current.subscriptionId(),
                null,
                "/api/open/webhooks/" + subscriptionId + "/secret/rotate",
                "management",
                "secret_rotated",
                true,
                false,
                "webhook secret rotated"
        );
        return updated;
    }

    public List<OpenPlatformCallLog> listCallLogs(String tenantId) {
        return openPlatformCallLogRepository.findByTenantId(tenantId);
    }

    public List<WebhookOrchestrationView> listWebhookOrchestrations(String tenantId) {
        return listWebhooks(tenantId).stream()
                .map(webhook -> {
                    List<OpenPlatformCallLog> callbackLogs = listCallLogs(tenantId).stream()
                            .filter(log -> webhook.subscriptionId().equals(log.subscriptionId()))
                            .filter(log -> "callback_inbound".equals(log.direction()))
                            .toList();
                    OpenPlatformCallLog lastLog = callbackLogs.stream()
                            .max(java.util.Comparator.comparing(OpenPlatformCallLog::createdAt))
                            .orElse(null);
                    return new WebhookOrchestrationView(
                            webhook.subscriptionId(),
                            webhook.organizationId(),
                            webhook.eventCode(),
                            webhook.callbackUrl(),
                            webhook.status(),
                            callbackLogs.size(),
                            (int) callbackLogs.stream().filter(log -> "accepted".equals(log.resultStatus())).count(),
                            (int) callbackLogs.stream().filter(log -> log.resultStatus() != null && log.resultStatus().startsWith("rejected")).count(),
                            (int) callbackLogs.stream().filter(log -> "rejected_replay".equals(log.resultStatus())).count(),
                            (int) callbackLogs.stream().filter(log -> "rejected_signature".equals(log.resultStatus())).count(),
                            lastLog == null ? null : lastLog.resultStatus(),
                            lastLog == null ? null : lastLog.traceId(),
                            webhook.createdAt()
                    );
                })
                .toList();
    }

    public IntegrationAuditOverviewView getIntegrationAudit(String tenantId) {
        List<OpenPlatformCallLog> logs = listCallLogs(tenantId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int revokedCredentialCount = 0;
        int expiringCredentialCount = 0;
        for (PluginApp app : listApps(tenantId)) {
            IntegrationCredential credential = integrationCredentialRepository
                    .findByPluginAppIdAndCredentialType(app.appId(), DEFAULT_CREDENTIAL_TYPE)
                    .orElse(null);
            if (credential == null) {
                continue;
            }
            if (!isCredentialActive(credential, now)) {
                revokedCredentialCount++;
            } else if (credential.expiresAt() != null && !credential.expiresAt().isAfter(now.plusDays(7))) {
                expiringCredentialCount++;
            }
        }
        return new IntegrationAuditOverviewView(
                logs.size(),
                (int) logs.stream().filter(log -> "external_inbound".equals(log.direction()) && "authorized".equals(log.resultStatus())).count(),
                (int) logs.stream().filter(log -> "callback_inbound".equals(log.direction()) && "accepted".equals(log.resultStatus())).count(),
                (int) logs.stream().filter(log -> "callback_inbound".equals(log.direction()) && log.resultStatus().startsWith("rejected")).count(),
                (int) logs.stream().filter(log -> "rejected_scope".equals(log.resultStatus())).count(),
                (int) logs.stream().filter(log -> "rejected_replay".equals(log.resultStatus())).count(),
                (int) logs.stream().filter(log -> "rejected_signature".equals(log.resultStatus())).count(),
                (int) listApps(tenantId).stream().filter(app -> "disabled".equals(app.status())).count(),
                revokedCredentialCount,
                expiringCredentialCount
        );
    }

    public OpenPlatformOverviewView getOverview(String tenantId) {
        List<PluginApp> apps = listApps(tenantId);
        List<WebhookSubscription> webhooks = listWebhooks(tenantId);
        List<OpenPlatformCallLog> logs = listCallLogs(tenantId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int activeCredentialCount = 0;
        int revokedCredentialCount = 0;
        int expiringCredentialCount = 0;
        for (PluginApp app : apps) {
            IntegrationCredential credential = integrationCredentialRepository
                    .findByPluginAppIdAndCredentialType(app.appId(), DEFAULT_CREDENTIAL_TYPE)
                    .orElse(null);
            if (credential == null) {
                continue;
            }
            if (isCredentialActive(credential, now)) {
                activeCredentialCount++;
                if (credential.expiresAt() != null && !credential.expiresAt().isAfter(now.plusDays(7))) {
                    expiringCredentialCount++;
                }
            } else {
                revokedCredentialCount++;
            }
        }
        return new OpenPlatformOverviewView(
                apps.size(),
                (int) apps.stream().filter(app -> "active".equals(app.status())).count(),
                (int) apps.stream().filter(app -> "disabled".equals(app.status())).count(),
                webhooks.size(),
                (int) webhooks.stream().filter(webhook -> "enabled".equals(webhook.status())).count(),
                (int) webhooks.stream().filter(webhook -> "disabled".equals(webhook.status())).count(),
                activeCredentialCount,
                revokedCredentialCount,
                expiringCredentialCount,
                logs.size()
        );
    }

    public ExternalAppProfile authenticateExternalProfile(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, (List<String>) null);
    }

    public ExternalAppProfile authorizeExternalFinanceRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, ERP_FINANCE_READ_SCOPE);
    }

    public ExternalAppProfile authorizeExternalErpMasterDataRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, List.of(
                ERP_MASTER_DATA_READ_SCOPE,
                ERP_FINANCE_READ_SCOPE
        ));
    }

    public ExternalAppProfile authorizeExternalErpAccountMappingRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, List.of(
                ERP_ACCOUNT_MAPPING_READ_SCOPE,
                ERP_FINANCE_READ_SCOPE
        ));
    }

    public ExternalAppProfile authorizeExternalErpIntegrationBaselineRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, List.of(
                ERP_INTEGRATION_BASELINE_READ_SCOPE,
                ERP_FINANCE_READ_SCOPE
        ));
    }

    private ExternalAppProfile authenticateExternalProfile(String accessKey,
                                                           String secret,
                                                           String endpoint,
                                                           String requiredScope) {
        return authenticateExternalProfile(
                accessKey,
                secret,
                endpoint,
                requiredScope == null ? null : List.of(requiredScope)
        );
    }

    private ExternalAppProfile authenticateExternalProfile(String accessKey,
                                                           String secret,
                                                           String endpoint,
                                                           List<String> requiredScopes) {
        IntegrationCredential credential = integrationCredentialRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new BusinessException("1008", "integration credential invalid", HttpStatus.FORBIDDEN));
        PluginApp pluginApp = pluginAppRepository.findByAppId(credential.pluginAppId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        Organization organization = organizationRepository.findById(pluginApp.organizationId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));

        if (!"active".equals(pluginApp.status())) {
            recordCallLog(
                    organization.tenantId(),
                    organization.id(),
                    pluginApp.appId(),
                    null,
                    null,
                    endpoint,
                    "external_inbound",
                    "rejected_disabled",
                    false,
                    false,
                    "plugin app disabled"
            );
            throw new BusinessException("1008", "plugin app disabled", HttpStatus.FORBIDDEN);
        }

        if (credential.expiresAt() != null && credential.expiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            recordCallLog(
                    organization.tenantId(),
                    organization.id(),
                    pluginApp.appId(),
                    null,
                    null,
                    endpoint,
                    "external_inbound",
                    "rejected_expired",
                    false,
                    false,
                    "integration credential expired"
            );
            throw new BusinessException("1008", "integration credential expired", HttpStatus.FORBIDDEN);
        }

        if (!constantTimeEquals(credential.secretDigest(), digestSecret(secret))) {
            recordCallLog(
                    organization.tenantId(),
                    organization.id(),
                    pluginApp.appId(),
                    null,
                    null,
                    endpoint,
                    "external_inbound",
                    "rejected_secret",
                    false,
                    false,
                    "integration credential secret invalid"
            );
            throw new BusinessException("1008", "integration credential invalid", HttpStatus.FORBIDDEN);
        }

        if (requiredScopes != null
                && requiredScopes.stream().noneMatch(scope -> pluginApp.permissionScope().contains(scope))) {
            recordCallLog(
                    organization.tenantId(),
                    organization.id(),
                    pluginApp.appId(),
                    null,
                    null,
                    endpoint,
                    "external_inbound",
                    "rejected_scope",
                    false,
                    false,
                    "integration permission scope denied"
            );
            throw new BusinessException("1009", "integration permission scope denied", HttpStatus.FORBIDDEN);
        }

        recordCallLog(
                organization.tenantId(),
                organization.id(),
                pluginApp.appId(),
                null,
                null,
                endpoint,
                "external_inbound",
                "authorized",
                true,
                false,
                "integration credential authorized"
        );
        auditLogService.recordForTenant(organization.tenantId(), "AUTHORIZE_OPEN_APP_CREDENTIAL", "plugin_app", pluginApp.appId());
        return new ExternalAppProfile(
                organization.tenantId(),
                organization.id(),
                organization.organizationName(),
                pluginApp.appId(),
                pluginApp.appName(),
                pluginApp.appType(),
                pluginApp.permissionScope(),
                credential.credentialType(),
                credential.expiresAt()
        );
    }

    public OpenCallbackReceiveResult receiveCallback(String subscriptionId, ReceiveOpenCallbackCommand command) {
        WebhookSubscription subscription = webhookSubscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        Organization organization = organizationRepository.findById(subscription.organizationId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));

        if (!"enabled".equals(subscription.status())) {
            recordCallLog(
                    organization.tenantId(),
                    subscription.organizationId(),
                    null,
                    subscription.subscriptionId(),
                    command.requestId(),
                    command.endpoint(),
                    "callback_inbound",
                    "rejected_disabled",
                    false,
                    false,
                    "webhook subscription disabled"
            );
            throw new BusinessException("1008", "webhook subscription disabled", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime callbackTimestamp = parseTimestamp(command.timestamp(), organization.tenantId(), subscription, command);
        validateTimestampFreshness(callbackTimestamp, organization.tenantId(), subscription, command);

        String expectedSignature = signCallback(
                subscription.secretToken(),
                subscription.subscriptionId(),
                command.timestamp(),
                command.nonce(),
                command.payload()
        );
        if (!constantTimeEquals(expectedSignature, command.signature())) {
            recordCallLog(
                    organization.tenantId(),
                    subscription.organizationId(),
                    null,
                    subscription.subscriptionId(),
                    command.requestId(),
                    command.endpoint(),
                    "callback_inbound",
                    "rejected_signature",
                    false,
                    false,
                    "callback signature invalid"
            );
            throw new BusinessException("1008", "callback signature invalid", HttpStatus.FORBIDDEN);
        }

        if (openCallbackReplayRepository.exists(subscription.subscriptionId(), command.requestId())) {
            recordCallLog(
                    organization.tenantId(),
                    subscription.organizationId(),
                    null,
                    subscription.subscriptionId(),
                    command.requestId(),
                    command.endpoint(),
                    "callback_inbound",
                    "rejected_replay",
                    true,
                    true,
                    "callback request replayed"
            );
            throw new BusinessException("1008", "callback request replayed", HttpStatus.CONFLICT);
        }

        openCallbackReplayRepository.save(new OpenCallbackReplayRecord(
                subscription.subscriptionId(),
                command.requestId(),
                OffsetDateTime.now()
        ));
        recordCallLog(
                organization.tenantId(),
                subscription.organizationId(),
                null,
                subscription.subscriptionId(),
                command.requestId(),
                command.endpoint(),
                "callback_inbound",
                "accepted",
                true,
                false,
                "callback verified"
        );
        auditLogService.recordForTenant(organization.tenantId(), "VERIFY_OPEN_CALLBACK", "webhook_subscription", subscription.subscriptionId());
        return new OpenCallbackReceiveResult(
                subscription.subscriptionId(),
                command.requestId(),
                true,
                false,
                "accepted"
        );
    }

    public void clear() {
        openCallbackReplayRepository.deleteAll();
        openPlatformCallLogRepository.deleteAll();
        webhookSubscriptionRepository.deleteAll();
        integrationCredentialRepository.deleteAll();
        pluginAppRepository.deleteAll();
    }

    private Organization requireOwnedOrganization(String tenantId, String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(organization.tenantId())) {
            throw new BusinessException("1005", "tenant context invalid", HttpStatus.FORBIDDEN);
        }
        return organization;
    }

    private WebhookSubscription requireOwnedWebhook(String tenantId, String subscriptionId) {
        WebhookSubscription subscription = webhookSubscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedOrganization(tenantId, subscription.organizationId());
        return subscription;
    }

    private PluginApp requireOwnedApp(String tenantId, String appId) {
        PluginApp pluginApp = pluginAppRepository.findByAppId(appId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedOrganization(tenantId, pluginApp.organizationId());
        return pluginApp;
    }

    private List<String> ownedOrganizationIds(String tenantId) {
        return organizationRepository.findByTenantId(tenantId).stream()
                .map(Organization::id)
                .toList();
    }

    private List<String> normalizePermissionScope(List<String> permissionScope) {
        if (permissionScope == null || permissionScope.isEmpty()) {
            throw new BusinessException("1004", "permissionScope is required", HttpStatus.BAD_REQUEST);
        }
        List<String> normalized = permissionScope.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
        if (normalized.isEmpty()) {
            throw new BusinessException("1004", "permissionScope is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private IntegrationCredentialView toCredentialView(IntegrationCredential credential) {
        return new IntegrationCredentialView(
                credential.credentialId(),
                credential.pluginAppId(),
                credential.credentialType(),
                credential.accessKey(),
                credential.secretKeyMasked(),
                credential.createdAt(),
                credential.expiresAt(),
                isCredentialActive(credential, OffsetDateTime.now(ZoneOffset.UTC))
        );
    }

    private boolean isCredentialActive(IntegrationCredential credential, OffsetDateTime now) {
        return credential.expiresAt() == null || credential.expiresAt().isAfter(now);
    }

    private void validateCallbackUrl(String callbackUrl) {
        String normalized = callbackUrl == null ? "" : callbackUrl.trim();
        if (!(normalized.startsWith("http://") || normalized.startsWith("https://"))) {
            throw new BusinessException("1004", "callbackUrl must start with http:// or https://", HttpStatus.BAD_REQUEST);
        }
    }

    private OffsetDateTime parseTimestamp(String timestamp,
                                          String tenantId,
                                          WebhookSubscription subscription,
                                          ReceiveOpenCallbackCommand command) {
        try {
            return OffsetDateTime.parse(timestamp);
        } catch (DateTimeParseException exception) {
            recordCallLog(
                    tenantId,
                    subscription.organizationId(),
                    null,
                    subscription.subscriptionId(),
                    command.requestId(),
                    command.endpoint(),
                    "callback_inbound",
                    "rejected_timestamp",
                    false,
                    false,
                    "callback timestamp invalid"
            );
            throw new BusinessException("1004", "callback timestamp invalid", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTimestampFreshness(OffsetDateTime callbackTimestamp,
                                            String tenantId,
                                            WebhookSubscription subscription,
                                            ReceiveOpenCallbackCommand command) {
        Duration drift = Duration.between(callbackTimestamp.toInstant(), OffsetDateTime.now(ZoneOffset.UTC).toInstant()).abs();
        if (drift.compareTo(CALLBACK_FRESHNESS_WINDOW) > 0) {
            recordCallLog(
                    tenantId,
                    subscription.organizationId(),
                    null,
                    subscription.subscriptionId(),
                    command.requestId(),
                    command.endpoint(),
                    "callback_inbound",
                    "rejected_expired",
                    false,
                    false,
                    "callback timestamp expired"
            );
            throw new BusinessException("1008", "callback timestamp expired", HttpStatus.BAD_REQUEST);
        }
    }

    private void recordCallLog(String tenantId,
                               String organizationId,
                               String appId,
                               String subscriptionId,
                               String requestId,
                               String endpoint,
                               String direction,
                               String resultStatus,
                               boolean signatureVerified,
                               boolean replayed,
                               String message) {
        openPlatformCallLogRepository.save(new OpenPlatformCallLog(
                null,
                tenantId,
                organizationId,
                appId,
                subscriptionId,
                requestId,
                endpoint,
                direction,
                SOURCE_MODULE,
                resultStatus,
                signatureVerified,
                replayed,
                TraceIdHolder.get(),
                message,
                OffsetDateTime.now()
        ));
    }

    private String signCallback(String secretToken,
                                String subscriptionId,
                                String timestamp,
                                String nonce,
                                String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String content = subscriptionId + "\n" + timestamp + "\n" + nonce + "\n" + payload;
            return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("open callback sign failed", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int diff = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            diff |= leftBytes[index] ^ rightBytes[index];
        }
        return diff == 0;
    }

    private String generateToken(String prefix, int length) {
        StringBuilder builder = new StringBuilder(prefix).append('_');
        while (builder.length() < prefix.length() + 1 + length) {
            builder.append(UUID.randomUUID().toString().replace("-", ""));
        }
        return builder.substring(0, prefix.length() + 1 + length);
    }

    private GeneratedCredential generateCredential() {
        String accessKey = generateToken("ak", 20);
        String secret = generateToken("sk", 32);
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return new GeneratedCredential(
                accessKey,
                secret,
                maskSecret(secret),
                digestSecret(secret),
                issuedAt.plus(CREDENTIAL_EXPIRE_DURATION),
                issuedAt
        );
    }

    private String digestSecret(String secret) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("integration credential digest failed", exception);
        }
    }

    private String maskSecret(String secret) {
        int suffixLength = Math.min(4, secret.length());
        return secret.substring(0, Math.min(4, secret.length()))
                + "****"
                + secret.substring(secret.length() - suffixLength);
    }

    public record CreatePluginAppCommand(
            String organizationId,
            String appName,
            String appType,
            List<String> permissionScope
    ) {
    }

    public record CreateWebhookSubscriptionCommand(
            String organizationId,
            String eventCode,
            String callbackUrl
    ) {
    }

    public record ReceiveOpenCallbackCommand(
            String requestId,
            String timestamp,
            String nonce,
            String signature,
            String payload,
            String endpoint
    ) {
    }

    public record IssuedIntegrationCredential(
            String appId,
            String credentialType,
            String accessKey,
            String secret,
            String secretMasked,
            OffsetDateTime expiresAt
    ) {
    }

    public record IntegrationCredentialView(
            String credentialId,
            String appId,
            String credentialType,
            String accessKey,
            String secretMasked,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt,
            boolean active
    ) {
    }

    public record OpenPlatformOverviewView(
            int appCount,
            int activeAppCount,
            int disabledAppCount,
            int webhookCount,
            int enabledWebhookCount,
            int disabledWebhookCount,
            int activeCredentialCount,
            int revokedCredentialCount,
            int expiringCredentialCount,
            int totalCallLogCount
    ) {
    }

    public record WebhookOrchestrationView(
            String subscriptionId,
            String organizationId,
            String eventCode,
            String callbackUrl,
            String status,
            int callbackAttemptCount,
            int acceptedCallbackCount,
            int rejectedCallbackCount,
            int replayRejectedCount,
            int signatureRejectedCount,
            String lastResultStatus,
            String lastTraceId,
            OffsetDateTime createdAt
    ) {
    }

    public record IntegrationAuditOverviewView(
            int totalCallLogCount,
            int externalAuthorizedCount,
            int callbackAcceptedCount,
            int callbackRejectedCount,
            int scopeRejectedCount,
            int replayRejectedCount,
            int signatureRejectedCount,
            int disabledAppCount,
            int revokedCredentialCount,
            int expiringCredentialCount
    ) {
    }

    public record ExternalAppProfile(
            String tenantId,
            String organizationId,
            String organizationName,
            String appId,
            String appName,
            String appType,
            List<String> permissionScope,
            String credentialType,
            OffsetDateTime expiresAt
    ) {
    }

    private record GeneratedCredential(
            String accessKey,
            String secret,
            String secretMasked,
            String secretDigest,
            OffsetDateTime expiresAt,
            OffsetDateTime issuedAt
    ) {
    }
}
