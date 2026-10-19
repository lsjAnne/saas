package backend.channel.dto;

import java.util.List;

public record ChannelSyncResult(
        String channelType,
        String storeId,
        String syncType,
        int totalCount,
        int succeededCount,
        int reusedCount,
        List<ChannelSyncItem> failedItems
) {
}
