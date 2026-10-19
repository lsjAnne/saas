package backend.live.barrage.controller;

import backend.auth.dto.LoginResponse;
import backend.common.api.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 弹幕源接入闭环测试：
 * 启动平台弹幕连接器（mock 模式）→ 连接器定时产生模拟弹幕 → 数字人问答服务作答并落库 →
 * 状态面板展示运行中（mock）→ 停止后恢复未运行。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.auth.allow-legacy-header-context=false",
        "app.live.barrage.connectors.douyin.mock-mode=true",
        "app.live.barrage.connectors.kuaishou.mock-mode=true",
        "app.live.barrage.connectors.wechat_channels.mock-mode=true"
})
class BarrageConnectionControllerTest {

    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldStartStopPlatformBarrageConnectorAndDriveAnswerLoop() throws Exception {
        JsonNode tenant = registerTenant("barrage-connector-test", "13944444444");
        String token = login("13944444444", BOOTSTRAP_PASSWORD);

        // 1. 初始状态：三平台均未运行
        mockMvc.perform(get("/api/live/barrage-connections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.platformType=='douyin')].running").value(false));

        // 2. 启动抖音弹幕连接器（mock 模式）
        mockMvc.perform(post("/api/live/barrage-connections/douyin/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.platformType=='douyin')].running").value(true))
                .andExpect(jsonPath("$.data[?(@.platformType=='douyin')].mode").value("mock"));

        // 3. 等待连接器产生模拟弹幕并被数字人问答服务作答落库
        Thread.sleep(3500);
        MvcResult barragesResult = mockMvc.perform(get("/api/digital-humans/barrages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode barrages = objectMapper.readTree(
                        barragesResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data");
        assertTrue(barrages.size() > 0, "mock 弹幕连接器应产生弹幕并进入数字人问答闭环");
        assertTrue(barrages.toString().contains("\"platformType\":\"douyin\""),
                "弹幕平台来源应为 douyin");

        // 4. 互动概览显示弹幕已被数字人作答（faq 命中或 fallback 兜底均计入 replied/fallback）
        MvcResult overviewResult = mockMvc.perform(get("/api/digital-humans/interaction-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode overview = objectMapper.readTree(
                        overviewResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data");
        long answered = overview.path("repliedCount").asLong() + overview.path("fallbackCount").asLong();
        assertTrue(answered > 0, "连接器弹幕应生成数字人互动回答记录");

        // 5. 停止连接器 → 恢复未运行
        mockMvc.perform(post("/api/live/barrage-connections/douyin/stop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.platformType=='douyin')].running").value(false));
        assertFalse(mockMvc.perform(get("/api/digital-humans/barrages")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus() != 200);
    }

    @Test
    void shouldReportAllPlatformStatusWithMockMode() throws Exception {
        JsonNode tenant = registerTenant("barrage-status-test", "13955555555");
        String token = login("13955555555", BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/live/barrage-connections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.platformType=='kuaishou')].mode").value("mock"))
                .andExpect(jsonPath("$.data[?(@.platformType=='wechat_channels')].mode").value("mock"))
                .andExpect(jsonPath("$.data[?(@.platformType=='douyin')].mode").value("mock"));
    }

    // ---------- helpers ----------

    private JsonNode registerTenant(String tenantName, String mobile) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-barrage",
                                  "mobile": "%s"
                                }
                                """.formatted(tenantName, mobile)))
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
