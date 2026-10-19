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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数字人自成长闭环测试：
 * 未命中知识库的问题进入学习记忆 → 同类问题语义聚合（同义改写归并为同一条）→
 * 出现 3 次自动提炼入库：有答案源直接启用（立即可答）；无答案源标记待完善（不参与命中，避免答非所问）→
 * 运营可确认/忽略 → 成长指标反映知识库增长与待完善情况。
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class DigitalHumanLearningControllerTest {

    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLearnUnansweredQuestionsAndAutoGrowKnowledgeBase() throws Exception {
        JsonNode tenant = registerTenant("digital-human-learning-test", "13922222222");
        String organizationId = tenant.path("defaultOrganizationId").asText();
        String token = login("13922222222", BOOTSTRAP_PASSWORD);

        // 1. 知识库初始 1 条（无线蓝牙耳机-价格，无 generic 答案源）
        createFaq(token, organizationId, "earbud-1001", "无线蓝牙耳机", "price",
                "多少钱,价格,售价", "无线蓝牙耳机多少钱",
                "这款无线蓝牙耳机日常价 ¥199，直播间下单立减 ¥30，到手 ¥169。");

        // 2. 三条"未命中"弹幕：语义相似（同义改写），知识库中没有该问答
        submitBarrage(token, organizationId, "这个耳机有几种颜色呀");
        submitBarrage(token, organizationId, "耳机有哪几种颜色");
        submitBarrage(token, organizationId, "颜色有几种");

        // 同义改写应归并为同一条并累计 3 次，触发自动提炼
        JsonNode items = listLearningItems(token);
        JsonNode colorItem = findItem(items, "颜色");
        assertNotNull(colorItem, "学习记忆应记住未答的颜色问题");
        assertEquals("confirmed", colorItem.path("status").asText(), "出现 3 次同类问题应自动转正");
        assertEquals(3, colorItem.path("occurrenceCount").asLong(), "同义改写应归并为同一条并累计 3 次");
        assertTrue(colorItem.path("faqId").asText().startsWith("dhfaq-"), "应自动生成 FAQ 并关联");

        // 3. 自动生成的 FAQ 无答案源 → 标记待完善（enabled=false），保留在知识库但不参与命中
        mockMvc.perform(get("/api/digital-humans/faqs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 4. 待完善知识不参与命中：同一问题再次提问仍走兜底（避免答非所问），并继续被记忆
        MvcResult reAsked = submitBarrage(token, organizationId, "耳机有哪几种颜色");
        assertEquals("fallback", hitTypeOf(reAsked), "待完善的自动知识不参与命中，防止答非所问");

        // 5. 成长指标：自动学习入库 1 条、待完善 1 条、转化率 100%
        mockMvc.perform(get("/api/digital-humans/learning/growth")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoLearnedCount").value(1))
                .andExpect(jsonPath("$.data.pendingFaqCount").value(1))
                .andExpect(jsonPath("$.data.conversionRate").value(greaterThan(0.0)));
    }

    @Test
    void shouldConfirmOrIgnoreLearningItemsAndKeepPendingKnowledgeUnmatched() throws Exception {
        JsonNode tenant = registerTenant("digital-human-learning-confirm-test", "13933333333");
        String organizationId = tenant.path("defaultOrganizationId").asText();
        String token = login("13933333333", BOOTSTRAP_PASSWORD);

        // 1. 知识库初始 1 条（无线蓝牙耳机-价格）；generic/shipping 类型无任何条目 → 相关问题必然未命中
        createFaq(token, organizationId, "earbud-1001", "无线蓝牙耳机", "price",
                "多少钱,价格,售价", "无线蓝牙耳机多少钱",
                "这款无线蓝牙耳机日常价 ¥199，直播间下单立减 ¥30，到手 ¥169。");

        // 2. 两条未命中问题进入学习记忆（new）
        submitBarrage(token, organizationId, "可以开发票吗");
        submitBarrage(token, organizationId, "在哪里看物流单号");
        JsonNode itemsAfter = listLearningItems(token);
        JsonNode invoiceItem = findItem(itemsAfter, "发票");
        JsonNode logisticsItem = findItem(itemsAfter, "物流");
        assertNotNull(invoiceItem);
        assertNotNull(logisticsItem);
        assertEquals("new", invoiceItem.path("status").asText());

        // 3. 运营确认发票问题 → 转正为"待完善"知识（无答案源，enabled=false，不参与命中）
        String invoiceId = invoiceItem.path("itemId").asText();
        mockMvc.perform(post("/api/digital-humans/learning/items/{id}/confirm", invoiceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"))
                .andExpect(jsonPath("$.data.faqId").isNotEmpty());

        mockMvc.perform(get("/api/digital-humans/faqs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        // 待完善知识不参与命中：同义问题再次提问仍走兜底，且归并到已确认条目累计次数
        MvcResult reAsked = submitBarrage(token, organizationId, "能开发票吗");
        assertEquals("fallback", hitTypeOf(reAsked), "待完善的自动知识不参与命中，防止答非所问");
        JsonNode itemsMerged = listLearningItems(token);
        JsonNode invoiceMerged = findItem(itemsMerged, "发票");
        assertEquals(2, invoiceMerged.path("occurrenceCount").asLong(), "已确认条目继续聚合同类提问");

        // 成长指标：自动学习 1 条、待完善 1 条（enabled 知识库仍只有手建 1 条）
        mockMvc.perform(get("/api/digital-humans/learning/growth")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.faqCount").value(1))
                .andExpect(jsonPath("$.data.autoLearnedCount").value(1))
                .andExpect(jsonPath("$.data.pendingFaqCount").value(1))
                .andExpect(jsonPath("$.data.statusConfirmedCount").value(1));

        // 4. 运营忽略物流问题 → 不再参与自动提炼
        String logisticsId = logisticsItem.path("itemId").asText();
        mockMvc.perform(post("/api/digital-humans/learning/items/{id}/ignore", logisticsId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ignored"));

        // 被忽略的问题再出现 → 独立记录，不再归并累积
        submitBarrage(token, organizationId, "物流单号在哪里看");
        JsonNode itemsFinal = listLearningItems(token);
        JsonNode ignoredLogistics = findItemWithStatus(itemsFinal, "物流", "ignored");
        assertEquals(1, ignoredLogistics.path("occurrenceCount").asLong(), "忽略后不再累积次数");
        assertNotNull(findItemWithStatus(itemsFinal, "物流", "new"), "被忽略的问题再次提问应独立记录");
    }

    // ---------- helpers ----------

    private void createFaq(String token, String organizationId, String productId, String productName,
                           String questionType, String keywords, String question, String answer) throws Exception {
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
                                  "enabled": true
                                }
                                """.formatted(organizationId, productId == null ? "" : productId,
                                productName == null ? "" : productName, questionType,
                                keywords, question, answer)))
                .andExpect(status().isOk())
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
                                  "userNick": "学习观众",
                                  "content": "%s"
                                }
                                """.formatted(organizationId, content)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String hitTypeOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("interaction").path("hitType").asText();
    }

    private JsonNode listLearningItems(String token) throws Exception {
        String content = mockMvc.perform(get("/api/digital-humans/learning/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content).path("data");
    }

    private JsonNode findItem(JsonNode items, String fragment) {
        for (JsonNode node : items) {
            if (node.path("question").asText().contains(fragment)) {
                return node;
            }
        }
        return null;
    }

    private JsonNode findItemWithStatus(JsonNode items, String fragment, String status) {
        for (JsonNode node : items) {
            if (node.path("question").asText().contains(fragment)
                    && status.equals(node.path("status").asText())) {
                return node;
            }
        }
        return null;
    }

    private JsonNode registerTenant(String tenantName, String mobile) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-learning",
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
