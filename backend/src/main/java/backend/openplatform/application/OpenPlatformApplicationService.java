package backend.openplatform.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.common.trace.TraceIdHolder;
import backend.saas.application.SaasTenantService;
import backend.saas.application.SaasTenantService.ExternalIntegrationConnectivitySnapshot;
import backend.saas.application.SaasTenantService.ExternalSystemConnectivitySnapshot;
import backend.openplatform.domain.repository.IntegrationCredentialRepository;
import backend.openplatform.domain.repository.OpenCallbackReplayRepository;
import backend.openplatform.domain.repository.OpenPlatformCallLogRepository;
import backend.openplatform.domain.repository.PluginAppRepository;
import backend.openplatform.domain.repository.WebhookSubscriptionRepository;
import backend.openplatform.model.IntegrationCredential;
import backend.openplatform.model.OpenCallbackReplayRecord;
import backend.openplatform.model.OpenCallbackReceiveResult;
import backend.openplatform.model.OpenPlatformCallLog;
import backend.openplatform.model.PluginApp;
import backend.openplatform.model.WebhookSubscription;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OpenPlatformApplicationService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Duration CALLBACK_FRESHNESS_WINDOW = Duration.ofMinutes(5);
    private static final Duration CREDENTIAL_EXPIRE_DURATION = Duration.ofDays(90);
    private static final String DEFAULT_CREDENTIAL_TYPE = "default";
    private static final String ERP_MASTER_DATA_READ_SCOPE = "erp.master_data.read";
    private static final String ERP_ACCOUNT_MAPPING_READ_SCOPE = "erp.account_mapping.read";
    private static final String ERP_INTEGRATION_BASELINE_READ_SCOPE = "erp.integration_baseline.read";
    private static final String ERP_OFBIZ_BASELINE_READ_SCOPE = "erp.ofbiz_baseline.read";
    private static final String WMS_LINKAGE_READ_SCOPE = "wms.linkage.read";
    private static final String WMS_OPENBOXES_BASELINE_READ_SCOPE = "wms.openboxes_baseline.read";
    private static final String TMS_CONTROL_TOWER_READ_SCOPE = "tms.control_tower.read";
    private static final String MESSAGING_RABBITMQ_BASELINE_READ_SCOPE = "messaging.rabbitmq_baseline.read";
    private static final String MESSAGING_CALLBACK_BRIDGE_READ_SCOPE = "messaging.callback_bridge.read";
    private static final String BI_SUPERSET_OVERVIEW_READ_SCOPE = "bi.superset_overview.read";
    private static final String SYSTEM_OBSERVABILITY_READINESS_READ_SCOPE = "system.observability_readiness.read";
    private static final String DELIVERY_READINESS_READ_SCOPE = "delivery.readiness.read";
    private static final Set<String> SUPPORTED_PERMISSION_SCOPES = Set.of(
            ERP_MASTER_DATA_READ_SCOPE,
            ERP_ACCOUNT_MAPPING_READ_SCOPE,
            ERP_INTEGRATION_BASELINE_READ_SCOPE,
            ERP_OFBIZ_BASELINE_READ_SCOPE,
            WMS_LINKAGE_READ_SCOPE,
            WMS_OPENBOXES_BASELINE_READ_SCOPE,
            TMS_CONTROL_TOWER_READ_SCOPE,
            MESSAGING_RABBITMQ_BASELINE_READ_SCOPE,
            MESSAGING_CALLBACK_BRIDGE_READ_SCOPE,
            BI_SUPERSET_OVERVIEW_READ_SCOPE,
            SYSTEM_OBSERVABILITY_READINESS_READ_SCOPE,
            DELIVERY_READINESS_READ_SCOPE
    );
    private static final String SOURCE_MODULE = "openplatform";

    private final AuditLogService auditLogService;
    private final OrganizationRepository organizationRepository;
    private final PluginAppRepository pluginAppRepository;
    private final IntegrationCredentialRepository integrationCredentialRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final OpenPlatformCallLogRepository openPlatformCallLogRepository;
    private final OpenCallbackReplayRepository openCallbackReplayRepository;
    private final SaasTenantService saasTenantService;
    private final Environment environment;

    public OpenPlatformApplicationService(AuditLogService auditLogService,
                                          OrganizationRepository organizationRepository,
                                          PluginAppRepository pluginAppRepository,
                                          IntegrationCredentialRepository integrationCredentialRepository,
                                          WebhookSubscriptionRepository webhookSubscriptionRepository,
                                          OpenPlatformCallLogRepository openPlatformCallLogRepository,
                                          OpenCallbackReplayRepository openCallbackReplayRepository,
                                          SaasTenantService saasTenantService,
                                          Environment environment) {
        this.auditLogService = auditLogService;
        this.organizationRepository = organizationRepository;
        this.pluginAppRepository = pluginAppRepository;
        this.integrationCredentialRepository = integrationCredentialRepository;
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.openPlatformCallLogRepository = openPlatformCallLogRepository;
        this.openCallbackReplayRepository = openCallbackReplayRepository;
        this.saasTenantService = saasTenantService;
        this.environment = environment;
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

    public PluginGovernanceOverviewView getPluginGovernance(String tenantId) {
        List<PluginApp> apps = listApps(tenantId);
        List<WebhookSubscription> webhooks = listWebhooks(tenantId);
        List<OpenPlatformCallLog> logs = listCallLogs(tenantId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, IntegrationCredential> credentialsByAppId = new HashMap<>();
        for (PluginApp app : apps) {
            integrationCredentialRepository.findByPluginAppIdAndCredentialType(app.appId(), DEFAULT_CREDENTIAL_TYPE)
                    .ifPresent(credential -> credentialsByAppId.put(app.appId(), credential));
        }
        Map<String, List<WebhookSubscription>> webhooksByOrganizationId = webhooks.stream()
                .collect(Collectors.groupingBy(WebhookSubscription::organizationId));
        Map<String, List<OpenPlatformCallLog>> logsByOrganizationId = logs.stream()
                .filter(log -> log.organizationId() != null && !log.organizationId().isBlank())
                .collect(Collectors.groupingBy(OpenPlatformCallLog::organizationId));
        List<PluginGovernanceEntryView> entries = apps.stream()
                .map(app -> buildPluginGovernanceEntry(
                        app,
                        credentialsByAppId.get(app.appId()),
                        webhooksByOrganizationId.getOrDefault(app.organizationId(), List.of()),
                        logsByOrganizationId.getOrDefault(app.organizationId(), List.of()),
                        now
                ))
                .sorted(Comparator
                        .comparingInt((PluginGovernanceEntryView entry) -> governanceRiskWeight(entry.governanceRiskLevel()))
                        .reversed()
                        .thenComparing(PluginGovernanceEntryView::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PluginGovernanceEntryView::appName))
                .toList();
        List<String> recommendedActions = entries.stream()
                .filter(entry -> !"low".equals(entry.governanceRiskLevel()))
                .map(PluginGovernanceEntryView::recommendedAction)
                .filter(action -> action != null && !action.isBlank())
                .distinct()
                .limit(4)
                .toList();
        int appsMissingCredentials = (int) entries.stream()
                .filter(entry -> "missing".equals(entry.credentialStatus()))
                .count();
        int appsWithRejectedTraffic = (int) entries.stream()
                .filter(entry -> entry.rejectedCallCount() > 0)
                .count();
        int organizationsWithoutWebhooks = (int) entries.stream()
                .filter(entry -> "missing".equals(entry.webhookStatus()))
                .map(PluginGovernanceEntryView::organizationId)
                .distinct()
                .count();
        return new PluginGovernanceOverviewView(
                entries.size(),
                (int) entries.stream().filter(entry -> "active".equals(entry.appStatus())).count(),
                (int) entries.stream().filter(entry -> !"low".equals(entry.governanceRiskLevel())).count(),
                appsMissingCredentials,
                appsWithRejectedTraffic,
                organizationsWithoutWebhooks,
                recommendedActions,
                entries
        );
    }

    public ExternalAppProfile authenticateExternalProfile(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, (List<String>) null);
    }

    public ExternalAppProfile authorizeExternalErpMasterDataRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, ERP_MASTER_DATA_READ_SCOPE, "erp");
    }

    public ExternalAppProfile authorizeExternalErpAccountMappingRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, ERP_ACCOUNT_MAPPING_READ_SCOPE, "erp");
    }

    public ExternalAppProfile authorizeExternalErpIntegrationBaselineRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, ERP_INTEGRATION_BASELINE_READ_SCOPE, "erp");
    }

    public ExternalAppProfile authorizeExternalWmsLinkageRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, WMS_LINKAGE_READ_SCOPE, "wms");
    }

    public ExternalAppProfile authorizeExternalTmsControlTowerRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, TMS_CONTROL_TOWER_READ_SCOPE);
    }

    public ExternalAppProfile authorizeExternalErpOfbizBaselineRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, List.of(
                ERP_OFBIZ_BASELINE_READ_SCOPE,
                ERP_INTEGRATION_BASELINE_READ_SCOPE
        ), "erp");
    }

    public ExternalAppProfile authorizeExternalWmsOpenboxesBaselineRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, List.of(
                WMS_OPENBOXES_BASELINE_READ_SCOPE,
                WMS_LINKAGE_READ_SCOPE
        ), "wms");
    }

    public ExternalAppProfile authorizeExternalMessagingRabbitMqBaselineRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, MESSAGING_RABBITMQ_BASELINE_READ_SCOPE, "messaging");
    }

    public ExternalAppProfile authorizeExternalMessagingCallbackBridgeRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, MESSAGING_CALLBACK_BRIDGE_READ_SCOPE, "messaging");
    }

    public ExternalAppProfile authorizeExternalBiSupersetOverviewRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, BI_SUPERSET_OVERVIEW_READ_SCOPE, "bi");
    }

    public ExternalAppProfile authorizeExternalObservabilityReadinessRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, SYSTEM_OBSERVABILITY_READINESS_READ_SCOPE);
    }

    public ExternalAppProfile authorizeExternalDeliveryReadinessRead(String accessKey, String secret, String endpoint) {
        return authenticateExternalProfile(accessKey, secret, endpoint, DELIVERY_READINESS_READ_SCOPE);
    }

    public ExternalErpBaselineView getExternalErpOfbizBaseline() {
        String endpoint = environment.getProperty("app.integrations.external.erp.endpoint", "");
        ExternalSystemConnectionReadiness readiness = evaluateExternalSystemConnectionReadiness("erp");
        ExternalSystemConnectivitySnapshot connectivitySnapshot = findConnectivitySnapshot("erp");
        return new ExternalErpBaselineView(
                normalizedProperty("app.integrations.external.erp.provider", "ofbiz"),
                endpoint != null && !endpoint.isBlank(),
                extractHost(endpoint),
                maskEndpoint(endpoint),
                environment.getProperty("app.integrations.external.erp.party-sync-enabled", Boolean.class, false),
                normalizedProperty("app.integrations.external.erp.order-sync-mode", "manual"),
                environment.getProperty("app.integrations.external.erp.ledger-mapping-count", Integer.class, 0),
                environment.getProperty("app.integrations.external.erp.catalog-export-enabled", Boolean.class, false),
                readiness.credentialConfigured(),
                readiness.readinessStatus(),
                readiness.missingParts(),
                connectivitySnapshot != null && connectivitySnapshot.reachable(),
                connectivitySnapshot == null ? "probe not available" : connectivitySnapshot.detail()
        );
    }

    public ExternalWmsBaselineView getExternalWmsOpenboxesBaseline() {
        String endpoint = environment.getProperty("app.integrations.external.wms.endpoint", "");
        ExternalSystemConnectionReadiness readiness = evaluateExternalSystemConnectionReadiness("wms");
        ExternalSystemConnectivitySnapshot connectivitySnapshot = findConnectivitySnapshot("wms");
        return new ExternalWmsBaselineView(
                normalizedProperty("app.integrations.external.wms.provider", "openboxes"),
                endpoint != null && !endpoint.isBlank(),
                extractHost(endpoint),
                maskEndpoint(endpoint),
                environment.getProperty("app.integrations.external.wms.facility-count", Integer.class, 0),
                normalizedProperty("app.integrations.external.wms.stock-sync-mode", "manual"),
                normalizedProperty("app.integrations.external.wms.outbound-flow", "manual"),
                environment.getProperty("app.integrations.external.wms.batch-tracking-enabled", Boolean.class, false),
                readiness.credentialConfigured(),
                readiness.readinessStatus(),
                readiness.missingParts(),
                connectivitySnapshot != null && connectivitySnapshot.reachable(),
                connectivitySnapshot == null ? "probe not available" : connectivitySnapshot.detail()
        );
    }

    public ExternalMessagingBaselineView getExternalMessagingRabbitMqBaseline() {
        String endpoint = environment.getProperty("app.integrations.external.messaging.endpoint", "");
        boolean callbackBridgeEnabled = environment.getProperty("app.integrations.external.messaging.callback-bridge-enabled", Boolean.class, false);
        ExternalCallbackWorkerReadiness callbackWorkerReadiness = evaluateCallbackWorkerReadiness(callbackBridgeEnabled);
        ExternalSystemConnectivitySnapshot callbackWorkerProbeSnapshot = probeCallbackWorker();
        ExternalSystemConnectionReadiness readiness = evaluateExternalSystemConnectionReadiness("messaging");
        ExternalSystemConnectivitySnapshot connectivitySnapshot = findConnectivitySnapshot("messaging");
        return new ExternalMessagingBaselineView(
                normalizedProperty("app.integrations.external.messaging.provider", "rabbitmq"),
                endpoint != null && !endpoint.isBlank(),
                extractHost(endpoint),
                maskEndpoint(endpoint),
                normalizedProperty("app.integrations.external.messaging.virtual-host", ""),
                normalizedProperty("app.integrations.external.messaging.exchange", ""),
                environment.getProperty("app.integrations.external.messaging.queue-count", Integer.class, 0),
                callbackBridgeEnabled,
                environment.getProperty("app.integrations.external.messaging.dead-letter-enabled", Boolean.class, false),
                readiness.credentialConfigured(),
                readiness.callbackRequired(),
                readiness.callbackUrlConfigured(),
                readiness.readinessStatus(),
                readiness.missingParts(),
                connectivitySnapshot != null && connectivitySnapshot.reachable(),
                connectivitySnapshot == null ? "probe not available" : connectivitySnapshot.detail(),
                callbackWorkerReadiness.enabled(),
                callbackWorkerReadiness.provider(),
                callbackWorkerReadiness.maskedEndpoint(),
                callbackWorkerReadiness.consumerGroup(),
                callbackWorkerReadiness.ready(),
                callbackWorkerReadiness.missingParts(),
                callbackWorkerProbeSnapshot.reachable(),
                callbackWorkerProbeSnapshot.detail()
        );
    }

    public ExternalMessagingCallbackBridgeOverviewView getExternalMessagingCallbackBridgeOverview(String tenantId) {
        String endpoint = environment.getProperty("app.integrations.external.messaging.endpoint", "");
        boolean configured = endpoint != null && !endpoint.isBlank();
        boolean callbackBridgeEnabled = environment.getProperty("app.integrations.external.messaging.callback-bridge-enabled", Boolean.class, false);
        boolean deadLetterEnabled = environment.getProperty("app.integrations.external.messaging.dead-letter-enabled", Boolean.class, false);
        ExternalCallbackWorkerReadiness callbackWorkerReadiness = evaluateCallbackWorkerReadiness(callbackBridgeEnabled);
        ExternalSystemConnectivitySnapshot callbackWorkerProbeSnapshot = probeCallbackWorker();
        ExternalSystemConnectionReadiness readiness = evaluateExternalSystemConnectionReadiness("messaging");
        ExternalSystemConnectivitySnapshot connectivitySnapshot = findConnectivitySnapshot("messaging");
        List<WebhookOrchestrationView> orchestrations = listWebhookOrchestrations(tenantId);
        List<OpenPlatformCallLog> callbackLogs = listCallLogs(tenantId).stream()
                .filter(log -> "callback_inbound".equals(log.direction()))
                .toList();
        OpenPlatformCallLog lastLog = callbackLogs.stream()
                .max(java.util.Comparator.comparing(OpenPlatformCallLog::createdAt))
                .orElse(null);
        String overviewStatus;
        if (!configured) {
            overviewStatus = "fallback_config_required";
        } else if (!callbackBridgeEnabled) {
            overviewStatus = "bridge_disabled";
        } else if (orchestrations.stream().noneMatch(orchestration -> "enabled".equals(orchestration.status()))) {
            overviewStatus = "callback_subscription_required";
        } else if (!callbackWorkerReadiness.ready()) {
            overviewStatus = "callback_worker_required";
        } else if (!callbackWorkerProbeSnapshot.reachable()) {
            overviewStatus = "callback_worker_unreachable";
        } else {
            overviewStatus = "ready";
        }
        return new ExternalMessagingCallbackBridgeOverviewView(
                normalizedProperty("app.integrations.external.messaging.provider", "rabbitmq"),
                overviewStatus,
                configured,
                extractHost(endpoint),
                maskEndpoint(endpoint),
                normalizedProperty("app.integrations.external.messaging.virtual-host", ""),
                normalizedProperty("app.integrations.external.messaging.exchange", ""),
                callbackBridgeEnabled,
                deadLetterEnabled,
                readiness.credentialConfigured(),
                readiness.callbackRequired(),
                readiness.callbackUrlConfigured(),
                readiness.readinessStatus(),
                readiness.missingParts(),
                connectivitySnapshot != null && connectivitySnapshot.reachable(),
                connectivitySnapshot == null ? "probe not available" : connectivitySnapshot.detail(),
                callbackWorkerReadiness.enabled(),
                callbackWorkerReadiness.provider(),
                callbackWorkerReadiness.maskedEndpoint(),
                callbackWorkerReadiness.consumerGroup(),
                callbackWorkerReadiness.ready(),
                callbackWorkerReadiness.missingParts(),
                callbackWorkerProbeSnapshot.reachable(),
                callbackWorkerProbeSnapshot.detail(),
                orchestrations.size(),
                (int) orchestrations.stream().filter(orchestration -> "enabled".equals(orchestration.status())).count(),
                callbackLogs.size(),
                (int) callbackLogs.stream().filter(log -> "accepted".equals(log.resultStatus())).count(),
                (int) callbackLogs.stream().filter(log -> log.resultStatus() != null && log.resultStatus().startsWith("rejected")).count(),
                (int) callbackLogs.stream().filter(log -> "rejected_replay".equals(log.resultStatus())).count(),
                (int) callbackLogs.stream().filter(log -> "rejected_signature".equals(log.resultStatus())).count(),
                lastLog == null ? null : lastLog.traceId(),
                lastLog == null ? null : lastLog.resultStatus()
        );
    }

    public ExternalObservabilityReadinessOverviewView getExternalObservabilityReadiness(String tenantId) {
        SaasTenantService.ObservabilityReadinessView view = saasTenantService.getObservabilityReadiness(tenantId);
        return new ExternalObservabilityReadinessOverviewView(
                view.tenantId(),
                view.stack().ready(),
                buildExternalObservabilityStackReadinessView(view.stack()),
                view.auditTraceability(),
                view.blockingReasons()
        );
    }

    public ExternalDeliveryReadinessOverviewView getExternalDeliveryReadiness(String tenantId) {
        SaasTenantService.DeliveryReadinessView view = saasTenantService.getDeliveryReadiness(tenantId);
        List<String> blockingReasons = buildExternalDeliveryBlockingReasons(view);
        return new ExternalDeliveryReadinessOverviewView(
                view.tenantId(),
                view.pipeline().repository(),
                view.pipeline().ready(),
                view.externalIntegrations().ready(),
                view.acceptance().ready(),
                view.pipeline(),
                view.externalIntegrations(),
                view.acceptance(),
                blockingReasons
        );
    }

    private List<String> buildExternalDeliveryBlockingReasons(SaasTenantService.DeliveryReadinessView view) {
        List<String> blockingReasons = new ArrayList<>();
        collectDeliveryPipelineBlockingReasons(view.pipeline(), blockingReasons);
        collectExternalIntegrationBlockingReasons(view.tenantId(), view.externalIntegrations(), blockingReasons);
        collectAcceptanceBlockingReasons(view.acceptance(), blockingReasons);
        return List.copyOf(blockingReasons);
    }

    private void collectDeliveryPipelineBlockingReasons(SaasTenantService.DeliveryPipelineSnapshot pipeline,
                                                        List<String> blockingReasons) {
        if (isBlank(pipeline.githubOwner())) {
            blockingReasons.add("delivery pipeline is missing github owner");
        }
        if (isBlank(pipeline.githubRepository())) {
            blockingReasons.add("delivery pipeline is missing github repository");
        }
        if (isBlank(pipeline.registry())) {
            blockingReasons.add("delivery pipeline is missing container registry");
        }
        if (isBlank(pipeline.imageRepository())) {
            blockingReasons.add("delivery pipeline is missing image repository");
        }
        collectDeliveryControlBlockingReason(pipeline.releaseKeyControl(), blockingReasons);
        collectDeliveryControlBlockingReason(pipeline.registryAuthControl(), blockingReasons);
        collectDeliveryControlBlockingReason(pipeline.githubPublishingControl(), blockingReasons);
        collectDeliveryControlBlockingReason(pipeline.canaryControl(), blockingReasons);
        collectDeliveryAssetBlockingReason(pipeline.workflowAsset(), "workflow asset", blockingReasons);
        collectDeliveryAssetBlockingReason(pipeline.standardSaasComposeAsset(), "standard saas compose asset", blockingReasons);
        collectDeliveryAssetBlockingReason(pipeline.privateComposeAsset(), "private compose asset", blockingReasons);
        collectPipelineProbeBlockingReason(pipeline.githubProbe(), "github repository probe", blockingReasons);
        collectPipelineProbeBlockingReason(pipeline.registryProbe(), "container registry probe", blockingReasons);
    }

    private void collectDeliveryAssetBlockingReason(SaasTenantService.DeliveryAssetSnapshot asset,
                                                    String label,
                                                    List<String> blockingReasons) {
        if (!asset.ready()) {
            blockingReasons.add("delivery pipeline " + label + " is missing or invalid");
        }
    }

    private void collectPipelineProbeBlockingReason(SaasTenantService.ExternalSystemConnectivitySnapshot probe,
                                                    String label,
                                                    List<String> blockingReasons) {
        if (!probe.configured()) {
            blockingReasons.add("delivery pipeline is missing " + label + " endpoint");
            return;
        }
        if (!probe.reachable()) {
            blockingReasons.add("delivery pipeline " + label + " is unreachable");
        }
    }

    private void collectDeliveryControlBlockingReason(SaasTenantService.DeliveryControlDiagnosticSnapshot control,
                                                      List<String> blockingReasons) {
        if (isDeliveryControlReady(control)) {
            return;
        }
        if ("release-key".equals(control.controlCode()) && !control.trusted()) {
            blockingReasons.add("delivery pipeline release key injection must be explicitly configured");
            return;
        }
        if ("release-key".equals(control.controlCode())) {
            blockingReasons.add("delivery pipeline is missing release key injection");
            return;
        }
        if ("registry-auth".equals(control.controlCode()) && !control.trusted()) {
            blockingReasons.add("delivery pipeline container registry publish credentials must be explicitly configured");
            return;
        }
        if ("registry-auth".equals(control.controlCode())) {
            blockingReasons.add("delivery pipeline is missing container registry publish credentials");
            return;
        }
        if ("canary-strategy".equals(control.controlCode()) && "missing-strategy".equals(control.status())) {
            blockingReasons.add("delivery pipeline canary strategy is missing");
            return;
        }
        if ("canary-strategy".equals(control.controlCode())) {
            blockingReasons.add("delivery pipeline canary strategy is disabled");
            return;
        }
        if ("github-publish-mode".equals(control.controlCode()) && "workflow-invalid".equals(control.status())) {
            blockingReasons.add("delivery pipeline github publish workflow asset is missing or invalid");
            return;
        }
        if ("github-publish-mode".equals(control.controlCode()) && "missing-repository".equals(control.status())) {
            return;
        }
        if ("github-publish-mode".equals(control.controlCode()) && !control.trusted()) {
            blockingReasons.add("delivery pipeline github publish mode must be explicitly configured");
            return;
        }
        if ("github-publish-mode".equals(control.controlCode())) {
            blockingReasons.add("delivery pipeline is missing github publish mode");
        }
    }

    private boolean isDeliveryControlReady(SaasTenantService.DeliveryControlDiagnosticSnapshot control) {
        if (!control.configured()) {
            return false;
        }
        if ("github-publish-mode".equals(control.controlCode())
                || "release-key".equals(control.controlCode())
                || "registry-auth".equals(control.controlCode())) {
            return control.trusted();
        }
        return true;
    }

    private void collectExternalIntegrationBlockingReasons(String tenantId,
                                                           SaasTenantService.ExternalIntegrationConnectivitySnapshot snapshot,
                                                           List<String> blockingReasons) {
        blockingReasons.addAll(saasTenantService.getExternalIntegrationBlockingReasons(tenantId));
        for (String systemCode : saasTenantService.listRequiredExternalSystems(tenantId)) {
            SaasTenantService.ExternalSystemConnectivitySnapshot connectivitySnapshot =
                    findExternalIntegrationConnectivitySnapshot(snapshot, systemCode);
            if (connectivitySnapshot != null) {
                collectExternalIntegrationBlockingReason(connectivitySnapshot, blockingReasons);
            }
        }
    }

    private SaasTenantService.ExternalSystemConnectivitySnapshot findExternalIntegrationConnectivitySnapshot(
            SaasTenantService.ExternalIntegrationConnectivitySnapshot snapshot,
            String systemCode) {
        return switch (systemCode) {
            case "erp" -> snapshot.erp();
            case "wms" -> snapshot.wms();
            case "tax" -> snapshot.tax();
            case "messaging" -> snapshot.messaging();
            case "bi" -> snapshot.bi();
            case "routing" -> snapshot.routing();
            default -> null;
        };
    }

    private void collectExternalIntegrationBlockingReason(SaasTenantService.ExternalSystemConnectivitySnapshot snapshot,
                                                          List<String> blockingReasons) {
        String label = isBlank(snapshot.provider()) ? snapshot.systemCode() : snapshot.provider();
        if (!snapshot.configured()) {
            blockingReasons.add(label + " integration is not configured");
            return;
        }
        if (!snapshot.trusted()) {
            blockingReasons.add(label + " integration endpoint must be explicitly configured");
            return;
        }
        if (!snapshot.reachable()) {
            blockingReasons.add(label + " integration probe is unreachable");
        }
    }

    private void collectAcceptanceBlockingReasons(SaasTenantService.DualDeliveryAcceptanceSnapshot acceptance,
                                                  List<String> blockingReasons) {
        collectAcceptanceEndpointBlockingReason(acceptance.standardSaas(), blockingReasons);
        collectAcceptanceEndpointBlockingReason(acceptance.privateDeployment(), blockingReasons);
    }

    private void collectAcceptanceEndpointBlockingReason(SaasTenantService.DeliveryEndpointSnapshot endpoint,
                                                         List<String> blockingReasons) {
        if (!endpoint.configured()) {
            boolean missingBaseUrl = endpoint.maskedBaseUrl() == null || endpoint.maskedBaseUrl().isBlank();
            boolean missingVerificationTimestamp = endpoint.verifiedAt() == null || endpoint.verifiedAt().isBlank();
            if (missingBaseUrl) {
                blockingReasons.add("dual delivery acceptance is missing " + endpoint.mode() + " base url");
            }
            if (missingVerificationTimestamp) {
                blockingReasons.add("dual delivery acceptance is missing " + endpoint.mode() + " verification timestamp");
            }
            if (!missingBaseUrl && !missingVerificationTimestamp) {
                blockingReasons.add("dual delivery acceptance is missing " + endpoint.mode() + " base url or verification evidence");
            }
            return;
        }
        if ("invalid".equals(endpoint.verificationStatus())) {
            blockingReasons.add("dual delivery acceptance " + endpoint.mode() + " verification timestamp is invalid");
            return;
        }
        if (!endpoint.verificationFresh()) {
            blockingReasons.add("dual delivery acceptance " + endpoint.mode() + " verification evidence is stale");
            return;
        }
        if (!endpoint.reachable()) {
            blockingReasons.add("dual delivery acceptance " + endpoint.mode() + " probe is unreachable");
        }
    }

    private ExternalAppProfile authenticateExternalProfile(String accessKey,
                                                           String secret,
                                                           String endpoint,
                                                           String requiredScope) {
        return authenticateExternalProfile(
                accessKey,
                secret,
                endpoint,
                requiredScope == null ? null : List.of(requiredScope),
                null
        );
    }

    private ExternalAppProfile authenticateExternalProfile(String accessKey,
                                                           String secret,
                                                           String endpoint,
                                                           List<String> requiredScopes) {
        return authenticateExternalProfile(accessKey, secret, endpoint, requiredScopes, null);
    }

    private ExternalAppProfile authenticateExternalProfile(String accessKey,
                                                           String secret,
                                                           String endpoint,
                                                           String requiredScope,
                                                           String requiredSystemCode) {
        return authenticateExternalProfile(
                accessKey,
                secret,
                endpoint,
                requiredScope == null ? null : List.of(requiredScope),
                requiredSystemCode
        );
    }

    private ExternalAppProfile authenticateExternalProfile(String accessKey,
                                                           String secret,
                                                           String endpoint,
                                                           List<String> requiredScopes,
                                                           String requiredSystemCode) {
        IntegrationCredential credential = integrationCredentialRepository.findByAccessKey(accessKey)
                .orElseThrow(this::integrationCredentialInvalid);
        PluginApp pluginApp = pluginAppRepository.findByAppId(credential.pluginAppId())
                .orElseThrow(this::integrationCredentialInvalid);
        Organization organization = organizationRepository.findById(pluginApp.organizationId())
                .orElseThrow(this::integrationCredentialInvalid);

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

        if (requiredSystemCode != null
                && !saasTenantService.listRequiredExternalSystems(organization.tenantId()).contains(requiredSystemCode)) {
            String message = "tenant external integration " + requiredSystemCode + " is not enabled";
            recordCallLog(
                    organization.tenantId(),
                    organization.id(),
                    pluginApp.appId(),
                    null,
                    null,
                    endpoint,
                    "external_inbound",
                    "rejected_integration_disabled",
                    false,
                    false,
                    message
            );
            throw new BusinessException("1009", message, HttpStatus.FORBIDDEN);
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
                .orElseThrow(this::objectNotFound);
        if (!tenantId.equals(organization.tenantId())) {
            throw objectNotFound();
        }
        return organization;
    }

    private WebhookSubscription requireOwnedWebhook(String tenantId, String subscriptionId) {
        WebhookSubscription subscription = webhookSubscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(this::objectNotFound);
        requireOwnedOrganization(tenantId, subscription.organizationId());
        return subscription;
    }

    private PluginApp requireOwnedApp(String tenantId, String appId) {
        PluginApp pluginApp = pluginAppRepository.findByAppId(appId)
                .orElseThrow(this::objectNotFound);
        requireOwnedOrganization(tenantId, pluginApp.organizationId());
        return pluginApp;
    }

    private PluginGovernanceEntryView buildPluginGovernanceEntry(PluginApp app,
                                                                 IntegrationCredential credential,
                                                                 List<WebhookSubscription> organizationWebhooks,
                                                                 List<OpenPlatformCallLog> organizationLogs,
                                                                 OffsetDateTime now) {
        List<String> organizationWebhookIds = organizationWebhooks.stream()
                .map(WebhookSubscription::subscriptionId)
                .toList();
        List<OpenPlatformCallLog> relevantLogs = organizationLogs.stream()
                .filter(log -> app.appId().equals(log.appId())
                        || (log.subscriptionId() != null && organizationWebhookIds.contains(log.subscriptionId())))
                .toList();
        OpenPlatformCallLog lastLog = relevantLogs.stream()
                .max(Comparator.comparing(OpenPlatformCallLog::createdAt))
                .orElse(null);
        int enabledWebhookCount = (int) organizationWebhooks.stream()
                .filter(webhook -> "enabled".equals(webhook.status()))
                .count();
        int rejectedCallCount = (int) relevantLogs.stream()
                .filter(log -> log.resultStatus() != null && log.resultStatus().startsWith("rejected"))
                .count();
        String credentialStatus = determineCredentialStatus(credential, now);
        String webhookStatus = determineWebhookStatus(organizationWebhooks, enabledWebhookCount);
        List<String> issues = buildGovernanceIssues(app, credentialStatus, webhookStatus, rejectedCallCount);
        return new PluginGovernanceEntryView(
                app.appId(),
                app.organizationId(),
                app.appName(),
                app.appType(),
                app.status(),
                app.permissionScope(),
                credentialStatus,
                webhookStatus,
                organizationWebhooks.size(),
                enabledWebhookCount,
                relevantLogs.size(),
                rejectedCallCount,
                lastLog == null ? null : lastLog.resultStatus(),
                lastLog == null ? null : lastLog.createdAt(),
                determineGovernanceRiskLevel(issues),
                issues,
                buildGovernanceRecommendedAction(issues)
        );
    }

    private String determineCredentialStatus(IntegrationCredential credential, OffsetDateTime now) {
        if (credential == null) {
            return "missing";
        }
        if (!isCredentialActive(credential, now)) {
            return "revoked";
        }
        if (credential.expiresAt() != null && !credential.expiresAt().isAfter(now.plusDays(7))) {
            return "expiring_soon";
        }
        return "active";
    }

    private String determineWebhookStatus(List<WebhookSubscription> organizationWebhooks, int enabledWebhookCount) {
        if (organizationWebhooks.isEmpty()) {
            return "missing";
        }
        if (enabledWebhookCount == 0) {
            return "disabled";
        }
        if (enabledWebhookCount < organizationWebhooks.size()) {
            return "partial";
        }
        return "ready";
    }

    private List<String> buildGovernanceIssues(PluginApp app,
                                               String credentialStatus,
                                               String webhookStatus,
                                               int rejectedCallCount) {
        List<String> issues = new ArrayList<>();
        if (!"active".equals(app.status())) {
            issues.add("app_disabled");
        }
        if (!"active".equals(credentialStatus)) {
            issues.add("credential_" + credentialStatus);
        }
        if (!"ready".equals(webhookStatus)) {
            issues.add("webhook_" + webhookStatus);
        }
        if (rejectedCallCount > 0) {
            issues.add("traffic_rejected");
        }
        return List.copyOf(issues);
    }

    private String determineGovernanceRiskLevel(List<String> issues) {
        if (issues.contains("credential_revoked") || issues.contains("traffic_rejected")) {
            return "high";
        }
        if (!issues.isEmpty()) {
            return "medium";
        }
        return "low";
    }

    private String buildGovernanceRecommendedAction(List<String> issues) {
        if (issues.contains("credential_revoked")) {
            return "Refresh and re-issue the integration credential immediately.";
        }
        if (issues.contains("traffic_rejected")) {
            return "Review recent rejected traffic and fix scope, signature, or callback policy.";
        }
        if (issues.contains("credential_missing")) {
            return "Issue the first credential before exposing this app externally.";
        }
        if (issues.contains("credential_expiring_soon")) {
            return "Rotate the credential before it reaches the expiration window.";
        }
        if (issues.contains("webhook_missing")) {
            return "Add at least one enabled webhook subscription for callback governance.";
        }
        if (issues.contains("webhook_disabled") || issues.contains("webhook_partial")) {
            return "Enable webhook subscriptions to restore callback governance coverage.";
        }
        if (issues.contains("app_disabled")) {
            return "Confirm whether the app should stay disabled or be re-enabled for traffic.";
        }
        return "Governance baseline is healthy.";
    }

    private int governanceRiskWeight(String governanceRiskLevel) {
        return switch (governanceRiskLevel) {
            case "high" -> 3;
            case "medium" -> 2;
            default -> 1;
        };
    }

    private BusinessException objectNotFound() {
        return new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
    }

    private BusinessException integrationCredentialInvalid() {
        return new BusinessException("1008", "integration credential invalid", HttpStatus.FORBIDDEN);
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
        List<String> unsupportedScopes = normalized.stream()
                .filter(scope -> !SUPPORTED_PERMISSION_SCOPES.contains(scope))
                .toList();
        if (!unsupportedScopes.isEmpty()) {
            throw new BusinessException(
                    "1004",
                    "unsupported permissionScope: " + String.join(", ", unsupportedScopes),
                    HttpStatus.BAD_REQUEST
            );
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

    private String normalizedProperty(String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ExternalSystemConnectionReadiness evaluateExternalSystemConnectionReadiness(String systemCode) {
        String prefix = "app.integrations.external.systems." + systemCode + ".";
        List<String> missingParts = new ArrayList<>();
        if (!isConfigured(prefix + "endpoint")) {
            missingParts.add("endpoint");
        }
        boolean credentialConfigured = environment.getProperty(prefix + "credential-configured", Boolean.class, false);
        if (!credentialConfigured) {
            missingParts.add("credentials");
        }
        boolean callbackRequired = environment.getProperty(prefix + "callback-required", Boolean.class, false);
        boolean callbackUrlConfigured = !callbackRequired || isConfigured(prefix + "callback-url");
        if (callbackRequired && !callbackUrlConfigured) {
            missingParts.add("callback url");
        }
        return new ExternalSystemConnectionReadiness(
                credentialConfigured,
                callbackRequired,
                callbackUrlConfigured,
                missingParts.isEmpty() ? "ready" : "blocked",
                List.copyOf(missingParts)
        );
    }

    private ExternalCallbackWorkerReadiness evaluateCallbackWorkerReadiness(boolean callbackBridgeEnabled) {
        String endpointKey = "app.integrations.external.messaging.callback-worker-endpoint";
        String consumerGroupKey = "app.integrations.external.messaging.callback-worker-consumer-group";
        boolean workerEnabled = environment.getProperty("app.integrations.external.messaging.callback-worker-enabled", Boolean.class, false);
        List<String> missingParts = new ArrayList<>();
        if (callbackBridgeEnabled) {
            if (!workerEnabled) {
                missingParts.add("worker enabled");
            }
            if (!isConfigured(endpointKey)) {
                missingParts.add("worker endpoint");
            }
            if (!isConfigured(consumerGroupKey)) {
                missingParts.add("consumer group");
            }
        }
        return new ExternalCallbackWorkerReadiness(
                workerEnabled,
                normalizedProperty("app.integrations.external.messaging.callback-worker-provider", "spring-event"),
                maskEndpoint(environment.getProperty(endpointKey, "")),
                normalizedProperty(consumerGroupKey, ""),
                !callbackBridgeEnabled || missingParts.isEmpty(),
                List.copyOf(missingParts)
        );
    }

    private ExternalSystemConnectivitySnapshot probeCallbackWorker() {
        return saasTenantService.probeHttpExternalEndpoint(
                "callback-worker",
                normalizedProperty("app.integrations.external.messaging.callback-worker-provider", "spring-event"),
                environment.getProperty("app.integrations.external.messaging.callback-worker-endpoint", "")
        );
    }

    private ExternalSystemConnectivitySnapshot findConnectivitySnapshot(String systemCode) {
        ExternalIntegrationConnectivitySnapshot snapshot = saasTenantService.getExternalIntegrationConnectivitySnapshot();
        return switch (systemCode) {
            case "erp" -> snapshot.erp();
            case "wms" -> snapshot.wms();
            case "tax" -> snapshot.tax();
            case "messaging" -> snapshot.messaging();
            case "bi" -> snapshot.bi();
            case "routing" -> snapshot.routing();
            default -> null;
        };
    }

    private ExternalObservabilityStackReadinessView buildExternalObservabilityStackReadinessView(
            SaasTenantService.ObservabilityStackReadinessSnapshot stack) {
        return new ExternalObservabilityStackReadinessView(
                stack.ready(),
                stack.configuredCount(),
                stack.reachableCount(),
                buildExternalObservabilityEndpointView(stack.logAggregation()),
                buildExternalObservabilityEndpointView(stack.trace()),
                buildExternalObservabilityEndpointView(stack.alertRouter()),
                buildExternalObservabilityEndpointView(stack.dashboard()),
                stack.releaseGateStatus(),
                stack.releaseGateDetail()
        );
    }

    private ExternalObservabilityEndpointView buildExternalObservabilityEndpointView(ExternalSystemConnectivitySnapshot snapshot) {
        return new ExternalObservabilityEndpointView(
                snapshot.configured(),
                snapshot.host(),
                snapshot.maskedTarget(),
                snapshot.sourceType(),
                snapshot.sourceName(),
                snapshot.defaultValue(),
                snapshot.trusted(),
                snapshot.status(),
                snapshot.reachable(),
                snapshot.detail()
        );
    }

    private boolean isConfigured(String key) {
        String value = environment.getProperty(key, "");
        return value != null && !value.isBlank();
    }

    private String extractHost(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String maskEndpoint(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            String host = uri.getHost() == null ? "" : uri.getHost();
            String authority = host;
            if (uri.getPort() >= 0) {
                authority = authority + ":" + uri.getPort();
            }
            if (scheme.isBlank() || authority.isBlank()) {
                return "***";
            }
            return scheme + "://" + authority + "/***";
        } catch (IllegalArgumentException exception) {
            return "***";
        }
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

    public record PluginGovernanceOverviewView(
            int totalApps,
            int activeApps,
            int riskyApps,
            int appsMissingCredentials,
            int appsWithRejectedTraffic,
            int organizationsWithoutWebhooks,
            List<String> recommendedActions,
            List<PluginGovernanceEntryView> entries
    ) {
    }

    public record PluginGovernanceEntryView(
            String appId,
            String organizationId,
            String appName,
            String appType,
            String appStatus,
            List<String> permissionScope,
            String credentialStatus,
            String webhookStatus,
            int organizationWebhookCount,
            int enabledWebhookCount,
            int recentCallCount,
            int rejectedCallCount,
            String lastResultStatus,
            OffsetDateTime lastActivityAt,
            String governanceRiskLevel,
            List<String> issues,
            String recommendedAction
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

    public record ExternalErpBaselineView(
            String provider,
            boolean configured,
            String host,
            String maskedEndpoint,
            boolean partySyncEnabled,
            String orderSyncMode,
            int ledgerMappingCount,
            boolean catalogExportEnabled,
            boolean credentialConfigured,
            String readinessStatus,
            List<String> missingParts,
            boolean probeReachable,
            String probeDetail
    ) {
    }

    public record ExternalWmsBaselineView(
            String provider,
            boolean configured,
            String host,
            String maskedEndpoint,
            int facilityCount,
            String stockSyncMode,
            String outboundFlow,
            boolean batchTrackingEnabled,
            boolean credentialConfigured,
            String readinessStatus,
            List<String> missingParts,
            boolean probeReachable,
            String probeDetail
    ) {
    }

    public record ExternalMessagingBaselineView(
            String provider,
            boolean configured,
            String host,
            String maskedEndpoint,
            String virtualHost,
            String exchange,
            int queueCount,
            boolean callbackBridgeEnabled,
            boolean deadLetterEnabled,
            boolean credentialConfigured,
            boolean callbackRequired,
            boolean callbackUrlConfigured,
            String readinessStatus,
            List<String> missingParts,
            boolean probeReachable,
            String probeDetail,
            boolean callbackWorkerEnabled,
            String callbackWorkerProvider,
            String callbackWorkerMaskedEndpoint,
            String callbackWorkerConsumerGroup,
            boolean callbackWorkerReady,
            List<String> callbackWorkerMissingParts,
            boolean callbackWorkerProbeReachable,
            String callbackWorkerProbeDetail
    ) {
    }

    public record ExternalMessagingCallbackBridgeOverviewView(
            String provider,
            String overviewStatus,
            boolean configured,
            String host,
            String maskedEndpoint,
            String virtualHost,
            String exchange,
            boolean callbackBridgeEnabled,
            boolean deadLetterEnabled,
            boolean credentialConfigured,
            boolean callbackRequired,
            boolean callbackUrlConfigured,
            String readinessStatus,
            List<String> missingParts,
            boolean probeReachable,
            String probeDetail,
            boolean callbackWorkerEnabled,
            String callbackWorkerProvider,
            String callbackWorkerMaskedEndpoint,
            String callbackWorkerConsumerGroup,
            boolean callbackWorkerReady,
            List<String> callbackWorkerMissingParts,
            boolean callbackWorkerProbeReachable,
            String callbackWorkerProbeDetail,
            int totalSubscriptionCount,
            int enabledSubscriptionCount,
            int callbackAttemptCount,
            int acceptedCallbackCount,
            int rejectedCallbackCount,
            int replayRejectedCount,
            int signatureRejectedCount,
            String lastTraceId,
            String lastResultStatus
    ) {
    }

    public record ExternalObservabilityReadinessOverviewView(
            String tenantId,
            boolean stackReady,
            ExternalObservabilityStackReadinessView stack,
            SaasTenantService.AuditTraceabilitySummary auditTraceability,
            List<String> blockingReasons
    ) {
    }

    public record ExternalDeliveryReadinessOverviewView(
            String tenantId,
            String repository,
            boolean pipelineReady,
            boolean externalIntegrationsReady,
            boolean acceptanceReady,
            SaasTenantService.DeliveryPipelineSnapshot pipeline,
            SaasTenantService.ExternalIntegrationConnectivitySnapshot externalIntegrations,
            SaasTenantService.DualDeliveryAcceptanceSnapshot acceptance,
            List<String> blockingReasons
    ) {
    }

    public record ExternalObservabilityStackReadinessView(
            boolean ready,
            int configuredCount,
            int reachableCount,
            ExternalObservabilityEndpointView logAggregation,
            ExternalObservabilityEndpointView trace,
            ExternalObservabilityEndpointView alertRouter,
            ExternalObservabilityEndpointView dashboard,
            String releaseGateStatus,
            String releaseGateDetail
    ) {
    }

    public record ExternalObservabilityEndpointView(
            boolean configured,
            String host,
            String maskedEndpoint,
            String sourceType,
            String sourceName,
            boolean defaultValue,
            boolean trusted,
            String status,
            boolean probeReachable,
            String probeDetail
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

    private record ExternalSystemConnectionReadiness(
            boolean credentialConfigured,
            boolean callbackRequired,
            boolean callbackUrlConfigured,
            String readinessStatus,
            List<String> missingParts
    ) {
    }

    private record ExternalCallbackWorkerReadiness(
            boolean enabled,
            String provider,
            String maskedEndpoint,
            String consumerGroup,
            boolean ready,
            List<String> missingParts
    ) {
    }
}
