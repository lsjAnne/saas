package com.dianshang.platform.qa;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.qa.application.QaService;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.store.application.StoreChannelService;
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
class QaConversationControllerTest {

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

    @Autowired
    private QaService qaService;

    @BeforeEach
    void setUp() {
        qaService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldManageConversationsMessagesAndFaqKnowledge() throws Exception {
        QaFixture fixture = prepareFixture();

        MvcResult faqResult = mockMvc.perform(post("/api/faq-knowledge")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "question": "多久发货",
                                  "answer": "您好，默认 48 小时内安排发货。",
                                  "sourceType": "manual"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceType").value("manual"))
                .andReturn();

        String faqId = objectMapper.readTree(faqResult.getResponse().getContentAsString())
                .path("data")
                .path("faqId")
                .asText();

        String conversationId = qaService.createConversationSeed(
                        fixture.tenantId(),
                        fixture.storeId(),
                        "douyin",
                        "conv-9001",
                        "customer-qa-1",
                        false)
                .conversationId();
        qaService.createMessageSeed(
                fixture.tenantId(),
                conversationId,
                "customer",
                "请问多久发货？",
                false
        );

        mockMvc.perform(get("/api/conversations")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].conversationId").value(conversationId));

        mockMvc.perform(get("/api/conversations/{id}", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationStatus").value("open"));

        mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].senderType").value("customer"));

        mockMvc.perform(post("/api/conversations/{id}/reply-suggestion", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.suggestedReply").value("您好，默认 48 小时内安排发货。"))
                .andExpect(jsonPath("$.data.sourceType").value("manual"))
                .andExpect(jsonPath("$.data.autoSendEligible").value(true))
                .andExpect(jsonPath("$.data.matchedQuestion").value("多久发货"));

        mockMvc.perform(get("/api/conversations/{id}", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationStatus").value("auto_replied"));

        mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].senderType").value("ai"))
                .andExpect(jsonPath("$.data[1].aiGeneratedFlag").value(true));

        mockMvc.perform(get("/api/faq-knowledge")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].faqId").value(faqId));
    }

    @Test
    void shouldSendConversationMessageAndMarkConversationProcessing() throws Exception {
        QaFixture fixture = prepareFixture();
        String conversationId = qaService.createConversationSeed(
                        fixture.tenantId(),
                        fixture.storeId(),
                        "douyin",
                        "conv-9002",
                        "customer-qa-2",
                        false)
                .conversationId();
        qaService.createMessageSeed(
                fixture.tenantId(),
                conversationId,
                "customer",
                "need manual follow up",
                false
        );

        mockMvc.perform(post("/api/conversations/{id}/send", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentText": "manual reply sent by agent"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.senderType").value("agent"))
                .andExpect(jsonPath("$.data.aiGeneratedFlag").value(false))
                .andExpect(jsonPath("$.data.contentText").value("manual reply sent by agent"));

        mockMvc.perform(get("/api/conversations/{id}", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationStatus").value("processing"));

        mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].senderType").value("agent"));
    }

    @Test
    void shouldTransferConversationToManualAndRecordReason() throws Exception {
        QaFixture fixture = prepareFixture();
        String conversationId = qaService.createConversationSeed(
                        fixture.tenantId(),
                        fixture.storeId(),
                        "douyin",
                        "conv-9003",
                        "customer-qa-3",
                        false)
                .conversationId();
        qaService.createMessageSeed(
                fixture.tenantId(),
                conversationId,
                "customer",
                "complaint needs escalation",
                false
        );

        mockMvc.perform(post("/api/conversations/{id}/transfer-manual", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "risk complaint requires manual handling"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.conversationStatus").value("waiting_manual"))
                .andExpect(jsonPath("$.data.riskFlag").value(true));

        mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].senderType").value("system"))
                .andExpect(jsonPath("$.data[1].contentText").value("transfer to manual: risk complaint requires manual handling"));
    }

    @Test
    void shouldUpdateFaqKnowledgeAndDisableItFromFutureMatch() throws Exception {
        QaFixture fixture = prepareFixture();

        MvcResult faqResult = mockMvc.perform(post("/api/faq-knowledge")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "question": "shipping time",
                                  "answer": "ship in 48 hours",
                                  "sourceType": "manual"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andReturn();
        String faqId = objectMapper.readTree(faqResult.getResponse().getContentAsString())
                .path("data")
                .path("faqId")
                .asText();

        mockMvc.perform(put("/api/faq-knowledge/{id}", faqId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "question": "shipping time",
                                  "answer": "ship in 72 hours",
                                  "sourceType": "manual",
                                  "enabled": false
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.faqId").value(faqId))
                .andExpect(jsonPath("$.data.answer").value("ship in 72 hours"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        String conversationId = qaService.createConversationSeed(
                        fixture.tenantId(),
                        fixture.storeId(),
                        "douyin",
                        "conv-9004",
                        "customer-qa-4",
                        false)
                .conversationId();
        qaService.createMessageSeed(
                fixture.tenantId(),
                conversationId,
                "customer",
                "shipping time",
                false
        );

        mockMvc.perform(post("/api/conversations/{id}/reply-suggestion", conversationId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceType").value("ai"))
                .andExpect(jsonPath("$.data.matchedQuestion").isEmpty());
    }

    private QaFixture prepareFixture() throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "问答测试中心",
                                  "ownerName": "吴八",
                                  "mobile": "13500000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tenantData = objectMapper.readTree(tenantResult.getResponse().getContentAsString()).path("data");
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();

        MvcResult storeResult = mockMvc.perform(post("/api/stores/connect")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "shop-qa-601",
                                  "shopName": "问答测试店",
                                  "profitThreshold": 16.00,
                                  "riskThreshold": 72.00
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andReturn();
        String storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();

        return new QaFixture(tenantId, storeId);
    }
}

record QaFixture(String tenantId, String storeId) {
}
