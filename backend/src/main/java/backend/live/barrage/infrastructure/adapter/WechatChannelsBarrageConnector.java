package backend.live.barrage.infrastructure.adapter;

import backend.live.barrage.infrastructure.config.BarrageConnectorProperties;
import org.springframework.stereotype.Component;

/**
 * 微信视频号直播间弹幕连接器。
 *
 * <p>真实模式（mock-mode=false）接入要点（待真实凭证联调）：
 * <ul>
 *   <li>视频号直播开放能力（微信视频号助手 / 直播数据服务，需商家主体授权）；</li>
 *   <li>使用 {@code app-key/app-secret} 换取授权后订阅直播间评论事件；</li>
 *   <li>解析评论（nickname/content）后回调 sink；直播间场次切换需按场次重新订阅。</li>
 * </ul>
 */
@Component
public class WechatChannelsBarrageConnector extends AbstractPlatformBarrageConnector {

    public WechatChannelsBarrageConnector(BarrageConnectorProperties properties) {
        super(properties);
    }

    @Override
    public String platformType() {
        return "wechat_channels";
    }

    @Override
    protected String realConnectDoc() {
        return "视频号直播开放能力评论订阅（商家主体授权 + 场次评论事件）";
    }
}
