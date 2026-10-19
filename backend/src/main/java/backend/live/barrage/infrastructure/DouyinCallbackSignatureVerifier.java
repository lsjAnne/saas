package backend.live.barrage.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 抖音直播间数据推送签名校验（开放平台「数据开放说明」协议）。
 *
 * <p>算法（与官方 java 示例一致）：将 header 中参与签名的键（x-nonce-str、x-timestamp、
 * x-roomid、x-msg-type，不含 x-signature / content-type / x-tt-source）按 key 字典序排序，
 * 以 {@code key1=value1&key2=value2} 连接后直接拼接 body 字符串与 secret，对拼接结果做
 * MD5（16 字节）再 Base64 编码，得到 signature。</p>
 */
public final class DouyinCallbackSignatureVerifier {

    private DouyinCallbackSignatureVerifier() {
    }

    /** 参与签名计算的 header 键（字典序排序由调用方统一处理） */
    private static final List<String> SIGN_KEYS = List.of(
            "x-nonce-str", "x-timestamp", "x-roomid", "x-msg-type"
    );

    /**
     * 计算签名。
     *
     * @param headers 平台推送的请求头（须已包含 x-nonce-str / x-timestamp / x-roomid / x-msg-type）
     * @param body    推送 body 原串（payload json）
     * @param secret  签名密钥（推送配置中的 secret）
     * @return base64(md5(rawData))
     */
    public static String sign(Map<String, String> headers, String body, String secret) {
        List<String> keys = new ArrayList<>(SIGN_KEYS);
        keys.sort(Comparator.naturalOrder());
        StringBuilder kv = new StringBuilder();
        for (String key : keys) {
            String value = headers.getOrDefault(key, "");
            if (!kv.isEmpty()) {
                kv.append('&');
            }
            kv.append(key).append('=').append(value);
        }
        String rawData = kv + body + secret;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("signature compute failed", e);
        }
    }

    /**
     * 校验签名：期望值等于计算值。
     */
    public static boolean verify(Map<String, String> headers, String body, String secret, String signature) {
        if (signature == null || signature.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        return signature.equals(sign(headers, body, secret));
    }

    /**
     * 是否为抖音内网专线推送（请求头 x-tt-source 含 internal-callback）。
     * 内网专线免验签，仅需校验来源标记。
     */
    public static boolean isInternalCallback(String xTtSource) {
        return xTtSource != null && xTtSource.contains("internal-callback");
    }
}
