package backend.digitalhuman.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class DigitalHumanInteractionControllerTest {

    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAnswerBarragesByProductAndQuestionTypeWithFallback() throws Exception {
        JsonNode tenant = registerTenant("digital-human-interaction-test");
        String organizationId = tenant.path("defaultOrganizationId").asText();
        String token = login("13911111111", BOOTSTRAP_PASSWORD);

        // 1. 商品1「无线蓝牙耳机」问答库：价格 / 续航 / 延迟 三类问题
        createFaq(token, organizationId, "earbud-1001", "无线蓝牙耳机", "price",
                "多少钱,价格,售价", "无线蓝牙耳机多少钱", "这款无线蓝牙耳机日常价 ¥199，直播间下单立减 ¥30，到手 ¥169。", true);
        createFaq(token, organizationId, "earbud-1001", "无线蓝牙耳机", "usage",
                "续航,充电", "无线蓝牙耳机续航怎么样", "单次充满可连续播放 8 小时，配合充电仓总续航约 24 小时。", true);
        createFaq(token, organizationId, "earbud-1001", "无线蓝牙耳机", "generic",
                "延迟,卡顿", "无线蓝牙耳机延迟高吗", "采用低延迟芯片，影音延迟低至 60ms，打游戏也基本感觉不到。", true);

        // 2. 商品2「轻薄羽绒服」问答库：尺码 / 售后 两类问题
        createFaq(token, organizationId, "jacket-2002", "轻薄羽绒服", "size",
                "尺码,大小,码数", "轻薄羽绒服有尺码推荐吗", "尺码从 S 到 XXL 都有，您拍下后把身高体重发给客服，帮您推荐最合适的码。", true);
        createFaq(token, organizationId, "jacket-2002", "轻薄羽绒服", "after_sale",
                "退货,退款,七天", "轻薄羽绒服可以退货吗", "支持 7 天无理由退换，只要吊牌完好、不影响二次销售就可以。", true);

        // 3. 不同商品、不同问题 → 对应回答
        MvcResult priceReply = submitBarrage(token, organizationId, "这个无线蓝牙耳机多少钱呀");
        assertAnswerContains(priceReply, "169");
        assertHitType(priceReply, "faq");

        MvcResult afterSaleReply = submitBarrage(token, organizationId, "羽绒服可以退货吗");
        assertAnswerContains(afterSaleReply, "7 天无理由");
        assertHitType(afterSaleReply, "faq");

        MvcResult sizeReply = submitBarrage(token, organizationId, "这个轻薄羽绒服尺码怎么选");
        assertAnswerContains(sizeReply, "客服");
        assertHitType(sizeReply, "faq");

        MvcResult usageReply = submitBarrage(token, organizationId, "无线蓝牙耳机续航怎么样");
        assertAnswerContains(usageReply, "8 小时");
        assertHitType(usageReply, "faq");

        // 4. 未命中知识库 → 兜底话术
        MvcResult fallbackReply = submitBarrage(token, organizationId, "主播在吗 有链接吗");
        assertHitType(fallbackReply, "fallback");

        // 5. 模拟一批弹幕（演示用）
        mockMvc.perform(post("/api/digital-humans/barrages/simulate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "sessionId": null,
                                  "platformType": "douyin",
                                  "count": 4
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));

        // 6. 互动记录回读（含命中类型）
        mockMvc.perform(get("/api/digital-humans/barrages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(4)));

        mockMvc.perform(get("/api/digital-humans/interaction-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.faqCount").value(5))
                .andExpect(jsonPath("$.data.barrageCount").value(greaterThan(4)))
                .andExpect(jsonPath("$.data.repliedCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.hitRate").isNumber())
                .andExpect(jsonPath("$.data.hotQuestions[0].hitCount").value(greaterThan(0)));

        // 7. 更新 FAQ：改答案后再次命中返回新答案
        String faqId = listFirstFaqId(token);
        mockMvc.perform(put("/api/digital-humans/faqs/{id}", faqId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "productId": "earbud-1001",
                                  "productName": "无线蓝牙耳机",
                                  "questionType": "price",
                                  "keywords": "多少钱,价格,售价",
                                  "question": "无线蓝牙耳机多少钱",
                                  "answer": "直播间福利价 ¥129，先到先得。",
                                  "enabled": true
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("直播间福利价 ¥129，先到先得。"));

        // 8. 删除一条 FAQ 后生效
        mockMvc.perform(delete("/api/digital-humans/faqs/{id}", faqId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/digital-humans/faqs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    private void createFaq(String token, String organizationId, String productId, String productName,
                           String questionType, String keywords, String question, String answer,
                           boolean enabled) throws Exception {
        mockMvc.perform(post("/api/digital-humans/faqs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "productId": "%s",
                                  "productName": "%s",
                                  "questionType": "%s",
                                  "keywords": "%s",
                                  "question": "%s",
                                  "answer": "%s",
                                  "enabled": %b
                                }
                                """.formatted(organizationId, productId, productName, questionType,
                                keywords, question, answer, enabled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.faqId").isNotEmpty())
                .andReturn();
    }

    private MvcResult submitBarrage(String token, String organizationId, String content) throws Exception {
        return mockMvc.perform(post("/api/digital-humans/barrages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "sessionId": null,
                                  "platformType": "douyin",
                                  "userNick": "测试观众",
                                  "content": "%s"
                                }
                                """.formatted(organizationId, content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interaction.answer").isNotEmpty())
                .andReturn();
    }

    private void assertAnswerContains(MvcResult result, String fragment) throws Exception {
        String answer = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("interaction").path("answer").asText();
        org.junit.jupiter.api.Assertions.assertTrue(answer.contains(fragment),
                "answer should contain '" + fragment + "' but was: " + answer);
    }

    private void assertHitType(MvcResult result, String expected) throws Exception {
        String hitType = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("interaction").path("hitType").asText();
        org.junit.jupiter.api.Assertions.assertEquals(expected, hitType);
    }

    private String listFirstFaqId(String token) throws Exception {
        String content = mockMvc.perform(get("/api/digital-humans/faqs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(content).path("data");
        for (JsonNode node : data) {
            if ("price".equals(node.path("questionType").asText())
                    && "无线蓝牙耳机".equals(node.path("productName").asText())) {
                return node.path("faqId").asText();
            }
        }
        throw new IllegalStateException("price faq not found");
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-interaction",
                                  "mobile": "13911111111"
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
