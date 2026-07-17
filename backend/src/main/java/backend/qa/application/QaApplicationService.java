package backend.qa.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.qa.application.QaService.SendConversationMessageRequest;
import backend.qa.application.QaService.TransferConversationManualRequest;
import backend.qa.domain.repository.ConversationMessageRepository;
import backend.qa.domain.repository.CustomerConversationRepository;
import backend.qa.domain.repository.FaqKnowledgeRepository;
import backend.qa.dto.CreateFaqKnowledgeRequest;
import backend.qa.dto.ReplySuggestionView;
import backend.qa.model.ConversationMessage;
import backend.qa.model.CustomerConversation;
import backend.qa.model.FaqKnowledge;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class QaApplicationService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "complaint",
            "refund",
            "chargeback",
            "fraud",
            "lawsuit",
            "scam",
            "\u6295\u8bc9",
            "\u9000\u6b3e",
            "\u7ef4\u6743",
            "\u8d54\u507f",
            "\u6b3a\u8bc8"
    );
    private static final List<String> SHIPPING_KEYWORDS = List.of(
            "shipping",
            "delivery",
            "arrive",
            "\u7269\u6d41",
            "\u53d1\u8d27",
            "\u5230\u8d27",
            "\u5feb\u9012"
    );
    private static final List<String> REFUND_KEYWORDS = List.of(
            "refund",
            "return",
            "\u552e\u540e",
            "\u9000\u6b3e",
            "\u9000\u8d27",
            "\u8865\u507f"
    );
    private static final List<String> PAYMENT_KEYWORDS = List.of(
            "payment",
            "price",
            "invoice",
            "\u4ed8\u6b3e",
            "\u652f\u4ed8",
            "\u4ef7\u683c",
            "\u53d1\u7968"
    );
    private static final List<String> COMPLAINT_KEYWORDS = List.of(
            "complaint",
            "angry",
            "\u6295\u8bc9",
            "\u5dee\u8bc4",
            "\u7ef4\u6743",
            "scam"
    );

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
        String intentCode = resolveIntentCode(latestCustomerMessage.contentText());
        String sourceType = matchedFaq != null ? matchedFaq.sourceType() : "general".equals(intentCode) ? "ai_fallback" : "ai_intent";
        String matchedQuestion = matchedFaq != null ? matchedFaq.question() : null;
        List<String> riskLabels = buildRiskLabels(conversation, latestCustomerMessage.contentText(), intentCode);
        boolean highRisk = !riskLabels.isEmpty();
        String suggestedReply = matchedFaq != null
                ? matchedFaq.answer()
                : generateIntentReply(intentCode, highRisk);
        String confidenceLevel = matchedFaq != null
                ? "high"
                : "general".equals(intentCode)
                ? "low"
                : "medium";
        String handoffReason = highRisk
                ? buildHandoffReason(riskLabels, intentCode)
                : null;
        String recommendedAction = highRisk
                ? "transfer_manual"
                : matchedFaq != null
                ? "apply_or_send"
                : "review_before_send";
        String knowledgeSourceSummary = matchedFaq != null
                ? "faq:" + matchedFaq.question()
                : "intent:" + intentCode;

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
                matchedQuestion,
                confidenceLevel,
                handoffReason,
                riskLabels,
                recommendedAction,
                knowledgeSourceSummary
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

    private String resolveIntentCode(String content) {
        String normalized = normalize(content);
        if (containsKeyword(normalized, SHIPPING_KEYWORDS)) {
            return "shipping";
        }
        if (containsKeyword(normalized, REFUND_KEYWORDS)) {
            return "refund";
        }
        if (containsKeyword(normalized, PAYMENT_KEYWORDS)) {
            return "payment";
        }
        if (containsKeyword(normalized, COMPLAINT_KEYWORDS)) {
            return "complaint";
        }
        return "general";
    }

    private List<String> buildRiskLabels(CustomerConversation conversation, String content, String intentCode) {
        List<String> labels = new ArrayList<>();
        if (conversation.riskFlag()) {
            labels.add("conversation_risk_flag");
        }
        if (containsHighRiskKeyword(content)) {
            labels.add("high_risk_keyword");
        }
        if ("refund".equals(intentCode)) {
            labels.add("after_sale_request");
        }
        if ("complaint".equals(intentCode)) {
            labels.add("complaint_escalation");
        }
        return labels.stream().distinct().toList();
    }

    private String generateIntentReply(String intentCode, boolean highRisk) {
        String suffix = highRisk
                ? "This request should be reviewed by a human agent before final confirmation."
                : "Please review the wording before sending it to the customer.";
        return switch (intentCode) {
            case "shipping" -> "We have received your delivery question. We are checking the latest shipping progress and will sync the confirmed timeline shortly. " + suffix;
            case "refund" -> "We have received your refund or after-sales request. We are verifying the order status, return eligibility, and next processing step. " + suffix;
            case "payment" -> "We have received your payment or pricing question. We are checking the payable amount, discount condition, and related billing detail. " + suffix;
            case "complaint" -> "We are sorry for the inconvenience. We have recorded the complaint and will review the related order, fulfillment, and service history immediately. " + suffix;
            default -> "We have received your request and are checking the relevant order, product, and service context. " + suffix;
        };
    }

    private String buildHandoffReason(List<String> riskLabels, String intentCode) {
        if (riskLabels.contains("complaint_escalation")) {
            return "complaint or rights-protection intent detected, manual handling is required";
        }
        if (riskLabels.contains("after_sale_request")) {
            return "after-sales or refund intent detected, manual review is required";
        }
        if (riskLabels.contains("high_risk_keyword")) {
            return "high-risk keyword detected, manual review is required";
        }
        if (riskLabels.contains("conversation_risk_flag")) {
            return "conversation is already marked as risky, manual review is required";
        }
        return "intent " + intentCode + " requires manual review";
    }

    private boolean containsKeyword(String normalized, List<String> keywords) {
        return keywords.stream().map(this::normalize).anyMatch(normalized::contains);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replace("?", "")
                .replace("!", "")
                .replace(",", "")
                .replace(".", "")
                .replace(":", "")
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
