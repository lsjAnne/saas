package backend.live.barrage.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.live.barrage.application.DouyinLiveCallbackService;
import backend.live.barrage.application.DouyinLiveCallbackService.CallbackResult;
import backend.live.barrage.application.DouyinLiveCallbackService.StartGameResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 抖音真实弹幕接入（直播小玩法·内网专线推送模式）公开端点。
 *
 * <p>端点不声明租户权限注解——抖音开放平台通过抖音云内网专线推送指令，请求不带业务
 * 租户 JWT；来源合法性由 {@code x-tt-source=internal-callback} 标记（内网专线免验签）
 * 或开放平台签名（公网模式）保障。路径与抖音云控制台「抖音回调」配置一致：
 * 回调名 danmu-callback、回调路径 /live_room/callback。</p>
 */
@RestController
public class DouyinLiveCallbackController {

    private final DouyinLiveCallbackService callbackService;

    public DouyinLiveCallbackController(DouyinLiveCallbackService callbackService) {
        this.callbackService = callbackService;
    }

    /**
     * 直播间数据回调（评论 / 礼物 / 点赞 / 粉丝团）。
     * 平台要求 2XX 即 ack；评论进入数字人问答闭环，回答随指令经抖音云网关下发客户端。
     */
    @PostMapping("/live_room/callback")
    public ApiResponse<CallbackResult> liveDataCallback(
            @RequestHeader(value = "x-roomid", required = false) String roomId,
            @RequestHeader(value = "x-msg-type", required = false) String msgType,
            @RequestHeader(value = "x-tt-source", required = false) String xTtSource,
            @RequestHeader(value = "x-nonce-str", required = false) String nonceStr,
            @RequestHeader(value = "x-timestamp", required = false) String timestamp,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestBody String body
    ) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-nonce-str", nonceStr == null ? "" : nonceStr);
        headers.put("x-timestamp", timestamp == null ? "" : timestamp);
        headers.put("x-roomid", roomId == null ? "" : roomId);
        headers.put("x-msg-type", msgType == null ? "" : msgType);
        headers.put("x-signature", signature == null ? "" : signature);
        CallbackResult result = callbackService.handle(
                roomId == null ? "" : roomId,
                msgType == null ? "" : msgType,
                xTtSource,
                headers,
                body
        );
        return ApiResponse.success(result, TraceIdHolder.get());
    }

    /**
     * 玩法客户端启动端点：返回直播间信息（抖音云模式经 SDK 请求头可直接获取直播间信息）。
     */
    @PostMapping("/start_game")
    public ApiResponse<StartGameResult> startGame(
            @RequestHeader(value = "x-roomid", required = false) String roomId
    ) {
        return ApiResponse.success(callbackService.startGame(roomId), TraceIdHolder.get());
    }
}
