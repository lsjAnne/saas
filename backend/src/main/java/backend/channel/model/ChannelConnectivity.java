package backend.channel.model;

import java.time.OffsetDateTime;

/**
 * 渠道连通性检查结果。
 */
public record ChannelConnectivity(
        boolean reachable,
        String message,
        OffsetDateTime checkedAt
) {
}
