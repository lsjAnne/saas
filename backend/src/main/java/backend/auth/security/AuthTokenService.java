package backend.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthTokenService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;
    private final long expireSeconds;

    public AuthTokenService(ObjectMapper objectMapper,
                            @Value("${app.auth.token-secret:${APP_AUTH_TOKEN_SECRET:local-dev-auth-token-secret-change-me}}") String tokenSecret,
                            @Value("${app.auth.token-expire-seconds:${APP_AUTH_TOKEN_EXPIRE_SECONDS:43200}}") long expireSeconds) {
        this.objectMapper = objectMapper;
        this.signingKey = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.expireSeconds = expireSeconds;
    }

    public String issue(String userId) {
        try {
            long issuedAt = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond();
            long expiredAt = issuedAt + expireSeconds;
            String header = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            )));
            String payload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of(
                    "sub", userId,
                    "iat", issuedAt,
                    "exp", expiredAt
            )));
            String signature = sign(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("issue auth token failed", exception);
        }
    }

    public Optional<String> parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String content = parts[0] + "." + parts[1];
            if (!constantTimeEquals(parts[2], sign(content))) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), MAP_TYPE);
            long expiredAt = ((Number) payload.get("exp")).longValue();
            if (expiredAt <= OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()) {
                return Optional.empty();
            }
            Object subject = payload.get("sub");
            return subject == null ? Optional.empty() : Optional.of(subject.toString());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int diff = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            diff |= leftBytes[index] ^ rightBytes[index];
        }
        return diff == 0;
    }
}

