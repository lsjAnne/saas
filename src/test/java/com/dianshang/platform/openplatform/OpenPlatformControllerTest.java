package com.dianshang.platform.openplatform;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        "app.integrations.external.systems.erp.endpoint=https://ofbiz.example.com/webtools/control",
        "app.integrations.external.systems.erp.credential-configured=true",
        "app.integrations.external.erp.party-sync-enabled=true",
        "app.integrations.external.erp.order-sync-mode=near_real_time",
        "app.integrations.external.erp.ledger-mapping-count=8",
        "app.integrations.external.erp.catalog-export-enabled=true",
        "app.integrations.external.wms.provider=openboxes",
        "app.integrations.external.wms.endpoint=https://openboxes.example.com/openboxes/api",
        "app.integrations.external.systems.wms.endpoint=https://openboxes.example.com/openboxes/api",
        "app.integrations.external.systems.wms.credential-configured=true",
        "app.integrations.external.systems.tax.endpoint=https://tax.example.com/api",
        "app.integrations.external.systems.tax.credential-configured=true",
        "app.integrations.external.wms.facility-count=3",
        "app.integrations.external.wms.stock-sync-mode=two_way",
        "app.integrations.external.wms.outbound-flow=wave_and_pick",
        "app.integrations.external.wms.batch-tracking-enabled=true",
        "app.integrations.external.messaging.provider=rabbitmq",
        "app.integrations.external.messaging.endpoint=amqps://rabbitmq.example.com:5671",
        "app.integrations.external.systems.messaging.endpoint=amqps://rabbitmq.example.com:5671",
        "app.integrations.external.systems.messaging.credential-configured=true",
        "app.integrations.external.systems.messaging.callback-required=true",
        "app.integrations.external.systems.messaging.callback-url=https://callback.example.com/messages",
        "app.integrations.external.messaging.virtual-host=tenant-hub",
        "app.integrations.external.messaging.exchange=tenant.events",
        "app.integrations.external.messaging.queue-count=4",
        "app.integrations.external.messaging.callback-bridge-enabled=true",
        "app.integrations.external.messaging.dead-letter-enabled=true",
        "app.integrations.external.messaging.callback-worker-enabled=true",
        "app.integrations.external.messaging.callback-worker-provider=spring-event",
        "app.integrations.external.messaging.callback-worker-endpoint=http://callback-worker.example.internal/consume",
        "app.integrations.external.messaging.callback-worker-consumer-group=open-platform-callbacks",
        "app.integrations.external.bi.provider=superset",
        "app.integrations.external.bi.endpoint=https://superset.example.com/api/v1",
        "app.integrations.external.systems.bi.credential-configured=true",
        "app.integrations.external.bi.dashboard-count=6",
        "app.integrations.external.bi.dataset-count=18",
        "app.integrations.external.bi.embed-enabled=true",
        "app.integrations.external.routing.provider=osrm",
        "app.integrations.external.routing.endpoint=https://router.example.com/osrm",
        "app.integrations.external.systems.routing.endpoint=https://router.example.com/osrm",
        "app.integrations.external.systems.routing.credential-configured=true",
        "app.observability.log-aggregation-endpoint=https://logs.example.com/collect",
        "app.observability.trace-endpoint=https://trace.example.com/api/traces",
        "app.observability.alert-router-endpoint=https://alerts.example.com/api/alerts",
        "app.observability.dashboard-url=https://grafana.example.com/d/tenant",
        "app.delivery.github-owner=lsjAnne",
        "app.delivery.github-repository=saas",
        "app.delivery.registry=ghcr.io",
        "app.delivery.image-repository=lsjAnne/dian-shang-ping-tai",
        "app.delivery.release-key-configured=true",
        "app.delivery.registry-auth-configured=true",
        "app.delivery.canary-enabled=true",
        "app.delivery.canary-strategy=header-weighted",
        "app.delivery.standard-saas-base-url=https://saas.example.com",
        "app.delivery.standard-saas-verified-at=2026-06-10T15:10:00+08:00",
        "app.delivery.private-base-url=https://private.example.com",
        "app.delivery.private-verified-at=2026-06-10T15:25:00+08:00"
})
class OpenPlatformControllerTest {
    private static final String DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE = "default-delivery-github-publish-mode-test";

    private static final HttpServer EXTERNAL_HTTP_SERVER = createExternalHttpServer();
    private static final int HTTP_PORT = EXTERNAL_HTTP_SERVER.getAddress().getPort();
    private static final TcpProbeServer RABBITMQ_TCP_SERVER = createRabbitMqProbeServer();

    @DynamicPropertySource
    static void registerExternalProbeProperties(DynamicPropertyRegistry registry) {
        String httpBase = "http://127.0.0.1:" + HTTP_PORT;
        String messagingEndpoint = "amqp://127.0.0.1:" + RABBITMQ_TCP_SERVER.port() + "/tenant-hub";
        registry.add("app.integrations.external.systems.erp.endpoint", () -> httpBase + "/ofbiz/webtools/control");
        registry.add("app.integrations.external.systems.wms.endpoint", () -> httpBase + "/openboxes/api");
        registry.add("app.integrations.external.systems.messaging.endpoint", () -> messagingEndpoint);
        registry.add("app.integrations.external.systems.bi.endpoint", () -> httpBase + "/superset/api/v1");
        registry.add("app.integrations.external.erp.endpoint", () -> httpBase + "/ofbiz/webtools/control");
        registry.add("app.integrations.external.wms.endpoint", () -> httpBase + "/openboxes/api");
        registry.add("app.integrations.external.messaging.endpoint", () -> messagingEndpoint);
        registry.add("app.integrations.external.bi.endpoint", () -> httpBase + "/superset/api/v1");
        registry.add("app.integrations.external.routing.endpoint", () -> httpBase + "/osrm");
        registry.add("app.integrations.external.messaging.callback-worker-endpoint", () -> httpBase + "/callback-worker/consume");
        registry.add("app.observability.log-aggregation-endpoint", () -> httpBase + "/observability/logs");
        registry.add("app.observability.trace-endpoint", () -> httpBase + "/observability/traces");
        registry.add("app.observability.alert-router-endpoint", () -> httpBase + "/observability/alerts");
        registry.add("app.observability.dashboard-url", () -> httpBase + "/observability/dashboard");
        registry.add("app.delivery.github-probe-endpoint", () -> httpBase + "/delivery/github/lsjAnne/saas");
        registry.add("app.delivery.registry-probe-endpoint", () -> httpBase + "/delivery/registry/lsjAnne/dian-shang-ping-tai");
        registry.add("app.delivery.standard-saas-base-url", () -> httpBase + "/delivery/standard-saas");
        registry.add("app.delivery.private-base-url", () -> httpBase + "/delivery/private");
        registry.add("app.integrations.external.probe-timeout-millis", () -> "1000");
    }

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
    private ConfigurableEnvironment environment;

    @AfterAll
    static void shutdownProbeServers() {
        EXTERNAL_HTTP_SERVER.stop(0);
        RABBITMQ_TCP_SERVER.close();
    }

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
        removePropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE);
        applyPropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE, Map.of(
                "app.delivery.github-publish-mode", "github-actions"
        ));
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
                .andExpect(jsonPath("$.data.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.partySyncEnabled").value(true))
                .andExpect(jsonPath("$.data.orderSyncMode").value("near_real_time"))
                .andExpect(jsonPath("$.data.ledgerMappingCount").value(8))
                .andExpect(jsonPath("$.data.catalogExportEnabled").value(true))
                .andExpect(jsonPath("$.data.credentialConfigured").value(true))
                .andExpect(jsonPath("$.data.readinessStatus").value("ready"))
                .andExpect(jsonPath("$.data.missingParts.length()").value(0))
                .andExpect(jsonPath("$.data.probeReachable").value(true))
                .andExpect(jsonPath("$.data.probeDetail").value("http 200"));

        mockMvc.perform(get("/api/open/external/wms/openboxes-baseline")
                        .header("X-Trace-Id", "open-openboxes-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("openboxes"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.facilityCount").value(3))
                .andExpect(jsonPath("$.data.stockSyncMode").value("two_way"))
                .andExpect(jsonPath("$.data.outboundFlow").value("wave_and_pick"))
                .andExpect(jsonPath("$.data.batchTrackingEnabled").value(true))
                .andExpect(jsonPath("$.data.credentialConfigured").value(true))
                .andExpect(jsonPath("$.data.readinessStatus").value("ready"))
                .andExpect(jsonPath("$.data.missingParts.length()").value(0))
                .andExpect(jsonPath("$.data.probeReachable").value(true))
                .andExpect(jsonPath("$.data.probeDetail").value("http 200"));

        mockMvc.perform(get("/api/open/external/messaging/rabbitmq-baseline")
                        .header("X-Trace-Id", "open-rabbitmq-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("rabbitmq"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.virtualHost").value("tenant-hub"))
                .andExpect(jsonPath("$.data.exchange").value("tenant.events"))
                .andExpect(jsonPath("$.data.queueCount").value(4))
                .andExpect(jsonPath("$.data.callbackBridgeEnabled").value(true))
                .andExpect(jsonPath("$.data.deadLetterEnabled").value(true))
                .andExpect(jsonPath("$.data.credentialConfigured").value(true))
                .andExpect(jsonPath("$.data.callbackRequired").value(true))
                .andExpect(jsonPath("$.data.callbackUrlConfigured").value(true))
                .andExpect(jsonPath("$.data.readinessStatus").value("ready"))
                .andExpect(jsonPath("$.data.missingParts.length()").value(0))
                .andExpect(jsonPath("$.data.probeReachable").value(true))
                .andExpect(jsonPath("$.data.probeDetail").value("tcp connected"))
                .andExpect(jsonPath("$.data.callbackWorkerEnabled").value(true))
                .andExpect(jsonPath("$.data.callbackWorkerProvider").value("spring-event"))
                .andExpect(jsonPath("$.data.callbackWorkerMaskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.callbackWorkerConsumerGroup").value("open-platform-callbacks"))
                .andExpect(jsonPath("$.data.callbackWorkerReady").value(true))
                .andExpect(jsonPath("$.data.callbackWorkerMissingParts.length()").value(0))
                .andExpect(jsonPath("$.data.callbackWorkerProbeReachable").value(true))
                .andExpect(jsonPath("$.data.callbackWorkerProbeDetail").value("http 200"));

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
    void shouldExposeExternalBiAndMessagingCallbackBridgeOverviews() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-external-overview-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        WebhookFixture webhookFixture = createWebhook(fixture.organizationId(), ownerToken, "order.created");
        String payload = "{\"event\":\"order.created\",\"orderId\":\"order-bridge-1001\"}";
        String requestId = "req-open-bridge-1001";
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        String nonce = "nonce-open-bridge-001";
        String signature = signCallback(webhookFixture.secretToken(), webhookFixture.subscriptionId(), timestamp, nonce, payload);

        mockMvc.perform(post("/api/open/callbacks/{id}", webhookFixture.subscriptionId())
                        .header("X-Trace-Id", "callback-bridge-trace-001")
                        .header("X-Open-Request-Id", requestId)
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/open/callbacks/{id}", webhookFixture.subscriptionId())
                        .header("X-Trace-Id", "callback-bridge-trace-002")
                        .header("X-Open-Request-Id", requestId)
                        .header("X-Open-Timestamp", timestamp)
                        .header("X-Open-Nonce", nonce)
                        .header("X-Open-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("1008"));

        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "external-overview-app",
                "bi.superset_overview.read",
                "messaging.callback_bridge.read"
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

        mockMvc.perform(get("/api/open/external/bi/superset-overview")
                        .header("X-Trace-Id", "open-bi-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("superset"))
                .andExpect(jsonPath("$.data.overviewStatus").value("ready"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.dashboardCount").value(6))
                .andExpect(jsonPath("$.data.datasetCount").value(18))
                .andExpect(jsonPath("$.data.embedEnabled").value(true))
                .andExpect(jsonPath("$.data.credentialConfigured").value(true))
                .andExpect(jsonPath("$.data.readinessStatus").value("ready"))
                .andExpect(jsonPath("$.data.missingParts.length()").value(0))
                .andExpect(jsonPath("$.data.probeReachable").value(true))
                .andExpect(jsonPath("$.data.probeDetail").value("http 200"));

        mockMvc.perform(get("/api/open/external/messaging/callback-bridge")
                        .header("X-Trace-Id", "open-callback-bridge-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("rabbitmq"))
                .andExpect(jsonPath("$.data.overviewStatus").value("ready"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.virtualHost").value("tenant-hub"))
                .andExpect(jsonPath("$.data.exchange").value("tenant.events"))
                .andExpect(jsonPath("$.data.callbackBridgeEnabled").value(true))
                .andExpect(jsonPath("$.data.deadLetterEnabled").value(true))
                .andExpect(jsonPath("$.data.credentialConfigured").value(true))
                .andExpect(jsonPath("$.data.callbackRequired").value(true))
                .andExpect(jsonPath("$.data.callbackUrlConfigured").value(true))
                .andExpect(jsonPath("$.data.readinessStatus").value("ready"))
                .andExpect(jsonPath("$.data.missingParts.length()").value(0))
                .andExpect(jsonPath("$.data.probeReachable").value(true))
                .andExpect(jsonPath("$.data.probeDetail").value("tcp connected"))
                .andExpect(jsonPath("$.data.callbackWorkerEnabled").value(true))
                .andExpect(jsonPath("$.data.callbackWorkerProvider").value("spring-event"))
                .andExpect(jsonPath("$.data.callbackWorkerMaskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.callbackWorkerConsumerGroup").value("open-platform-callbacks"))
                .andExpect(jsonPath("$.data.callbackWorkerReady").value(true))
                .andExpect(jsonPath("$.data.callbackWorkerMissingParts.length()").value(0))
                .andExpect(jsonPath("$.data.callbackWorkerProbeReachable").value(true))
                .andExpect(jsonPath("$.data.callbackWorkerProbeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.totalSubscriptionCount").value(1))
                .andExpect(jsonPath("$.data.enabledSubscriptionCount").value(1))
                .andExpect(jsonPath("$.data.callbackAttemptCount").value(2))
                .andExpect(jsonPath("$.data.acceptedCallbackCount").value(1))
                .andExpect(jsonPath("$.data.rejectedCallbackCount").value(1))
                .andExpect(jsonPath("$.data.replayRejectedCount").value(1))
                .andExpect(jsonPath("$.data.signatureRejectedCount").value(0))
                .andExpect(jsonPath("$.data.lastTraceId").value("callback-bridge-trace-002"))
                .andExpect(jsonPath("$.data.lastResultStatus").value("rejected_replay"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("accepted"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_replay"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[4].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[5].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[5].traceId").value("open-bi-trace-001"))
                .andExpect(jsonPath("$.data[6].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[6].traceId").value("open-callback-bridge-trace-001"));
    }

    @Test
    void shouldRejectExternalCallbackBridgeAccessWhenScopeMissing() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-callback-bridge-scope-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "callback-bridge-scope-missing-app",
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

        mockMvc.perform(get("/api/open/external/messaging/callback-bridge")
                        .header("X-Trace-Id", "open-callback-bridge-scope-trace-001")
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
                .andExpect(jsonPath("$.data[2].traceId").value("open-callback-bridge-scope-trace-001"));
    }

    @Test
    void shouldExposeExternalObservabilityAndDeliveryReadinessOverviews() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-readiness-overview-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "readiness-overview-app",
                "system.observability_readiness.read",
                "delivery.readiness.read"
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

        mockMvc.perform(get("/api/open/external/system/observability-readiness")
                        .header("X-Trace-Id", "open-observability-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(fixture.tenantId()))
                .andExpect(jsonPath("$.data.stackReady").value(true))
                .andExpect(jsonPath("$.data.stack.configuredCount").value(4))
                .andExpect(jsonPath("$.data.stack.reachableCount").value(4))
                .andExpect(jsonPath("$.data.stack.logAggregation.sourceType").value("override"))
                .andExpect(jsonPath("$.data.stack.logAggregation.defaultValue").value(false))
                .andExpect(jsonPath("$.data.stack.logAggregation.trusted").value(true))
                .andExpect(jsonPath("$.data.stack.logAggregation.status").value("configured"))
                .andExpect(jsonPath("$.data.stack.logAggregation.probeReachable").value(true))
                .andExpect(jsonPath("$.data.stack.logAggregation.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.stack.trace.probeReachable").value(true))
                .andExpect(jsonPath("$.data.stack.alertRouter.probeReachable").value(true))
                .andExpect(jsonPath("$.data.stack.dashboard.probeReachable").value(true))
                .andExpect(jsonPath("$.data.blockingReasons.length()").value(0));

        mockMvc.perform(get("/api/open/external/delivery/readiness")
                        .header("X-Trace-Id", "open-delivery-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(fixture.tenantId()))
                .andExpect(jsonPath("$.data.repository").value("lsjAnne/saas"))
                .andExpect(jsonPath("$.data.pipelineReady").value(true))
                .andExpect(jsonPath("$.data.externalIntegrationsReady").value(true))
                .andExpect(jsonPath("$.data.acceptanceReady").value(true))
                .andExpect(jsonPath("$.data.pipeline.workflowAsset.ready").value(true))
                .andExpect(jsonPath("$.data.pipeline.standardSaasComposeAsset.ready").value(true))
                .andExpect(jsonPath("$.data.pipeline.privateComposeAsset.ready").value(true))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.controlCode").value("release-key"))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.value").value("true"))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.controlCode").value("registry-auth"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.value").value("true"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.controlCode").value("github-publish-mode"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.value").value("github-actions"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.controlCode").value("canary-strategy"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.value").value("header-weighted"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.systemCode").value("github-repository"))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.reachable").value(true))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.systemCode").value("container-registry"))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.reachable").value(true))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.verificationFresh").value(true))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.reachable").value(true))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.verificationFresh").value(true))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.reachable").value(true))
                .andExpect(jsonPath("$.data.blockingReasons.length()").value(0));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[2].traceId").value("open-observability-trace-001"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("authorized"))
                .andExpect(jsonPath("$.data[3].traceId").value("open-delivery-trace-001"));
    }

    @Test
    void shouldExposeStaleAcceptanceEvidenceInExternalDeliveryReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-stale-delivery-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "stale-delivery-app",
                "delivery.readiness.read"
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

        String sourceName = "open-delivery-stale-evidence-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.acceptance-evidence-max-age-days", "7",
                "app.delivery.standard-saas-verified-at", OffsetDateTime.now().minusDays(30).toString(),
                "app.delivery.private-verified-at", OffsetDateTime.now().minusDays(30).toString()
        ));
        try {
            mockMvc.perform(get("/api/open/external/delivery/readiness")
                            .header("X-Trace-Id", "open-delivery-stale-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.acceptanceReady").value(false))
                    .andExpect(jsonPath("$.data.acceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.acceptance.privateDeployment.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("dual delivery acceptance standard-saas verification evidence is stale")))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("dual delivery acceptance private-deployment verification evidence is stale")));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExposeMissingDeliveryAssetsInExternalDeliveryReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-missing-delivery-assets-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "missing-delivery-assets-app",
                "delivery.readiness.read"
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

        String sourceName = "open-delivery-missing-assets-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.workflow-path", ".tools/missing/backend-delivery.yml",
                "app.delivery.standard-saas-compose-path", ".tools/missing/docker-compose.saas.yml",
                "app.delivery.private-compose-path", ".tools/missing/docker-compose.private.yml"
        ));
        try {
            mockMvc.perform(get("/api/open/external/delivery/readiness")
                            .header("X-Trace-Id", "open-delivery-missing-assets-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipelineReady").value(false))
                    .andExpect(jsonPath("$.data.pipeline.workflowAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.standardSaasComposeAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.privateComposeAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline workflow asset is missing or invalid")))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline standard saas compose asset is missing or invalid")))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline private compose asset is missing or invalid")));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExposeMissingCanaryStrategyInExternalDeliveryReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-missing-canary-strategy-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "missing-canary-strategy-app",
                "delivery.readiness.read"
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

        String sourceName = "open-delivery-missing-canary-strategy-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.canary-strategy", ""
        ));
        try {
            mockMvc.perform(get("/api/open/external/delivery/readiness")
                            .header("X-Trace-Id", "open-delivery-missing-canary-strategy-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipelineReady").value(false))
                    .andExpect(jsonPath("$.data.pipeline.canaryControl.configured").value(false))
                    .andExpect(jsonPath("$.data.pipeline.canaryControl.sourceType").value("override"))
                    .andExpect(jsonPath("$.data.pipeline.canaryControl.status").value("missing-strategy"))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline canary strategy is missing")));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExposeDefaultGithubPublishModeAsBlockingReasonInExternalDeliveryReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-default-github-publish-mode-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "default-github-publish-mode-app",
                "delivery.readiness.read"
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

        removePropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE);
        try {
            mockMvc.perform(get("/api/open/external/delivery/readiness")
                            .header("X-Trace-Id", "open-delivery-default-github-publish-mode-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipelineReady").value(false))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.configured").value(true))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.sourceType").value("default"))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.defaultValue").value(true))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.trusted").value(false))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.status").value("configured"))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline github publish mode must be explicitly configured")));
        } finally {
            applyPropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE, Map.of(
                    "app.delivery.github-publish-mode", "github-actions"
            ));
        }
    }

    @Test
    void shouldExposeUntrustedReleaseKeyAsBlockingReasonInExternalDeliveryReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-untrusted-release-key-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "untrusted-release-key-app",
                "delivery.readiness.read"
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

        String sourceName = "open-delivery-untrusted-release-key-test";
        InlinedTestPropertiesState originalInlineProperties = replaceInlinedTestProperties(Map.of(
                "app.delivery.release-key-configured", "${test.delivery.release-key:true}"
        ));
        applyPropertyOverrides(sourceName, Map.of(
                "test.delivery.release-key", "true"
        ));
        try {
            mockMvc.perform(get("/api/open/external/delivery/readiness")
                            .header("X-Trace-Id", "open-delivery-untrusted-release-key-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipelineReady").value(false))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.configured").value(true))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.sourceType").value("resolved"))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.defaultValue").value(false))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.trusted").value(false))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline release key injection must be explicitly configured")));
        } finally {
            removePropertyOverrides(sourceName);
            restoreInlinedTestProperties(originalInlineProperties);
        }
    }

    @Test
    void shouldExposeUntrustedRegistryAuthAsBlockingReasonInExternalDeliveryReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-untrusted-registry-auth-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "untrusted-registry-auth-app",
                "delivery.readiness.read"
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

        String sourceName = "open-delivery-untrusted-registry-auth-test";
        InlinedTestPropertiesState originalInlineProperties = replaceInlinedTestProperties(Map.of(
                "app.delivery.registry-auth-configured", "${test.delivery.registry-auth:true}"
        ));
        applyPropertyOverrides(sourceName, Map.of(
                "test.delivery.registry-auth", "true"
        ));
        try {
            mockMvc.perform(get("/api/open/external/delivery/readiness")
                            .header("X-Trace-Id", "open-delivery-untrusted-registry-auth-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipelineReady").value(false))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.configured").value(true))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.sourceType").value("resolved"))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.defaultValue").value(false))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.trusted").value(false))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline container registry publish credentials must be explicitly configured")));
        } finally {
            removePropertyOverrides(sourceName);
            restoreInlinedTestProperties(originalInlineProperties);
        }
    }

    @Test
    void shouldExposeUntrustedObservabilityEndpointAsBlockingReasonInExternalObservabilityReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-untrusted-observability-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "untrusted-observability-app",
                "system.observability_readiness.read"
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

        InlinedTestPropertiesState originalDynamicProperties = replaceNamedPropertySource("Dynamic Test Properties", Map.of(
                "app.observability.log-aggregation-endpoint", "${test.observability.log-endpoint}",
                "test.observability.log-endpoint", "http://127.0.0.1:" + HTTP_PORT + "/observability/logs"
        ));
        try {
            mockMvc.perform(get("/api/open/external/system/observability-readiness")
                            .header("X-Trace-Id", "open-observability-untrusted-endpoint-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stackReady").value(false))
                    .andExpect(jsonPath("$.data.stack.logAggregation.configured").value(true))
                    .andExpect(jsonPath("$.data.stack.logAggregation.sourceType").value("resolved"))
                    .andExpect(jsonPath("$.data.stack.logAggregation.defaultValue").value(false))
                    .andExpect(jsonPath("$.data.stack.logAggregation.trusted").value(false))
                    .andExpect(jsonPath("$.data.stack.logAggregation.status").value("configured-untrusted"))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("observability stack log aggregation endpoint must be explicitly configured")));
        } finally {
            restoreNamedPropertySource("Dynamic Test Properties", originalDynamicProperties);
        }
    }

    @Test
    void shouldExposeUntrustedExternalCallbackUrlAsBlockingReasonInExternalDeliveryReadinessOverview() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-untrusted-external-callback-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "untrusted-external-callback-app",
                "delivery.readiness.read"
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

        String sourceName = "open-delivery-untrusted-external-callback-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.integrations.external.systems.messaging.callback-url", "${test.external.messaging.callback-url:https://callback.example.com/messages}",
                "test.external.messaging.callback-url", "https://callback-runtime.example.com/messages"
        ));
        try {
            mockMvc.perform(get("/api/open/external/delivery/readiness")
                            .header("X-Trace-Id", "open-delivery-untrusted-external-callback-trace-001")
                            .header("X-Open-App-Key", accessKey)
                            .header("X-Open-App-Secret", secret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.externalIntegrationsReady").value(false))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("messaging(rabbitmq) integration callback url must be explicitly configured")));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldRejectExternalReadinessOverviewsWhenScopeMissing() throws Exception {
        OpenPlatformFixture fixture = prepareFixture("open-platform-readiness-scope-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        JsonNode appData = createAppWithScopes(
                fixture.organizationId(),
                ownerToken,
                "readiness-scope-missing-app",
                "bi.superset_overview.read"
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

        mockMvc.perform(get("/api/open/external/system/observability-readiness")
                        .header("X-Trace-Id", "open-observability-scope-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"))
                .andExpect(jsonPath("$.message").value("integration permission scope denied"));

        mockMvc.perform(get("/api/open/external/delivery/readiness")
                        .header("X-Trace-Id", "open-delivery-scope-trace-001")
                        .header("X-Open-App-Key", accessKey)
                        .header("X-Open-App-Secret", secret))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"))
                .andExpect(jsonPath("$.message").value("integration permission scope denied"));

        mockMvc.perform(get("/api/open/logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].resultStatus").value("created"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("credential_refreshed"))
                .andExpect(jsonPath("$.data[2].resultStatus").value("rejected_scope"))
                .andExpect(jsonPath("$.data[2].traceId").value("open-observability-scope-trace-001"))
                .andExpect(jsonPath("$.data[3].resultStatus").value("rejected_scope"))
                .andExpect(jsonPath("$.data[3].traceId").value("open-delivery-scope-trace-001"));
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

    private static HttpServer createExternalHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/ofbiz/webtools/control", OpenPlatformControllerTest::writeOk);
            server.createContext("/openboxes/api", OpenPlatformControllerTest::writeOk);
            server.createContext("/superset/api/v1", OpenPlatformControllerTest::writeOk);
            server.createContext("/osrm", OpenPlatformControllerTest::writeOk);
            server.createContext("/callback-worker/consume", OpenPlatformControllerTest::writeOk);
            server.createContext("/observability/logs", OpenPlatformControllerTest::writeOk);
            server.createContext("/observability/traces", OpenPlatformControllerTest::writeOk);
            server.createContext("/observability/alerts", OpenPlatformControllerTest::writeOk);
            server.createContext("/observability/dashboard", OpenPlatformControllerTest::writeOk);
            server.createContext("/delivery/github/lsjAnne/saas", OpenPlatformControllerTest::writeOk);
            server.createContext("/delivery/registry/lsjAnne/dian-shang-ping-tai", OpenPlatformControllerTest::writeOk);
            server.createContext("/delivery/standard-saas", OpenPlatformControllerTest::writeOk);
            server.createContext("/delivery/private", OpenPlatformControllerTest::writeOk);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start external http probe server", exception);
        }
    }

    private static void writeOk(HttpExchange exchange) throws IOException {
        byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static TcpProbeServer createRabbitMqProbeServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                while (!serverSocket.isClosed()) {
                    try (Socket socket = serverSocket.accept()) {
                        socket.getOutputStream().write(0);
                        socket.getOutputStream().flush();
                    } catch (IOException exception) {
                        if (!serverSocket.isClosed()) {
                            throw new IllegalStateException("rabbitmq probe accept failed", exception);
                        }
                    }
                }
            });
            return new TcpProbeServer(serverSocket, executor);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start rabbitmq probe server", exception);
        }
    }

    private record TcpProbeServer(ServerSocket serverSocket, ExecutorService executor) {
        private int port() {
            return serverSocket.getLocalPort();
        }

        private void close() {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            executor.shutdownNow();
        }
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

    private void applyPropertyOverrides(String sourceName, Map<String, Object> properties) {
        environment.getPropertySources().addFirst(new MapPropertySource(sourceName, new LinkedHashMap<>(properties)));
    }

    private void removePropertyOverrides(String sourceName) {
        environment.getPropertySources().remove(sourceName);
    }

    private InlinedTestPropertiesState replaceNamedPropertySource(String sourceName, Map<String, Object> overrides) {
        InlinedTestPropertiesState original = detachPropertySource(sourceName);
        Map<String, Object> properties = copyProperties(original.propertySource());
        properties.putAll(overrides);
        attachPropertySource(new InlinedTestPropertiesState(
                new MapPropertySource(sourceName, properties),
                original.previousName(),
                original.nextName()
        ));
        return original;
    }

    private void restoreNamedPropertySource(String sourceName, InlinedTestPropertiesState original) {
        environment.getPropertySources().remove(sourceName);
        attachPropertySource(original);
    }

    private InlinedTestPropertiesState replaceInlinedTestProperties(Map<String, Object> overrides) {
        return replaceNamedPropertySource("Inlined Test Properties", overrides);
    }

    private void restoreInlinedTestProperties(InlinedTestPropertiesState original) {
        restoreNamedPropertySource("Inlined Test Properties", original);
    }

    private Map<String, Object> copyProperties(PropertySource<?> propertySource) {
        Map<String, Object> copied = new LinkedHashMap<>();
        if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
            for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                copied.put(propertyName, enumerablePropertySource.getProperty(propertyName));
            }
        }
        return copied;
    }

    private InlinedTestPropertiesState detachInlinedTestProperties() {
        return detachPropertySource("Inlined Test Properties");
    }

    private InlinedTestPropertiesState detachPropertySource(String sourceName) {
        MutablePropertySources propertySources = environment.getPropertySources();
        PropertySource<?> original = null;
        String previousName = null;
        String nextName = null;
        boolean found = false;
        for (PropertySource<?> propertySource : propertySources) {
            if (sourceName.equals(propertySource.getName())) {
                original = propertySource;
                found = true;
                continue;
            }
            if (!found) {
                previousName = propertySource.getName();
            } else {
                nextName = propertySource.getName();
                break;
            }
        }
        if (original != null) {
            propertySources.remove(sourceName);
        }
        return new InlinedTestPropertiesState(original, previousName, nextName);
    }

    private void attachPropertySource(InlinedTestPropertiesState state) {
        if (state == null || state.propertySource() == null) {
            return;
        }
        MutablePropertySources propertySources = environment.getPropertySources();
        if (state.nextName() != null && propertySources.contains(state.nextName())) {
            propertySources.addBefore(state.nextName(), state.propertySource());
            return;
        }
        if (state.previousName() != null && propertySources.contains(state.previousName())) {
            propertySources.addAfter(state.previousName(), state.propertySource());
            return;
        }
        propertySources.addLast(state.propertySource());
    }
}

record InlinedTestPropertiesState(PropertySource<?> propertySource, String previousName, String nextName) {
}

record OpenPlatformFixture(String tenantId, String organizationId) {
}

record WebhookFixture(String subscriptionId, String secretToken) {
}

record ExternalExecutionFixture(String storeId, String productId, String orderId, String fulfillmentTaskId) {
}
