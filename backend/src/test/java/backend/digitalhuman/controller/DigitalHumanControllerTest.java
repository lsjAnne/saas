package backend.digitalhuman.controller;

import backend.auth.dto.LoginResponse;
import backend.common.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
class DigitalHumanControllerTest {

    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRunDigitalHumanLiveClosedLoopWithMock() throws Exception {
        JsonNode tenant = registerTenant("digital-human-test");
        String organizationId = tenant.path("defaultOrganizationId").asText();
        String token = login("13900000000", BOOTSTRAP_PASSWORD);

        // 1. 创建数字人形象
        MvcResult avatarResult = mockMvc.perform(post("/api/digital-humans/avatars")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "avatarName": "带货主播小美",
                                  "avatarType": "2D",
                                  "voiceName": "甜美女声",
                                  "previewUrl": "mock://avatar/mei.png",
                                  "enabled": true
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarId").isNotEmpty())
                .andExpect(jsonPath("$.data.avatarName").value("带货主播小美"))
                .andReturn();
        String avatarId = objectMapper.readTree(avatarResult.getResponse().getContentAsString())
                .path("data").path("avatarId").asText();

        // 2. 创建话术剧本
        MvcResult scriptResult = mockMvc.perform(post("/api/digital-humans/scripts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "scriptName": "夜间清仓话术",
                                  "sceneType": "product-intro",
                                  "contentText": "欢迎来到直播间，今天这款商品限时清仓……",
                                  "version": "v1",
                                  "status": "published"
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scriptId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("published"))
                .andReturn();
        String scriptId = objectMapper.readTree(scriptResult.getResponse().getContentAsString())
                .path("data").path("scriptId").asText();

        // 3. 创建无人排班（每日 23:00-02:00）
        MvcResult scheduleResult = mockMvc.perform(post("/api/digital-humans/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "scheduleName": "每日无人值守夜场",
                                  "avatarId": "%s",
                                  "scriptId": "%s",
                                  "platformType": "douyin",
                                  "repeatType": "daily",
                                  "startAt": "2026-10-18T23:00:00+08:00",
                                  "endAt": "2026-10-19T02:00:00+08:00",
                                  "enabled": true
                                }
                                """.formatted(organizationId, avatarId, scriptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("scheduled"))
                .andReturn();
        String scheduleId = objectMapper.readTree(scheduleResult.getResponse().getContentAsString())
                .path("data").path("scheduleId").asText();

        // 4. 按排班启动无人直播（mock 开播）
        MvcResult startResult = mockMvc.perform(post("/api/digital-humans/schedules/{id}/start", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.liveStatus").value("live"))
                .andExpect(jsonPath("$.data.mockFlag").value(true))
                .andReturn();
        String sessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data").path("sessionId").asText();

        // 5. 心跳保活 + mock 指标增长
        mockMvc.perform(post("/api/digital-humans/sessions/{id}/heartbeat", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewerPeak").value(org.hamcrest.Matchers.greaterThan(0)));

        // 6. 概览：在播会话 = 1
        mockMvc.perform(get("/api/digital-humans/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarCount").value(1))
                .andExpect(jsonPath("$.data.scriptCount").value(1))
                .andExpect(jsonPath("$.data.scheduleCount").value(1))
                .andExpect(jsonPath("$.data.liveSessionCount").value(1));

        // 7. 停播
        mockMvc.perform(post("/api/digital-humans/schedules/{id}/stop", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveStatus").value("ended"));

        // 8. 会话列表可见已结束会话
        mockMvc.perform(get("/api/digital-humans/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].liveStatus").value("ended"));

        // 9. 更新形象并生效
        mockMvc.perform(put("/api/digital-humans/avatars/{id}", avatarId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "avatarName": "带货主播小美V2",
                                  "avatarType": "3D",
                                  "voiceName": "知性女声",
                                  "previewUrl": "mock://avatar/mei-v2.png",
                                  "enabled": true
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarName").value("带货主播小美V2"))
                .andExpect(jsonPath("$.data.avatarType").value("3D"));
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-dh",
                                  "mobile": "13900000000"
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
