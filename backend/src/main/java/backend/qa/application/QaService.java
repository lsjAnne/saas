package backend.qa.application;

import backend.qa.dto.CreateFaqKnowledgeRequest;
import backend.qa.dto.ReplySuggestionView;
import backend.qa.model.ConversationMessage;
import backend.qa.model.CustomerConversation;
import backend.qa.model.FaqKnowledge;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Deprecated(forRemoval = false)
public class QaService {

    private final QaApplicationService qaApplicationService;

    public QaService(QaApplicationService qaApplicationService) {
        this.qaApplicationService = qaApplicationService;
    }

    public List<CustomerConversation> listConversations(String tenantId) {
        return qaApplicationService.listConversations(tenantId);
    }

    public CustomerConversation getConversation(String tenantId, String conversationId) {
        return qaApplicationService.getConversation(tenantId, conversationId);
    }

    public List<ConversationMessage> listMessages(String tenantId, String conversationId) {
        return qaApplicationService.listMessages(tenantId, conversationId);
    }

    public ReplySuggestionView generateReplySuggestion(String tenantId, String conversationId) {
        return qaApplicationService.generateReplySuggestion(tenantId, conversationId);
    }

    public ConversationMessage sendMessage(String tenantId, String conversationId, SendConversationMessageRequest request) {
        return qaApplicationService.sendMessage(tenantId, conversationId, request);
    }

    public CustomerConversation transferManual(String tenantId, String conversationId, TransferConversationManualRequest request) {
        return qaApplicationService.transferManual(tenantId, conversationId, request);
    }

    public List<FaqKnowledge> listFaqKnowledge(String tenantId) {
        return qaApplicationService.listFaqKnowledge(tenantId);
    }

    public FaqKnowledge createFaqKnowledge(String tenantId, CreateFaqKnowledgeRequest request) {
        return qaApplicationService.createFaqKnowledge(tenantId, request);
    }

    public FaqKnowledge updateFaqKnowledge(String tenantId, String faqId, CreateFaqKnowledgeRequest request) {
        return qaApplicationService.updateFaqKnowledge(tenantId, faqId, request);
    }

    public CustomerConversation createConversationSeed(String tenantId,
                                                       String storeId,
                                                       String platformType,
                                                       String platformConversationId,
                                                       String customerId,
                                                       boolean riskFlag) {
        return qaApplicationService.createConversationSeed(
                tenantId,
                storeId,
                platformType,
                platformConversationId,
                customerId,
                riskFlag
        );
    }

    public ConversationMessage createMessageSeed(String tenantId,
                                                 String conversationId,
                                                 String senderType,
                                                 String contentText,
                                                 boolean riskFlag) {
        return qaApplicationService.createMessageSeed(tenantId, conversationId, senderType, contentText, riskFlag);
    }

    public void clear() {
        qaApplicationService.clear();
    }

    public record SendConversationMessageRequest(
            @NotBlank(message = "contentText is required")
            String contentText
    ) {
    }

    public record TransferConversationManualRequest(
            @NotBlank(message = "reason is required")
            String reason
    ) {
    }
}

