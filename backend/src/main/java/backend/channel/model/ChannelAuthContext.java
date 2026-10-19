package backend.channel.model;

import java.util.Map;

/**
 * 渠道调用上下文：一次网关调用所需的租户/组织/店铺与凭证信息，由适配器使用。
 */
public record ChannelAuthContext(
        String tenantId,
        String organizationId,
        String channelAccountId,
        String storeId,
        String appKey,
        String appSecret,
        String redirectUri,
        Map<String, Object> extra
) {
}
