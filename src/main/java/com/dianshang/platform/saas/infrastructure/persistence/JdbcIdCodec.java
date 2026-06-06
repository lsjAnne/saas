package com.dianshang.platform.saas.infrastructure.persistence;

public final class JdbcIdCodec {

    private JdbcIdCodec() {
    }

    public static long parseTenantId(String tenantId) {
        return parseNumericSuffix(tenantId, "tenant-");
    }

    public static String formatTenantId(long tenantId) {
        return "tenant-" + tenantId;
    }

    public static long parseSubscriptionId(String subscriptionId) {
        return parseNumericSuffix(subscriptionId, "sub-");
    }

    public static long parseBillingOrderId(String billingOrderId) {
        return parseNumericSuffix(billingOrderId, "bill-");
    }

    public static String formatBillingOrderId(long billingOrderId) {
        return "bill-" + billingOrderId;
    }

    public static long parseInvoiceRequestId(String invoiceRequestId) {
        return parseNumericSuffix(invoiceRequestId, "invoice-");
    }

    public static String formatInvoiceRequestId(long invoiceRequestId) {
        return "invoice-" + invoiceRequestId;
    }

    public static long parseOrganizationId(String organizationId) {
        return parseNumericSuffix(organizationId, "org-");
    }

    public static String formatOrganizationId(long organizationId) {
        return "org-" + organizationId;
    }

    public static long parseMemberId(String memberId) {
        return parseNumericSuffix(memberId, "member-");
    }

    public static String formatMemberId(long memberId) {
        return "member-" + memberId;
    }

    public static long parseMemberTagId(String memberTagId) {
        return parseNumericSuffix(memberTagId, "member-tag-");
    }

    public static String formatMemberTagId(long memberTagId) {
        return "member-tag-" + memberTagId;
    }

    public static long parseStoreId(String storeId) {
        return parseNumericSuffix(storeId, "store-");
    }

    public static String formatStoreId(long storeId) {
        return "store-" + storeId;
    }

    public static long parseChannelAccountId(String channelAccountId) {
        return parseNumericSuffix(channelAccountId, "channel-");
    }

    public static String formatChannelAccountId(long channelAccountId) {
        return "channel-" + channelAccountId;
    }

    public static long parseRuleId(String ruleId) {
        return parseNumericSuffix(ruleId, "rule-");
    }

    public static String formatRuleId(long ruleId) {
        return "rule-" + ruleId;
    }

    public static long parseApprovalId(String approvalId) {
        return parseNumericSuffix(approvalId, "approval-");
    }

    public static String formatApprovalId(long approvalId) {
        return "approval-" + approvalId;
    }

    public static long parseNotificationTaskId(String notificationTaskId) {
        return parseNumericSuffix(notificationTaskId, "notification-");
    }

    public static String formatNotificationTaskId(long notificationTaskId) {
        return "notification-" + notificationTaskId;
    }

    public static long parseNotificationTemplateId(String notificationTemplateId) {
        return parseNumericSuffix(notificationTemplateId, "notification-template-");
    }

    public static String formatNotificationTemplateId(long notificationTemplateId) {
        return "notification-template-" + notificationTemplateId;
    }

    public static long parseCandidateProductId(String candidateProductId) {
        return parseNumericSuffix(candidateProductId, "candidate-");
    }

    public static String formatCandidateProductId(long candidateProductId) {
        return "candidate-" + candidateProductId;
    }

    public static long parseSupplierId(String supplierId) {
        return parseNumericSuffix(supplierId, "supplier-");
    }

    public static String formatSupplierId(long supplierId) {
        return "supplier-" + supplierId;
    }

    public static long parseProductDraftId(String productDraftId) {
        return parseNumericSuffix(productDraftId, "draft-");
    }

    public static String formatProductDraftId(long productDraftId) {
        return "draft-" + productDraftId;
    }

    public static long parseProductId(String productId) {
        return parseNumericSuffix(productId, "product-");
    }

    public static String formatProductId(long productId) {
        return "product-" + productId;
    }

    public static long parseProductMappingId(String productMappingId) {
        return parseNumericSuffix(productMappingId, "mapping-");
    }

    public static String formatProductMappingId(long productMappingId) {
        return "mapping-" + productMappingId;
    }

    public static long parseSkuId(String skuId) {
        return parseNumericSuffix(skuId, "sku-");
    }

    public static String formatSkuId(long skuId) {
        return "sku-" + skuId;
    }

    public static long parseInventorySnapshotId(String inventorySnapshotId) {
        return parseNumericSuffix(inventorySnapshotId, "inventory-");
    }

    public static String formatInventorySnapshotId(long inventorySnapshotId) {
        return "inventory-" + inventorySnapshotId;
    }

    public static long parseReplenishmentTaskId(String replenishmentTaskId) {
        return parseNumericSuffix(replenishmentTaskId, "replenishment-");
    }

    public static String formatReplenishmentTaskId(long replenishmentTaskId) {
        return "replenishment-" + replenishmentTaskId;
    }

    public static long parseOrderId(String orderId) {
        return parseNumericSuffix(orderId, "order-");
    }

    public static String formatOrderId(long orderId) {
        return "order-" + orderId;
    }

    public static long parseOrderItemId(String orderItemId) {
        return parseNumericSuffix(orderItemId, "order-item-");
    }

    public static String formatOrderItemId(long orderItemId) {
        return "order-item-" + orderItemId;
    }

    public static long parseFulfillmentTaskId(String fulfillmentTaskId) {
        return parseNumericSuffix(fulfillmentTaskId, "fulfillment-");
    }

    public static String formatFulfillmentTaskId(long fulfillmentTaskId) {
        return "fulfillment-" + fulfillmentTaskId;
    }

    public static long parseLogisticsRecordId(String logisticsRecordId) {
        return parseNumericSuffix(logisticsRecordId, "logistics-");
    }

    public static String formatLogisticsRecordId(long logisticsRecordId) {
        return "logistics-" + logisticsRecordId;
    }

    public static long parseExceptionTaskId(String exceptionTaskId) {
        return parseNumericSuffix(exceptionTaskId, "exception-");
    }

    public static String formatExceptionTaskId(long exceptionTaskId) {
        return "exception-" + exceptionTaskId;
    }

    public static long parseTicketId(String ticketId) {
        return parseNumericSuffix(ticketId, "ticket-");
    }

    public static String formatTicketId(long ticketId) {
        return "ticket-" + ticketId;
    }

    public static long parseAfterSaleId(String afterSaleId) {
        return parseNumericSuffix(afterSaleId, "after-sale-");
    }

    public static String formatAfterSaleId(long afterSaleId) {
        return "after-sale-" + afterSaleId;
    }

    public static long parseConversationId(String conversationId) {
        return parseNumericSuffix(conversationId, "conversation-");
    }

    public static String formatConversationId(long conversationId) {
        return "conversation-" + conversationId;
    }

    public static long parseConversationMessageId(String messageId) {
        return parseNumericSuffix(messageId, "message-");
    }

    public static String formatConversationMessageId(long messageId) {
        return "message-" + messageId;
    }

    public static long parseFaqId(String faqId) {
        return parseNumericSuffix(faqId, "faq-");
    }

    public static String formatFaqId(long faqId) {
        return "faq-" + faqId;
    }

    public static long parseLivePlanId(String livePlanId) {
        return parseNumericSuffix(livePlanId, "live-plan-");
    }

    public static String formatLivePlanId(long livePlanId) {
        return "live-plan-" + livePlanId;
    }

    public static long parseLiveScriptId(String liveScriptId) {
        return parseNumericSuffix(liveScriptId, "live-script-");
    }

    public static String formatLiveScriptId(long liveScriptId) {
        return "live-script-" + liveScriptId;
    }

    public static long parseLiveSessionId(String liveSessionId) {
        return parseNumericSuffix(liveSessionId, "live-session-");
    }

    public static String formatLiveSessionId(long liveSessionId) {
        return "live-session-" + liveSessionId;
    }

    public static long parseLiveProductItemId(String liveProductItemId) {
        return parseNumericSuffix(liveProductItemId, "live-product-item-");
    }

    public static String formatLiveProductItemId(long liveProductItemId) {
        return "live-product-item-" + liveProductItemId;
    }

    public static long parseCampaignId(String campaignId) {
        return parseNumericSuffix(campaignId, "campaign-");
    }

    public static String formatCampaignId(long campaignId) {
        return "campaign-" + campaignId;
    }

    public static long parseCouponTemplateId(String couponTemplateId) {
        return parseNumericSuffix(couponTemplateId, "coupon-template-");
    }

    public static String formatCouponTemplateId(long couponTemplateId) {
        return "coupon-template-" + couponTemplateId;
    }

    public static long parseFinanceBillId(String financeBillId) {
        return parseNumericSuffix(financeBillId, "finance-bill-");
    }

    public static String formatFinanceBillId(long financeBillId) {
        return "finance-bill-" + financeBillId;
    }

    public static long parseSettlementRecordId(String settlementRecordId) {
        return parseNumericSuffix(settlementRecordId, "settlement-record-");
    }

    public static String formatSettlementRecordId(long settlementRecordId) {
        return "settlement-record-" + settlementRecordId;
    }

    public static long parsePluginAppId(String pluginAppId) {
        return parseNumericSuffix(pluginAppId, "app-");
    }

    public static String formatPluginAppId(long pluginAppId) {
        return "app-" + pluginAppId;
    }

    public static long parseWebhookSubscriptionId(String webhookSubscriptionId) {
        return parseNumericSuffix(webhookSubscriptionId, "webhook-");
    }

    public static String formatWebhookSubscriptionId(long webhookSubscriptionId) {
        return "webhook-" + webhookSubscriptionId;
    }

    public static long parseOpenPlatformCallLogId(String callLogId) {
        return parseNumericSuffix(callLogId, "open-log-");
    }

    public static String formatOpenPlatformCallLogId(long callLogId) {
        return "open-log-" + callLogId;
    }

    public static long parseIntegrationCredentialId(String credentialId) {
        return parseNumericSuffix(credentialId, "integration-credential-");
    }

    public static String formatIntegrationCredentialId(long credentialId) {
        return "integration-credential-" + credentialId;
    }

    private static long parseNumericSuffix(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            throw new IllegalArgumentException("非法ID格式: " + value);
        }
        return Long.parseLong(value.substring(prefix.length()));
    }
}
