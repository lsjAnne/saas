package backend.channel.application;

import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道适配器注册表。Spring 自动收集所有 ChannelClient Bean（当前阶段为空列表也可正常启动）。
 */
@Component
public class ChannelClientRegistry {

    private final Map<ChannelType, ChannelClient> clients = new EnumMap<>(ChannelType.class);

    public ChannelClientRegistry(List<ChannelClient> clients) {
        for (ChannelClient client : clients) {
            this.clients.put(client.channelType(), client);
        }
    }

    public ChannelClient resolve(ChannelType channelType) {
        ChannelClient client = clients.get(channelType);
        if (client == null) {
            throw new BusinessException(
                    "8101",
                    "unsupported channel type: " + channelType.code(),
                    HttpStatus.BAD_REQUEST
            );
        }
        return client;
    }

    public List<ChannelType> supportedTypes() {
        return List.copyOf(clients.keySet());
    }

    public boolean supports(ChannelType channelType) {
        return clients.containsKey(channelType);
    }
}
