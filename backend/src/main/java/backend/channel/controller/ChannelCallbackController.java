package backend.channel.controller;

import backend.channel.application.ChannelAuthService;
import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 平台 OAuth 浏览器回调。无我们的登录态，因此不加任何租户权限注解，公开可访问。
 */
@RestController
public class ChannelCallbackController {

    private static final MediaType TEXT_UTF8 = MediaType.valueOf("text/plain;charset=UTF-8");

    private final ChannelAuthService channelAuthService;

    public ChannelCallbackController(ChannelAuthService channelAuthService) {
        this.channelAuthService = channelAuthService;
    }

    @GetMapping("/api/channels/callback/{channelType}")
    public ResponseEntity<String> callback(@PathVariable String channelType,
                                           @RequestParam(value = "code", required = false) String code,
                                           @RequestParam(value = "state", required = false) String state,
                                           @RequestParam(required = false) Map<String, String> rawParams) {
        ChannelType type;
        try {
            type = ChannelType.fromCode(channelType);
        } catch (BusinessException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(TEXT_UTF8)
                    .body("unsupported channel type: " + channelType);
        }
        try {
            channelAuthService.completeAuthorization(type, code, state, rawParams == null ? Map.of() : rawParams);
            return ResponseEntity.ok().contentType(TEXT_UTF8).body("授权成功，可关闭页面");
        } catch (BusinessException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(TEXT_UTF8)
                    .body("授权失败: " + exception.getMessage());
        }
    }
}
