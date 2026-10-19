package backend.live.barrage.infrastructure.adapter;

import backend.live.barrage.infrastructure.config.BarrageConnectorProperties;
import org.springframework.stereotype.Component;

/**
 * 快手直播间弹幕连接器。
 *
 * <p>真实模式（mock-mode=false）接入要点（待真实凭证联调）：
 * <ul>
 *   <li>快手开放平台「直播数据-弹幕/评论」开放能力（需绑定主播账号并完成应用审核）；</li>
 *   <li>使用 {@code app-key/app-secret} 获取授权后，通过开放平台消息推送（WebSocket/回调）接收直播间评论；</li>
 *   <li>解析评论内容（user_name/comment_text）后回调 sink；直播间关闭或掉线需按平台重连策略恢复。</li>
 * </ul>
 */
@Component
public class KuaishouBarrageConnector extends AbstractPlatformBarrageConnector {

    public KuaishouBarrageConnector(BarrageConnectorProperties properties) {
        super(properties);
    }

    @Override
    public String platformType() {
        return "kuaishou";
    }

    @Override
    protected String realConnectDoc() {
        return "快手开放平台直播弹幕开放能力（主播授权 + 消息推送订阅）";
    }
}
