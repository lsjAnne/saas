package backend.channel.model;

import java.util.Map;

/**
 * 发货指令（推送到平台）。
 */
public record ChannelShipRequest(
        String tenantId,
        String organizationId,
        String channelAccountId,
        String storeId,
        String platformOrderId,
        String logisticsCompany,
        String logisticsNo,
        Map<String, Object> extra
) {
}
