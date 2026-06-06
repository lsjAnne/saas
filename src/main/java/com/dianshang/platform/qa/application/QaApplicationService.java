package com.dianshang.platform.qa.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.qa.application.QaService.SendConversationMessageRequest;
import com.dianshang.platform.qa.application.QaService.TransferConversationManualRequest;
import com.dianshang.platform.qa.domain.repository.ConversationMessageRepository;
import com.dianshang.platform.qa.domain.repository.CustomerConversationRepository;
import com.dianshang.platform.qa.domain.repository.FaqKnowledgeRepository;
import com.dianshang.platform.qa.dto.CreateFaqKnowledgeRequest;
import com.dianshang.platform.qa.dto.ReplySuggestionView;
import com.dianshang.platform.qa.model.ConversationMessage;
import com.dianshang.platform.qa.model.CustomerConversation;
import com.dianshang.platform.qa.model.FaqKnowledge;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class QaApplicationService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of("赔偿", "投诉", "退款", "维权", "假货", "complaint", "refund");
    private static final String DEFAULT_AI_REPLY = "已收到您的问题，我们正在为您核实具体情况，稍后给您更准确的处理建议。";

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final CustomerConversationRepository customerConversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final FaqKnowledgeRepository faqKnowledgeRepository;

    public QaApplicationService(AuditLogService auditLogService,
                                StoreRepository storeRepository,
                                CustomerConversationRepository customerConversationRepository,
                                ConversationMessageRepository conversationMessageRepository,
                                FaqKnowledgeRepository faqKnowledgeRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.customerConversationRepository = customerConversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.faqKnowledgeRepository = faqKnowledgeRepository;
    }

    public List<CustomerConversation> listConversations(String tenantId) {
        return customerConversationRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public CustomerConversation getConversation(String tenantId, String conversationId) {
        CustomerConversation conversation = customerConversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, conversation.storeId());
        return conversation;
    }

    public List<ConversationMessage> listMessages(String tenantId, String conversationId) {
        getConversation(tenantId, conversationId);
        return conversationMessageRepository.findByConversationId(conversationId);
    }

    public ReplySuggestionView generateReplySuggestion(String tenantId, String conversationId) {
        CustomerConversation conversation = getConversation(tenantId, conversationId);
        List<ConversationMessage> messages = conversationMessageRepository.findByConversationId(conversationId);
        ConversationMessage latestCustomerMessage = messages.stream()
                .filter(message -> "customer".equals(message.senderType()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new BusinessException("7701", "customer message not found", HttpStatus.BAD_REQUEST));

        FaqKnowledge matchedFaq = matchFaq(conversation.storeId(), latestCustomerMessage.contentText());
        String sourceType = matchedFaq != null ? matchedFaq.sourceType() : "ai";
        String matchedQuestion = matchedFaq != null ? matchedFaq.question() : null;
        boolean highRisk = conversation.riskFlag() || containsHighRiskKeyword(latestCustomerMessage.contentText());
        String suggestedReply = matchedFaq != null ? matchedFaq.answer() : DEFAULT_AI_REPLY;

        ConversationMessage aiMessage = conversationMessageRepository.save(new ConversationMessage(
                null,
                conversationId,
                "ai",
                "text",
                suggestedReply,
                true,
                highRisk,
                OffsetDateTime.now()
        ));
        String nextStatus = highRisk ? "waiting_manual" : "auto_replied";
        customerConversationRepository.save(conversation.withStatus(nextStatus, aiMessage.createdAt()));
        auditLogService.recordForTenant(tenantId, "GENERATE_CONVERSATION_REPLY_SUGGESTION", "customer_conversation", conversationId);

        return new ReplySuggestionView(
                conversationId,
                aiMessage.messageId(),
                suggestedReply,
                sourceType,
                !highRisk,
                matchedQuestion
        );
    }

    public ConversationMessage sendMessage(String tenantId, String conversationId, SendConversationMessageRequest request) {
        CustomerConversation conversation = getConversation(tenantId, conversationId);
        ConversationMessage agentMessage = conversationMessageRepository.save(new ConversationMessage(
                null,
                conversationId,
                "agent",
                "text",
                request.contentText(),
                false,
                false,
                OffsetDateTime.now()
        ));
        customerConversationRepository.save(conversation.withStatus("processing", agentMessage.createdAt()));
        auditLogService.recordForTenant(tenantId, "SEND_CONVERSATION_MESSAGE", "customer_conversation", conversationId);
        return agentMessage;
    }

    public CustomerConversation transferManual(String tenantId, String conversationId, TransferConversationManualRequest request) {
        CustomerConversation conversation = getConversation(tenantId, conversationId);
        ConversationMessage systemMessage = conversationMessageRepository.save(new ConversationMessage(
                null,
                conversationId,
                "system",
                "text",
                "transfer to manual: " + request.reason(),
                false,
                true,
                OffsetDateTime.now()
        ));
        CustomerConversation updated = customerConversationRepository.save(
                conversation.withStatusAndRisk("waiting_manual", true, systemMessage.createdAt())
        );
        auditLogService.recordForTenant(tenantId, "TRANSFER_CONVERSATION_MANUAL", "customer_conversation", conversationId);
        return updated;
    }

    public List<FaqKnowledge> listFaqKnowledge(String tenantId) {
        return faqKnowledgeRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public FaqKnowledge createFaqKnowledge(String tenantId, CreateFaqKnowledgeRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        FaqKnowledge faqKnowledge = faqKnowledgeRepository.save(new FaqKnowledge(
                null,
                request.storeId(),
                request.productId(),
                request.question(),
                request.answer(),
                request.sourceType(),
                request.enabled() == null || request.enabled(),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_FAQ_KNOWLEDGE", "faq_knowledge", faqKnowledge.faqId());
        return faqKnowledge;
    }

    public FaqKnowledge updateFaqKnowledge(String tenantId, String faqId, CreateFaqKnowledgeRequest request) {
        FaqKnowledge current = listFaqKnowledge(tenantId).stream()
                .filter(item -> faqId.equals(item.faqId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!current.storeId().equals(request.storeId())) {
            throw new BusinessException("1004", "storeId does not match current faq", HttpStatus.BAD_REQUEST);
        }
        requireOwnedStore(tenantId, request.storeId());
        FaqKnowledge updated = faqKnowledgeRepository.save(new FaqKnowledge(
                faqId,
                current.storeId(),
                request.productId(),
                request.question(),
                request.answer(),
                request.sourceType(),
                request.enabled() == null ? current.enabled() : request.enabled(),
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_FAQ_KNOWLEDGE", "faq_knowledge", faqId);
        return updated;
    }

    public CustomerConversation createConversationSeed(String tenantId,
                                                       String storeId,
                                                       String platformType,
                                                       String platformConversationId,
                                                       String customerId,
                                                       boolean riskFlag) {
        requireOwnedStore(tenantId, storeId);
        return customerConversationRepository.save(new CustomerConversation(
                null,
                storeId,
                platformType,
                platformConversationId,
                customerId,
                "open",
                riskFlag,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));
    }

    public ConversationMessage createMessageSeed(String tenantId,
                                                 String conversationId,
                                                 String senderType,
                                                 String contentText,
                                                 boolean riskFlag) {
        CustomerConversation conversation = getConversation(tenantId, conversationId);
        ConversationMessage message = conversationMessageRepository.save(new ConversationMessage(
                null,
                conversationId,
                senderType,
                "text",
                contentText,
                false,
                riskFlag,
                OffsetDateTime.now()
        ));
        customerConversationRepository.save(conversation.withStatus(conversation.conversationStatus(), message.createdAt()));
        return message;
    }

    public void clear() {
        conversationMessageRepository.deleteAll();
        customerConversationRepository.deleteAll();
        faqKnowledgeRepository.deleteAll();
    }

    private FaqKnowledge matchFaq(String storeId, String customerText) {
        String normalizedCustomerText = normalize(customerText);
        return faqKnowledgeRepository.findByStoreIds(List.of(storeId)).stream()
                .filter(FaqKnowledge::enabled)
                .filter(faq -> {
                    String normalizedQuestion = normalize(faq.question());
                    return normalizedCustomerText.contains(normalizedQuestion) || normalizedQuestion.contains(normalizedCustomerText);
                })
                .findFirst()
                .orElse(null);
    }

    private boolean containsHighRiskKeyword(String content) {
        String normalized = normalize(content);
        return HIGH_RISK_KEYWORDS.stream().map(this::normalize).anyMatch(normalized::contains);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replace("，", "")
                .replace("。", "")
                .replace("？", "")
                .replace("?", "")
                .replace(",", "")
                .trim();
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }
}
