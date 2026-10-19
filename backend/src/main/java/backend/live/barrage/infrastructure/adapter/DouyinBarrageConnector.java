package backend.live.barrage.infrastructure.adapter;

import backend.live.barrage.infrastructure.config.BarrageConnectorProperties;
import org.springframework.stereotype.Component;

/**
 * 抖音直播间弹幕连接器。
 *
 * <p>真实模式（mock-mode=false）接入要点（待真实凭证联调）：
 * <ul>
 *   <li>通过抖音开放平台「直播-互动开放能力」申请权限（需主播授权，scope 含 live 能力）；</li>
 *   <li>使用 {@code app-key/app-secret} 换取 access_token 后，连接直播互动 WebSocket 服务；</li>
 *   <li>订阅评论消息（room_id 对应的直播间观众评论），消息体按平台协议解析出 user_nick/content 后回调 sink；</li>
 *   <li>需保活心跳与断线重连（直播间切换 room_id 需重新建立连接）。</li>
 * </ul>
 */
@Component
public class DouyinBarrageConnector extends AbstractPlatformBarrageConnector {

    public DouyinBarrageConnector(BarrageConnectorProperties properties) {
        super(properties);
    }

    @Override
    public String platformType() {
        return "douyin";
    }

    @Override
    protected String realConnectDoc() {
        return "抖音开放平台直播互动能力 WebSocket（主播授权 + access_token + 评论订阅）";
    }
}
