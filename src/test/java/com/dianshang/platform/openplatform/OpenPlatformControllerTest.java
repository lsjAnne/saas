package com.dianshang.platform.openplatform;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.auth.application.AuthService;
import com.dianshang.platform.auth.dto.LoginResponse;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.finance.application.FinanceService;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.store.application.StoreChannelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
        "app.auth.allow-legacy-header-context=false",
        "app.integrations.external.erp.provider=ofbiz",
        "app.integrations.external.erp.endpoint=https://ofbiz.example.com/webtools/control",
        "app.integrations.external.erp.party-sync-enabled=true",
        "app.integrations.external.erp.order-sync-mode=near_real_time",
        "app.integrations.external.erp.ledger-mapping-count=8",
        "app.integrations.external.erp.catalog-export-enabled=true",
        "app.integrations.external.wms.provider=openboxes",
        "app.integrations.external.wms.endpoint=https://openboxes.example.com/openboxes/api",
        "app.integrations.external.wms.facility-count=3",
        "app.integrations.external.wms.stock-sync-mode=two_way",
        "app.integrations.external.wms.outbound-flow=wave_and_pick",
        "app.integrations.external.wms.batch-tracking-enabled=true",
        "app.integrations.external.messaging.provider=rabbitmq",
        "app.integrations.external.messaging.endpoint=amqps://rabbitmq.example.com:5671",
        "app.integrations.external.messaging.virtual-host=tenant-hub",
        "app.integrations.external.messaging.exchange=tenant.events",
        "app.integrations.external.messaging.queue-count=4",
        "app.integrations.external.messaging.callback-bridge-enabled=true",
        "app.integrations.external.messaging.dead-letter-enabled=true"
})
class OpenPlatformControllerTest {

    private static final String OWNER_MOBILE = "13800000000";
    private static final String BOOTSTRAP_PASSWORD = "123456";
    private static final String OPERATOR_MOBILE = "13900000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private OpenPlatformApplicationService openPlatformApplicationService;

    @Autowired
    private StoreChannelService storeChannelService;

    @Autowired
    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        authService.clearSensitiveOperationConfirmations();
        financeService.clear();
        storeChannelService.clear();
        openPlatformApplicationService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldCreateListAndToggleOpenPlatformResources() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        MvcResult createAppResult = mockMvc.perform(post("/api/open/apps")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "appName": "erp-connector",
                                  "appType": "erp",
                                  "permissionScope": ["order.read", "inventory.write"]
                                }
                                """.formatted(fixture.organizationId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationId").value(fixture.organizationId()))
                .andExpect(jsonPath("$.data.appName").value("erp-connector"))
                .andExpect(jsonPath("$.data.appType").value("erp"))
                .andExpect(jsonPath("$.data.permissionScope.length()").value(2))
                .andExpect(jsonPath("$.data.accessKey").isString())
                .andExpect(jsonPath("$.data.secretMasked").isString())
                .andExpect(jsonPath("$.data.status").value("active"))
                .andReturn();

        String appId = objectMapper.readTree(createAppResult.getResponse().getContentAsString())
                .path("data")
                .path("appId")
                .asText();

        mockMvc.perform(get("/api/open/apps")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].appId").value(appId))
                .andExpect(jsonPath("$.data[0].permissionScope.length()").value(2));

        MvcResult createWebhookResult = mockMvc.perform(post("/api/open/webhooks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "eventCode": "order.paid",
                                  "callbackUrl": "https://example.com/webhooks/order-paid"
                                }
                                """.formatted(fixture.organizationId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationId").value(fixture.organizationId()))
                .andExpect(jsonPath("$.data.eventCode").value("order.paid"))
                .andExpect(jsonPath("$.data.callbackUrl").value("https://example.com/webhooks/order-paid"))
                .andExpect(jsonPath("$.data.secretToken").isString())
                .andExpect(jsonPath("$.data.status").value("enabled"))
                .andReturn();

        String subscriptionId = objectMapper.readTree(createWebhookResult.getResponse().getContentAsString())
                .path("data")
                .path("subscriptionId")
                .asText();

        mockMvc.perform(get("/api/open/webhooks")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.data[0].status").value("enabled"));

        mockMvc.perform(post("/api/open/webhooks/{id}/disable", subscriptionId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disabled"));

        mockMvc.perform(post("/api/open/webhooks/{id}/enable", subscriptionId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("enabled"));
    }

    @Test
    void shouldRejectOpenPlatformAccessWithoutPermission() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-forbidden-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        inviteMember(fixture.organizationId(), ownerToken, "operator-user", "operator-user", OPERATOR_MOBILE, "operator");
        String operatorToken = login(OPERATOR_MOBILE, BOOTSTRAP_PASSWORD);

        mockMvc.perform(post("/api/open/apps")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "appName": "forbidden-app",
                                  "appType": "plugin",
                                  "permissionScope": ["order.read"]
                                }
                                """.formatted(fixture.organizationId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"));
    }

    @Test
    void shouldVerifyCallbackPreventReplayAndRecordCallLogs() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-callback-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        WebhookFixture webhookFixture = createWebhook(fixture.organizationId(), ownerToken, "order.created");
        String subscriptionId = webhookFixture.subscriptionId();
        String payload = "{\"event\":\"order.created\",\"orderId\":\"order-1001\"}";
        String requestId = "req-open-1001";
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        String nonce = "nonce-open-001";
        String signature = signCallback(webhookFixture.secretToken(), subscriptionId, timestamp, nonce, payload);

        mockMvc.perform(post("/api/open/callbacks/{id}", subscriptionId)
                        .header("X-Trace-Id", "callback-trace-001")
                        .header("X-Open-Request-Id", requestId)
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.replayed").value(false))
                .andExpect(jsonPath("$.data.status").value("accepted"));

        mockMvc.perform(post("/api/open/callbacks/{id}", subscriptionId)
                        .header("X-Trace-Id", "callback-trace-002")
                        .header("X-Open-Request-Id", requestId)
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("1008"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("accepted"))
                .andExpect(jsonPath("$.data[1].signatureVerified").value(true))
                .andExpect(jsonPath("$.data[1].replayed").value(false))
                .andExpect(jsonPath("$.data[1].traceId").value("callback-trace-001"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_replay"))
                .andExpect(jsonPath("$.data[2].signatureVerified").value(true))
                .andExpect(jsonPath("$.data[2].replayed").value(true))
                .andExpect(jsonPath("$.data[2].traceId").value("callback-trace-002"));
    }

    @Test
    void shouldRotateWebhookSecretAndRejectOldSignature() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-rotate-secret-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        WebhookFixture webhookFixture = createWebhook(fixture.organizationId(), ownerToken, "order.updated");
        String subscriptionId = webhookFixture.subscriptionId();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult rotateResult = mockMvc.perform(post("/api/open/webhooks/{id}/secret/rotate", subscriptionId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.data.secretToken").isString())
                .andReturn();

        String rotatedSecret = objectMapper.readTree(rotateResult.getResponse().getContentAsString())
                .path("data")
                .path("secretToken")
                .asText();

        String payload = "{\"event\":\"order.updated\",\"orderId\":\"order-2001\"}";
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        String nonce = "nonce-open-rotate-001";
        String oldSignature = signCallback(webhookFixture.secretToken(), subscriptionId, timestamp, nonce, payload);
        String newSignature = signCallback(rotatedSecret, subscriptionId, timestamp, nonce, payload);

        mockMvc.perform(post("/api/open/callbacks/{id}", subscriptionId)
                        .header("X-Trace-Id", "callback-trace-rotate-old")
                        .header("X-Open-Request-Id", "req-open-rotate-old")
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", oldSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1008"));

        mockMvc.perform(post("/api/open/callbacks/{id}", subscriptionId)
                        .header("X-Trace-Id", "callback-trace-rotate-new")
                        .header("X-Open-Request-Id", "req-open-rotate-new")
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", newSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.replayed").value(false));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("secret_rotated"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_signature"))
                .andExpect(jsonPath("$.data[2].traceId").value("callback-trace-rotate-old"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("accepted"))
                .andExpect(jsonPath("$.data[3].traceId").value("callback-trace-rotate-new"));
    }

    @Test
    void shouldRefreshCredentialAuthorizeExternalProfileAndRecordLogs() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-credential-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createApp(fixture.organizationId(), ownerToken, "erp-auth-app");
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appId").value(appId))
                .andExpect(jsonPath("$.data.credentialType").value("default"))
                .andExpect(jsonPath("$.data.accessKey").isString())
                .andExpect(jsonPath("$.data.secret").isString())
                .andExpect(jsonPath("$.data.secretMasked").isString())
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andReturn();

        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/profile")
                        .header("X-Trace-Id", "open-auth-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appId").value(appId))
                .andExpect(jsonPath("$.data.organizationId").value(fixture.organizationId()))
                .andExpect(jsonPath("$.data.appName").value("erp-auth-app"))
                .andExpect(jsonPath("$.data.appType").value("erp"))
                .andExpect(jsonPath("$.data.permissionScope.length()").value(2))
                .andExpect(jsonPath("$.data.permissionScope[0]").value("order.read"))
                .andExpect(jsonPath("$.data.permissionScope[1]").value("inventory.write"))
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andExpect(jsonPath("$.data.organizationName").isString())
                .andExpect(jsonPath("$.data.tenantId").value(fixture.tenantId()));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[1].appId").value(appId))
                .andExpect(jsonPath("$.data[2].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[2].appId").value(appId))
                .andExpect(jsonPath("$.data[2].traceId").value("open-auth-trace-001"))
                .andExpect(jsonPath("$.data[2].signatureVerified").value(true))
                .andExpect(jsonPath("$.data[2].replayed").value(false));
    }

    @Test
    void shouldExposeWebhookOrchestrationsAndIntegrationAuditViews() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-audit-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createApp(fixture.organizationId(), ownerToken, "erp-audit-app");
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        JsonNode credentialData = objectMapper.readTree(mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/profile")
                        .header("X-Trace-Id", "open-audit-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appId").value(appId));

        WebhookFixture webhookFixture = createWebhook(fixture.organizationId(), ownerToken, "order.shipped");
        String payload = "{\"event\":\"order.shipped\",\"orderId\":\"order-7001\"}";
        String requestId = "req-open-audit-001";
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        String nonce = "nonce-open-audit-001";
        String signature = signCallback(webhookFixture.secretToken(), webhookFixture.subscriptionId(), timestamp, nonce, payload);

        mockMvc.perform(post("/api/open/callbacks/{id}", webhookFixture.subscriptionId())
                        .header("X-Trace-Id", "callback-audit-trace-001")
                        .header("X-Open-Request-Id", requestId)
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("accepted"));

        mockMvc.perform(post("/api/open/callbacks/{id}", webhookFixture.subscriptionId())
                        .header("X-Trace-Id", "callback-audit-trace-002")
                        .header("X-Open-Request-Id", requestId)
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("1008"));

        mockMvc.perform(get("/api/open/webhook-orchestrations")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].subscriptionId").value(webhookFixture.subscriptionId()))
                .andExpect(jsonPath("$.data[0].eventCode").value("order.shipped"))
                .andExpect(jsonPath("$.data[0].callbackAttemptCount").value(2))
                .andExpect(jsonPath("$.data[0].acceptedCallbackCount").value(1))
                .andExpect(jsonPath("$.data[0].rejectedCallbackCount").value(1))
                .andExpect(jsonPath("$.data[0].replayRejectedCount").value(1))
                .andExpect(jsonPath("$.data[0].lastResultStatus").value("rejected_replay"));

        mockMvc.perform(get("/api/open/integration-audit")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCallLogCount").value(6))
                .andExpect(jsonPath("$.data.externalAuthorizedCount").value(1))
                .andExpect(jsonPath("$.data.callbackAcceptedCount").value(1))
                .andExpect(jsonPath("$.data.callbackRejectedCount").value(1))
                .andExpect(jsonPath("$.data.scopeRejectedCount").value(0))
                .andExpect(jsonPath("$.data.replayRejectedCount").value(1))
                .andExpect(jsonPath("$.data.signatureRejectedCount").value(0))
                .andExpect(jsonPath("$.data.disabledAppCount").value(0))
                .andExpect(jsonPath("$.data.revokedCredentialCount").value(0));
    }

    @Test
    void shouldRejectExternalProfileWhenSecretInvalidAndRecordLogs() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-invalid-secret-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createApp(fixture.organizationId(), ownerToken, "erp-invalid-secret-app");
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String accessKey = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("data")
                .path("accessKey")
                .asText();

        mockMvc.perform(get("/api/open/external/profile")
                        .header("X-Trace-Id", "open-auth-trace-002")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", "invalid-secret"))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("1008"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_secret"))
                .andExpect(jsonPath("$.data[2].appId").value(appId))
                .andExpect(jsonPath("$.data[2].traceId").value("open-auth-trace-002"))
                .andExpect(jsonPath("$.data[2].signatureVerified").value(false))
                .andExpect(jsonPath("$.data[2].replayed").value(false));
    }

    @Test
    void shouldRequireSensitiveConfirmationBeforeRefreshingOpenPlatformCredential() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-step-up-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createApp(fixture.organizationId(), ownerToken, "erp-step-up-app");
        String appId = appData.path("appId").asText();

        mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1015"))
                .andExpect(jsonPath("$.message").value("sensitive operation confirmation required"));

        mockMvc.perform(post("/api/auth/sensitive-operation-confirmations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "123456",
                                  "permissionCode": "openplatform.manage"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value("openplatform.manage"))
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.expiresAt").isString());

        mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appId").value(appId))
                .andExpect(jsonPath("$.data.accessKey").isString())
                .andExpect(jsonPath("$.data.secret").isString());
    }

    @Test
    void shouldExposeErpIntegrationBaselineResourcesAndRecordExternalLogs() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-erp-baseline-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        String storeId = connectStore(ownerToken, fixture.organizationId(), "erp-baseline-shop");

        mockMvc.perform(post("/api/erp-master-data-dictionaries")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictionaryType": "voucher_type",
                                  "dictionaryCode": "settlement_voucher",
                                  "dictionaryName": "结算凭证",
                                  "erpCode": "ERP_VOUCHER_SETTLEMENT",
                                  "erpName": "ERP结算凭证",
                                  "enabled": true,
                                  "remark": "开放接口字典"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/erp-account-mappings")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "mappingCategory": "voucher_subject",
                                  "businessType": "voucher_type",
                                  "businessCode": "settlement_voucher",
                                  "subjectCode": "600101",
                                  "subjectName": "主营业务收入",
                                  "direction": "credit",
                                  "enabled": true,
                                  "remark": "开放接口映射"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk());

        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "erp-baseline-app",
                "order.read",
                "inventory.write",
                "finance.read"
        );
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/erp/master-data-dictionaries")
                        .header("X-Trace-Id", "open-erp-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret)
                        .param("dictionaryType", "voucher_type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].dictionaryCode").value("settlement_voucher"))
                .andExpect(jsonPath("$.data[0].erpCode").value("ERP_VOUCHER_SETTLEMENT"));

        mockMvc.perform(get("/api/open/external/erp/account-mappings")
                        .header("X-Trace-Id", "open-erp-trace-002")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret)
                        .param("mappingCategory", "voucher_subject")
                        .param("storeId", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].businessCode").value("settlement_voucher"))
                .andExpect(jsonPath("$.data[0].subjectCode").value("600101"));

        mockMvc.perform(get("/api/open/external/erp/integration-baseline")
                        .header("X-Trace-Id", "open-erp-trace-003")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncMode").value("pull"))
                .andExpect(jsonPath("$.data.availableStoreCount").value(1))
                .andExpect(jsonPath("$.data.masterDataDictionaryCount").value(1))
                .andExpect(jsonPath("$.data.accountMappingCount").value(1))
                .andExpect(jsonPath("$.data.supportedResources.length()").value(5))
                .andExpect(jsonPath("$.data.supportedResources[0]").value("master_data_dictionary"))
                .andExpect(jsonPath("$.data.supportedResources[4]").value("period_closing"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[2].traceId").value("open-erp-trace-001"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[3].traceId").value("open-erp-trace-002"))
                .andExpect(jsonPath("$.data[4].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[4].traceId").value("open-erp-trace-003"));
    }

    @Test
    void shouldRejectErpExternalAccessWhenFinanceScopeMissingAndRecordLogs() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-scope-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createApp(fixture.organizationId(), ownerToken, "erp-scope-app");
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/erp/integration-baseline")
                        .header("X-Trace-Id", "open-erp-scope-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"))
                .andExpect(jsonPath("$.message").value("integration permission scope denied"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_scope"))
                .andExpect(jsonPath("$.data[2].appId").value(appId))
                .andExpect(jsonPath("$.data[2].traceId").value("open-erp-scope-trace-001"));
    }

    @Test
    void shouldAuthorizeSpecificErpResourceScopeWithoutGrantingOtherFinanceResources() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-granular-scope-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        String storeId = connectStore(ownerToken, fixture.organizationId(), "erp-granular-scope-shop");

        mockMvc.perform(post("/api/erp-master-data-dictionaries")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "dictionaryType": "voucher_type",
                                  "dictionaryCode": "settlement_voucher",
                                  "dictionaryName": "结算凭证",
                                  "erpCode": "ERP_VOUCHER_SETTLEMENT",
                                  "erpName": "ERP结算凭证",
                                  "enabled": true
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk());

        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "erp-granular-scope-app",
                "erp.master_data.read"
        );
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/erp/master-data-dictionaries")
                        .header("X-Trace-Id", "open-erp-granular-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret)
                        .param("dictionaryType", "voucher_type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].dictionaryCode").value("settlement_voucher"));

        mockMvc.perform(get("/api/open/external/erp/account-mappings")
                        .header("X-Trace-Id", "open-erp-granular-trace-002")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret)
                        .param("storeId", storeId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"))
                .andExpect(jsonPath("$.message").value("integration permission scope denied"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[2].traceId").value("open-erp-granular-trace-001"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("rejected_scope"))
                .andExpect(jsonPath("$.data[3].traceId").value("open-erp-granular-trace-002"));
    }

    @Test
    void shouldExposeExternalOfbizOpenboxesAndRabbitMqBaselines() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-external-baseline-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "external-baseline-app",
                "erp.ofbiz_baseline.read",
                "wms.openboxes_baseline.read",
                "messaging.rabbitmq_baseline.read"
        );
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/erp/ofbiz-baseline")
                        .header("X-Trace-Id", "open-ofbiz-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("ofbiz"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("ofbiz.example.com"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value("https://ofbiz.example.com/***"))
                .andExpect(jsonPath("$.data.partySyncEnabled").value(true))
                .andExpect(jsonPath("$.data.orderSyncMode").value("near_real_time"))
                .andExpect(jsonPath("$.data.ledgerMappingCount").value(8))
                .andExpect(jsonPath("$.data.catalogExportEnabled").value(true));

        mockMvc.perform(get("/api/open/external/wms/openboxes-baseline")
                        .header("X-Trace-Id", "open-openboxes-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("openboxes"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("openboxes.example.com"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value("https://openboxes.example.com/***"))
                .andExpect(jsonPath("$.data.facilityCount").value(3))
                .andExpect(jsonPath("$.data.stockSyncMode").value("two_way"))
                .andExpect(jsonPath("$.data.outboundFlow").value("wave_and_pick"))
                .andExpect(jsonPath("$.data.batchTrackingEnabled").value(true));

        mockMvc.perform(get("/api/open/external/messaging/rabbitmq-baseline")
                        .header("X-Trace-Id", "open-rabbitmq-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("rabbitmq"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("rabbitmq.example.com"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value("amqps://rabbitmq.example.com:5671/***"))
                .andExpect(jsonPath("$.data.virtualHost").value("tenant-hub"))
                .andExpect(jsonPath("$.data.exchange").value("tenant.events"))
                .andExpect(jsonPath("$.data.queueCount").value(4))
                .andExpect(jsonPath("$.data.callbackBridgeEnabled").value(true))
                .andExpect(jsonPath("$.data.deadLetterEnabled").value(true));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[2].traceId").value("open-ofbiz-trace-001"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[3].traceId").value("open-openboxes-trace-001"))
                .andExpect(jsonPath("$.data[4].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[4].traceId").value("open-rabbitmq-trace-001"));
    }

    @Test
    void shouldRejectRabbitMqBaselineAccessWhenScopeMissing() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-rabbitmq-scope-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "rabbitmq-scope-missing-app",
                "wms.openboxes_baseline.read"
        );
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/messaging/rabbitmq-baseline")
                        .header("X-Trace-Id", "open-rabbitmq-scope-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"))
                .andExpect(jsonPath("$.message").value("integration permission scope denied"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_scope"))
                .andExpect(jsonPath("$.data[2].appId").value(appId))
                .andExpect(jsonPath("$.data[2].traceId").value("open-rabbitmq-scope-trace-001"));
    }

    @Test
    void shouldExposeExternalWmsAndTmsViewsAndRecordExternalLogs() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-external-execution-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        ExternalExecutionFixture executionFixture = prepareExternalExecutionFixture(
                fixture.organizationId(),
                ownerToken,
                "open-external-execution-shop"
        );

        MvcResult warehouseResult = mockMvc.perform(post("/api/wms/warehouses")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "warehouseName": "Hangzhou Central Warehouse",
                                  "zoneCode": "HZ-A",
                                  "locationCode": "A-01-01",
                                  "temperatureZone": "ambient"
                                }
                                """.formatted(executionFixture.storeId())))
                .andExpect(status().isOk())
                .andReturn();
        String warehouseCode = objectMapper.readTree(warehouseResult.getResponse().getContentAsString())
                .path("data")
                .path("warehouseCode")
                .asText();

        mockMvc.perform(post("/api/wms/waves")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "warehouseCode": "%s",
                                  "waveType": "outbound",
                                  "strategyCode": "priority",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "skuId": "sku-1001",
                                      "locationCode": "A-01-01",
                                      "plannedQty": 2
                                    }
                                  ]
                                }
                                """.formatted(executionFixture.storeId(), warehouseCode, executionFixture.productId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waveStatus").value("released"));

        MvcResult carrierResult = mockMvc.perform(post("/api/tms/carriers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "carrierName": "External Express",
                                  "carrierCode": "EXT-EXP",
                                  "channelType": "express",
                                  "serviceScope": "national"
                                }
                                """.formatted(executionFixture.storeId())))
                .andExpect(status().isOk())
                .andReturn();
        String carrierId = objectMapper.readTree(carrierResult.getResponse().getContentAsString())
                .path("data")
                .path("carrierId")
                .asText();

        MvcResult shipmentResult = mockMvc.perform(post("/api/tms/shipments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fulfillmentTaskId": "%s",
                                  "carrierId": "%s",
                                  "shippingMode": "express",
                                  "freightAmount": 22.50,
                                  "originCity": "Hangzhou",
                                  "destinationCity": "Shanghai"
                                }
                                """.formatted(executionFixture.fulfillmentTaskId(), carrierId)))
                .andExpect(status().isOk())
                .andReturn();
        String shipmentId = objectMapper.readTree(shipmentResult.getResponse().getContentAsString())
                .path("data")
                .path("shipmentId")
                .asText();

        mockMvc.perform(post("/api/tms/shipments/{id}/tracking-events", shipmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingStatus": "in_transit",
                                  "locationText": "Hangzhou Sorting Center",
                                  "remark": "route collected"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestTrackingStatus").value("in_transit"));

        mockMvc.perform(post("/api/tms/freight-settlements")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shipmentId": "%s",
                                  "settleMode": "monthly",
                                  "costType": "freight",
                                  "billableWeight": 2.40,
                                  "freightAmount": 22.50
                                }
                                """.formatted(shipmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementStatus").value("pending"));

        mockMvc.perform(post("/api/tms/shipments/{id}/sign-off", shipmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signStatus": "delivered",
                                  "proofType": "electronic_pod",
                                  "remark": "customer signed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.podStatus").value("archived"));

        mockMvc.perform(post("/api/tms/reverse-logistics")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "shipmentId": "%s",
                                  "carrierId": "%s",
                                  "reverseType": "return_pickup",
                                  "remark": "after sale reverse pickup"
                                }
                                """.formatted(executionFixture.orderId(), shipmentId, carrierId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reverseStatus").value("initiated"));

        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "external-execution-app",
                "wms.linkage.read",
                "tms.control_tower.read"
        );
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/wms/linkage")
                        .header("X-Trace-Id", "open-wms-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret)
                        .param("storeId", executionFixture.storeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(executionFixture.storeId()))
                .andExpect(jsonPath("$.data.warehouseCount").value(1))
                .andExpect(jsonPath("$.data.activeWaveCount").value(1))
                .andExpect(jsonPath("$.data.lockedBatchCount").value(0))
                .andExpect(jsonPath("$.data.omsSyncStatus").value("ready"))
                .andExpect(jsonPath("$.data.erpSyncStatus").value("ready"))
                .andExpect(jsonPath("$.data.tmsHandoverStatus").value("pending"));

        mockMvc.perform(get("/api/open/external/tms/control-tower")
                        .header("X-Trace-Id", "open-tms-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(executionFixture.storeId()))
                .andExpect(jsonPath("$.data.carrierCount").value(1))
                .andExpect(jsonPath("$.data.shipmentCount").value(1))
                .andExpect(jsonPath("$.data.signedShipmentCount").value(1))
                .andExpect(jsonPath("$.data.reverseLogisticsCount").value(1))
                .andExpect(jsonPath("$.data.latestTrackingStatus").value("in_transit"))
                .andExpect(jsonPath("$.data.settlementPendingCount").value(1))
                .andExpect(jsonPath("$.data.podArchiveCount").value(1));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[2].traceId").value("open-wms-trace-001"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[3].traceId").value("open-tms-trace-001"));
    }

    @Test
    void shouldRejectTmsExternalAccessWhenControlTowerScopeMissing() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-tms-scope-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "tms-scope-missing-app",
                "wms.linkage.read"
        );
        String appId = appData.path("appId").asText();
        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);

        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/external/tms/control-tower")
                        .header("X-Trace-Id", "open-tms-scope-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"))
                .andExpect(jsonPath("$.message").value("integration permission scope denied"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_scope"))
                .andExpect(jsonPath("$.data[2].appId").value(appId))
                .andExpect(jsonPath("$.data[2].traceId").value("open-tms-scope-trace-001"));
    }

    @Test
    void shouldManagePluginCredentialLifecycleAndExposeOpenPlatformOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-lifecycle-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createApp(fixture.organizationId(), ownerToken, "lifecycle-app");
        String appId = appData.path("appId").asText();
        createWebhook(fixture.organizationId(), ownerToken, "inventory.synced");

        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);
        MvcResult refreshResult = mockMvc.perform(post("/api/open/apps/{id}/credentials/refresh", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode credentialData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String accessKey = credentialData.path("accessKey").asText();
        String secret = credentialData.path("secret").asText();

        mockMvc.perform(get("/api/open/apps/{id}/credentials", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].appId").value(appId))
                .andExpect(jsonPath("$.data[0].credentialType").value("default"))
                .andExpect(jsonPath("$.data[0].accessKey").value(accessKey))
                .andExpect(jsonPath("$.data[0].active").value(true));

        mockMvc.perform(post("/api/open/apps/{id}/disable", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disabled"));

        mockMvc.perform(get("/api/open/external/profile")
                        .header("X-Trace-Id", "open-lifecycle-trace-disabled")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1008"))
                .andExpect(jsonPath("$.message").value("plugin app disabled"));

        mockMvc.perform(post("/api/open/apps/{id}/enable", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));

        mockMvc.perform(get("/api/open/external/profile")
                        .header("X-Trace-Id", "open-lifecycle-trace-enabled")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appId").value(appId));

        confirmSensitivePermission(ownerToken, AuthPermissionCodes.OPENPLATFORM_MANAGE);
        mockMvc.perform(post("/api/open/apps/{id}/credentials/revoke", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appId").value(appId))
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/open/apps/{id}/credentials", appId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].active").value(false));

        mockMvc.perform(get("/api/open/external/profile")
                        .header("X-Trace-Id", "open-lifecycle-trace-revoked")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1008"))
                .andExpect(jsonPath("$.message").value("integration credential expired"));

        mockMvc.perform(get("/api/open/overview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appCount").value(1))
                .andExpect(jsonPath("$.data.activeAppCount").value(1))
                .andExpect(jsonPath("$.data.disabledAppCount").value(0))
                .andExpect(jsonPath("$.data.webhookCount").value(1))
                .andExpect(jsonPath("$.data.enabledWebhookCount").value(1))
                .andExpect(jsonPath("$.data.disabledWebhookCount").value(0))
                .andExpect(jsonPath("$.data.activeCredentialCount").value(0))
                .andExpect(jsonPath("$.data.revokedCredentialCount").value(1))
                .andExpect(jsonPath("$.data.expiringCredentialCount").value(0))
                .andExpect(jsonPath("$.data.totalCallLogCount").value(9));
    }

    private OpenPlatformFixture prepareFixture(String tenantName) throws Exception {
        JsonNode tenantData = registerTenant(tenantName);
        return new OpenPlatformFixture(
                tenantData.path("tenantId").asText(),
                tenantData.path("defaultOrganizationId").asText()
        );
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "open-owner",
                                  "mobile": "13800000000"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private WebhookFixture createWebhook(String organizationId, String token, String eventCode) throws Exception {
        MvcResult createWebhookResult = mockMvc.perform(post("/api/open/webhooks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "eventCode": "%s",
                                  "callbackUrl": "https://example.com/webhooks/%s"
                                }
                                """.formatted(organizationId, eventCode, eventCode.replace('.', '-'))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(createWebhookResult.getResponse().getContentAsString()).path("data");
        return new WebhookFixture(
                data.path("subscriptionId").asText(),
                data.path("secretToken").asText()
        );
    }

    private JsonNode createApp(String organizationId, String token, String appName) throws Exception {
        return createAppWithScopes(organizationId, token, appName, "order.read", "inventory.write");
    }

    private JsonNode createAppWithScopes(String organizationId,
                                         String token,
                                         String appName,
                                         String... permissionScopes) throws Exception {
        String scopeJson = java.util.Arrays.stream(permissionScopes)
                .map(scope -> "\"" + scope + "\"")
                .collect(java.util.stream.Collectors.joining(", "));
        MvcResult createAppResult = mockMvc.perform(post("/api/open/apps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "appName": "%s",
                                  "appType": "erp",
                                  "permissionScope": [%s]
                                }
                                """.formatted(organizationId, appName, scopeJson)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(createAppResult.getResponse().getContentAsString()).path("data");
    }

    private String connectStore(String token, String organizationId, String platformShopId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "%s",
                                  "platformType": "douyin",
                                  "platformShopId": "%s",
                                  "shopName": "erp-baseline-shop",
                                  "profitThreshold": 18.50,
                                  "riskThreshold": 72.00
                                }
                                """.formatted(organizationId, OWNER_MOBILE, platformShopId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("storeId").asText();
    }

    private ExternalExecutionFixture prepareExternalExecutionFixture(String organizationId,
                                                                    String token,
                                                                    String platformShopId) throws Exception {
        String storeId = connectStore(token, organizationId, platformShopId);

        MvcResult candidateResult = mockMvc.perform(post("/api/candidate-products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "sourceType": "1688",
                                  "sourceUrl": "https://source.example.com/p/open-external-3001",
                                  "title": "开放集成测试商品",
                                  "category": "家居",
                                  "estimatedProfit": 21.50,
                                  "riskLevel": "low",
                                  "recommendationReason": "用于开放平台外部系统联调验证",
                                  "aiSummary": "系统生成开放平台联调测试商品"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andReturn();
        String candidateProductId = objectMapper.readTree(candidateResult.getResponse().getContentAsString())
                .path("data")
                .path("candidateProductId")
                .asText();

        MvcResult draftResult = mockMvc.perform(post("/api/product-drafts/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateProductId": "%s",
                                  "aiVersion": "ai-open-external-v1",
                                  "suggestedPrice": 99.90
                                }
                                """.formatted(candidateProductId)))
                .andExpect(status().isOk())
                .andReturn();
        String draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString())
                .path("data")
                .path("productDraftId")
                .asText();

        MvcResult productResult = mockMvc.perform(post("/api/product-drafts/{id}/publish", draftId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformProductId": "open-external-product-3001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();

        MvcResult syncResult = mockMvc.perform(post("/api/orders/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "platformOrderId": "open-external-order-9201",
                                  "productId": "%s",
                                  "skuId": "sku-1001",
                                  "quantity": 2,
                                  "unitPrice": 76.90,
                                  "buyerName": "external-buyer",
                                  "buyerPhoneMask": "137****2001",
                                  "shippingAddress": "Hangzhou Yuhang Tongxie Road 66"
                                }
                                """.formatted(storeId, productId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode syncData = objectMapper.readTree(syncResult.getResponse().getContentAsString()).path("data");
        return new ExternalExecutionFixture(
                storeId,
                productId,
                syncData.path("orderId").asText(),
                syncData.path("fulfillmentTaskId").asText()
        );
    }

    private void confirmSensitivePermission(String token, String permissionCode) throws Exception {
        mockMvc.perform(post("/api/auth/sensitive-operation-confirmations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "permissionCode": "%s"
                                }
                                """.formatted(BOOTSTRAP_PASSWORD, permissionCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(permissionCode))
                .andExpect(jsonPath("$.data.confirmed").value(true));
    }

    private void inviteMember(String organizationId,
                              String token,
                              String userId,
                              String userName,
                              String mobile,
                              String roleCode) throws Exception {
        mockMvc.perform(post("/api/organizations/{id}/members/invite", organizationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "userName": "%s",
                                  "mobile": "%s",
                                  "roleCode": "%s"
                                }
                                """.formatted(userId, userName, mobile, roleCode)))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(
                content,
                new TypeReference<>() {
                }
        );
        return response.data().token();
    }

    private String signCallback(String secretToken,
                                String subscriptionId,
                                String timestamp,
                                String nonce,
                                String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String content = subscriptionId + "\n" + timestamp + "\n" + nonce + "\n" + payload;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }
}

record OpenPlatformFixture(String tenantId, String organizationId) {
}

record WebhookFixture(String subscriptionId, String secretToken) {
}

record ExternalExecutionFixture(String storeId, String productId, String orderId, String fulfillmentTaskId) {
}
