package backend.channel.model;

import java.util.Map;

/**
 * 平台 OAuth 回调入参。
 */
public record ChannelCallbackRequest(
        ChannelType channelType,
        String code,
        String state,
        Map<String, String> rawParams
) {
}
