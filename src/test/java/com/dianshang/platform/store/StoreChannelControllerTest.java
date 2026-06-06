package com.dianshang.platform.store;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.dto.LoginResponse;
import com.dianshang.platform.common.api.ApiResponse;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class StoreChannelControllerTest {

    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private StoreChannelService storeChannelService;

    @BeforeEach
    void setUp() {
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldConnectStoreUpdateSettingsAndManageChannelAccount() throws Exception {
        JsonNode tenantData = registerTenant("星辰电商");
        String ownerToken = login("13800000000", BOOTSTRAP_PASSWORD);
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();

        MvcResult connectResult = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "shop-001",
                                  "shopName": "星辰旗舰店",
                                  "profitThreshold": 18.50,
                                  "riskThreshold": 72.00,
                                  "defaultShipConfig": {
                                    "warehouse": "华东仓",
                                    "carrier": "中通"
                                  }
                                }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authStatus").value("connected"))
                .andExpect(jsonPath("$.data.platformType").value("douyin"))
                .andExpect(jsonPath("$.data.defaultShipConfig.warehouse").value("华东仓"))
                .andReturn();

        String storeId = objectMapper.readTree(connectResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();

        mockMvc.perform(get("/api/stores")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/stores/{id}", storeId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(storeId))
                .andExpect(jsonPath("$.data.shopName").value("星辰旗舰店"));

        mockMvc.perform(put("/api/stores/{id}/settings", storeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profitThreshold": 22.00,
                                  "riskThreshold": 80.00,
                                  "defaultShipConfig": {
                                    "warehouse": "华南仓",
                                    "carrier": "顺丰"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profitThreshold").value(22.0))
                .andExpect(jsonPath("$.data.defaultShipConfig.carrier").value("顺丰"));

        MvcResult channelCreateResult = mockMvc.perform(post("/api/channel-accounts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "channelType": "douyin-open",
                                  "accountName": "抖音主账号",
                                  "extraConfig": {
                                    "appKey": "app-001"
                                  }
                                }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authStatus").value("connected"))
                .andExpect(jsonPath("$.data.channelType").value("douyin-open"))
                .andReturn();

        String channelAccountId = objectMapper.readTree(channelCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("channelAccountId")
                .asText();

        mockMvc.perform(get("/api/channel-accounts")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].channelAccountId").value(channelAccountId));

        mockMvc.perform(post("/api/channel-accounts/{id}/refresh-auth", channelAccountId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelAccountId").value(channelAccountId))
                .andExpect(jsonPath("$.data.authStatus").value("connected"));
    }

    @Test
    void shouldRejectConnectingStoreWhenStoreQuotaExceeded() throws Exception {
        JsonNode tenantData = registerTenant("流光电商");
        String ownerToken = login("13800000000", BOOTSTRAP_PASSWORD);
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();

        for (int index = 1; index <= 3; index++) {
            mockMvc.perform(post("/api/stores/connect")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "organizationId": "%s",
                                      "ownerUserId": "tenant-admin",
                                      "platformType": "douyin",
                                      "platformShopId": "shop-00%s",
                                      "shopName": "测试店铺%s",
                                      "profitThreshold": 10.00,
                                      "riskThreshold": 50.00
                                    }
                                    """.formatted(defaultOrganizationId, index, index)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "shop-004",
                                  "shopName": "测试店铺4",
                                  "profitThreshold": 10.00,
                                  "riskThreshold": 50.00
                                }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("6002"))
                .andExpect(jsonPath("$.message").value("quota exceeded"));
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "张三",
                                  "mobile": "13800000000"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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
}
