package backend.live.barrage.application;

import backend.common.exception.BusinessException;
import backend.digitalhuman.application.DigitalHumanInteractionService;
import backend.digitalhuman.application.DigitalHumanInteractionService.InteractionReplyView;
import backend.digitalhuman.application.DigitalHumanInteractionService.SubmitBarrageRequest;
import backend.live.barrage.infrastructure.DouyinCallbackSignatureVerifier;
import backend.live.barrage.infrastructure.DouyinCloudPushGateway;
import backend.live.barrage.infrastructure.config.BarrageConnectorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抖音直播间数据回调服务（弹幕玩法·内网专线推送模式）。
 *
 * <p>平台通过开放平台配置的路径（默认 {@code /live_room/callback}）推送直播间数据：
 * <ul>
 *   <li>评论 live_comment → 逐条进入现有数字人问答闭环（按商品/问题类型作答、未命中进学习记忆），
 *       回答结果随指令推送到抖音云 WebSocket 网关（由网关下发到玩法客户端）；</li>
 *   <li>礼物 live_gift / 点赞 live_like / 粉丝团 fansclub → 仅计数统计（不进入问答，规避重复打扰）；</li>
 * </ul>
 * 来源校验：抖音云内网专线推送请求头 {@code x-tt-source} 含 {@code internal-callback} 免验签；
 * 公网推送按开放平台签名算法（MD5+Base64）验签。平台推送消息可能重复，按 {@code msg_id} 幂等去重。</p>
 *
 * <p>TODO(联调-真实凭证): 评论归属租户/组织目前取配置项
 * {@code app.live.barrage.connectors.douyin.tenant-id / organization-id}（回调不带业务租户标识）；
 * 多租户场景可扩展为 room_id → 组织映射注册表（见 docs/barrage-connect.md）。</p>
 */
@Service
public class DouyinLiveCallbackService {

    private static final Logger log = LoggerFactory.getLogger(DouyinLiveCallbackService.class);

    /** 幂等去重容量上限：超过后清空重建（平台消息重复窗口远小于此量级） */
    private static final int DEDUP_CAPACITY = 5000;

    private final BarrageConnectorProperties properties;
    private final DigitalHumanInteractionService interactionService;
    private final DouyinCloudPushGateway pushGateway;
    private final ObjectMapper objectMapper;

    /** msg_id 去重集合 */
    private final ConcurrentHashMap<String, Boolean> seenMsgIds = new ConcurrentHashMap<>();

    public DouyinLiveCallbackService(BarrageConnectorProperties properties,
                                     DigitalHumanInteractionService interactionService,
                                     DouyinCloudPushGateway pushGateway,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.interactionService = interactionService;
        this.pushGateway = pushGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理一次平台推送。
     *
     * @param roomId   直播间 id（header x-roomid）
     * @param msgType  消息类型（header x-msg-type：live_comment / live_gift / live_like / fansclub）
     * @param xTtSource 请求头 x-tt-source（内网专线标记 internal-callback）
     * @param headers  全部请求头（公网验签使用 x-nonce-str/x-timestamp/x-signature 等）
     * @param body     推送 body 原文（payload 数组 json）
     * @return 处理统计结果
     */
    public CallbackResult handle(String roomId, String msgType, String xTtSource,
                                 Map<String, String> headers, String body) {
        BarrageConnectorProperties.ConnectorSettings douyin = douyinSettings();
        // 1. 来源校验：内网专线免验签；否则按开放平台签名算法验签
        if (DouyinCallbackSignatureVerifier.isInternalCallback(xTtSource)) {
            log.debug("[douyin-callback] 内网专线推送 roomId={} msgType={}", roomId, msgType);
        } else if (!DouyinCallbackSignatureVerifier.verify(
                headers, body, douyin.getCallbackSecret(), headers.get("x-signature"))) {
            throw new BusinessException("2401", "douyin callback signature invalid", HttpStatus.FORBIDDEN);
        }

        // 2. 解析 payload（平台推送为对象数组）
        List<JsonNode> payloads;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (!node.isArray()) {
                throw new IllegalArgumentException("payload must be array");
            }
            List<JsonNode> items = new java.util.ArrayList<>();
            node.forEach(items::add);
            payloads = items;
        } catch (Exception e) {
            throw new BusinessException("2402", "douyin callback body unreadable", HttpStatus.BAD_REQUEST);
        }

        CallbackResult.Builder result = CallbackResult.builder(roomId, msgType);
        if (payloads.isEmpty()) {
            return result.build();
        }

        // 3. 按消息类型分发
        for (JsonNode item : payloads) {
            String msgId = item.path("msg_id").asText("");
            if (msgId.isBlank() || !dedup(msgId)) {
                result.skippedDuplicate();
                continue;
            }
            switch (msgType == null ? "" : msgType) {
                case "live_comment" -> handleComment(douyin, roomId, item, result);
                case "live_gift" -> result.gift();
                case "live_like" -> result.like();
                case "fansclub" -> result.fans();
                default -> log.warn("[douyin-callback] 未知消息类型 msgType={}", msgType);
            }
        }
        return result.build();
    }

    /**
     * 玩法客户端启动：返回直播间信息并示意开启数据推送任务。
     * 抖音云模式下客户端经 SDK 请求，后端可直接在请求头拿到直播间信息（免 token 置换）。
     *
     * <p>TODO(联调-开启推送任务): 按官方「启动任务」OpenAPI 调用开启（roomid, appid, msg_type 三维），
     * 并调用「置顶礼物」接口；本方法当前返回配置的 room-id，供开发联调占位。</p>
     */
    public StartGameResult startGame(String roomIdFromHeader) {
        BarrageConnectorProperties.ConnectorSettings douyin = douyinSettings();
        String roomId = roomIdFromHeader != null && !roomIdFromHeader.isBlank()
                ? roomIdFromHeader
                : douyin.getRoomId();
        return new StartGameResult(
                roomId,
                "ok",
                "弹幕玩法已就绪：数据推送任务开启与礼物置顶见 TODO(联调-开启推送任务)"
        );
    }

    // ---------- 私有 ----------

    private void handleComment(BarrageConnectorProperties.ConnectorSettings douyin,
                               String roomId, JsonNode item, CallbackResult.Builder result) {
        String content = item.path("content").asText("").trim();
        if (content.isBlank()) {
            result.skippedBlank();
            return;
        }
        String tenantId = douyin.getTenantId();
        String organizationId = douyin.getOrganizationId();
        if (tenantId.isBlank() || organizationId.isBlank()) {
            result.skippedNoTenantBinding();
            log.warn("[douyin-callback] 未配置租户/组织映射（tenant-id/organization-id），丢弃评论 roomId={} content={}", roomId, content);
            return;
        }
        String nickname = item.path("nickname").asText("观众");
        try {
            InteractionReplyView reply = interactionService.submitBarrage(tenantId, new SubmitBarrageRequest(
                    organizationId,
                    roomId,              // 直播间 id 作为会话维度，前端按 sessionId=roomId 查询问答记录
                    "douyin",
                    nickname,
                    content
            ));
            result.commentSubmitted();
            // 回答结果随指令推送到抖音云 WebSocket 网关 → 玩法客户端上屏
            String answer = reply.interaction().answer();
            pushGateway.push(roomId, instructionPayload(item, answer));
        } catch (Exception e) {
            result.commentFailed();
            log.warn("[douyin-callback] 评论问答处理失败 roomId={} content={} err={}", roomId, content, e.getMessage());
        }
    }

    /** 构造下发到玩法客户端的指令 JSON（问答答案随指令上屏） */
    private String instructionPayload(JsonNode comment, String answer) {
        try {
            return objectMapper.createObjectNode()
                    .put("type", "qa_reply")
                    .put("room_id", comment.path("room_id").asText(""))
                    .put("user_nick", comment.path("nickname").asText(""))
                    .put("question", comment.path("content").asText(""))
                    .put("answer", answer)
                    .toString();
        } catch (Exception e) {
            return "{\"type\":\"qa_reply\",\"answer\":\"" + answer.replace("\"", "'") + "\"}";
        }
    }

    /** msg_id 幂等去重 */
    private boolean dedup(String msgId) {
        if (seenMsgIds.size() >= DEDUP_CAPACITY) {
            seenMsgIds.clear();
        }
        return seenMsgIds.putIfAbsent(msgId, Boolean.TRUE) == null;
    }

    private BarrageConnectorProperties.ConnectorSettings douyinSettings() {
        BarrageConnectorProperties.ConnectorSettings settings = properties.getConnectors().get("douyin");
        return settings != null ? settings : new BarrageConnectorProperties.ConnectorSettings();
    }

    // ---------- 结果视图 ----------

    public record CallbackResult(
            String roomId,
            String msgType,
            int received,
            int commentSubmitted,
            int commentFailed,
            int giftCount,
            int likeCount,
            int fansCount,
            int skippedDuplicate,
            int skippedBlank,
            int skippedNoTenantBinding
    ) {
        public static Builder builder(String roomId, String msgType) {
            return new Builder(roomId, msgType);
        }

        public static class Builder {
            private final String roomId;
            private final String msgType;
            private int received;
            private int commentSubmitted;
            private int commentFailed;
            private int giftCount;
            private int likeCount;
            private int fansCount;
            private int skippedDuplicate;
            private int skippedBlank;
            private int skippedNoTenantBinding;

            Builder(String roomId, String msgType) {
                this.roomId = roomId;
                this.msgType = msgType;
            }

            public void commentSubmitted() {
                received++;
                commentSubmitted++;
            }

            public void commentFailed() {
                received++;
                commentFailed++;
            }

            public void gift() {
                received++;
                giftCount++;
            }

            public void like() {
                received++;
                likeCount++;
            }

            public void fans() {
                received++;
                fansCount++;
            }

            public void skippedDuplicate() {
                skippedDuplicate++;
            }

            public void skippedBlank() {
                skippedBlank++;
            }

            public void skippedNoTenantBinding() {
                skippedNoTenantBinding++;
            }

            public CallbackResult build() {
                return new CallbackResult(roomId, msgType, received, commentSubmitted, commentFailed,
                        giftCount, likeCount, fansCount, skippedDuplicate, skippedBlank, skippedNoTenantBinding);
            }
        }
    }

    public record StartGameResult(String roomId, String status, String message) {
    }
}
