package backend.channel.model;

import backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

/**
 * 渠道类型枚举，恰好 6 个平台。code() 为对外持久化/传输的稳定标识。
 */
public enum ChannelType {

    PDD("pdd"),
    TAOBAO("taobao"),
    DOUYIN("douyin"),
    WECHAT_CHANNELS("wechat_channels"),
    KUAISHOU("kuaishou"),
    JD("jd");

    private final String code;

    ChannelType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ChannelType fromCode(String code) {
        return tryFromCode(code)
                .orElseThrow(() -> new BusinessException("8104", "unsupported channel type", HttpStatus.BAD_REQUEST));
    }

    public static Optional<ChannelType> tryFromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        for (ChannelType channelType : values()) {
            if (channelType.code.equals(code)) {
                return Optional.of(channelType);
            }
        }
        return Optional.empty();
    }
}
