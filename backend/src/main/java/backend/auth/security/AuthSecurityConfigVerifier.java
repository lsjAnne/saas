package backend.auth.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AuthSecurityConfigVerifier implements ApplicationRunner {

    static final String DEFAULT_TOKEN_SECRET = "local-dev-auth-token-secret-change-me";
    static final String DEFAULT_BOOTSTRAP_PASSWORD = "123456";
    private static final Logger log = LoggerFactory.getLogger(AuthSecurityConfigVerifier.class);
    private static final int MIN_TOKEN_SECRET_LENGTH = 32;
    private static final int MIN_BOOTSTRAP_PASSWORD_LENGTH = 12;

    private final String tokenSecret;
    private final String bootstrapPassword;
    private final boolean requireExplicitSecrets;
    private final boolean legacyHeaderContextEnabled;

    public AuthSecurityConfigVerifier(
            @Value("${app.auth.token-secret:${APP_AUTH_TOKEN_SECRET:" + DEFAULT_TOKEN_SECRET + "}}") String tokenSecret,
            @Value("${app.auth.bootstrap-password:${APP_AUTH_BOOTSTRAP_PASSWORD:" + DEFAULT_BOOTSTRAP_PASSWORD + "}}") String bootstrapPassword,
            @Value("${app.auth.require-explicit-secrets:false}") boolean requireExplicitSecrets,
            @Value("${app.auth.allow-legacy-header-context:${APP_AUTH_ALLOW_LEGACY_HEADER_CONTEXT:false}}") boolean legacyHeaderContextEnabled) {
        this.tokenSecret = tokenSecret;
        this.bootstrapPassword = bootstrapPassword;
        this.requireExplicitSecrets = requireExplicitSecrets;
        this.legacyHeaderContextEnabled = legacyHeaderContextEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (requireExplicitSecrets) {
            verifyOrThrow();
            return;
        }
        warnIfWeak();
    }

    void verifyOrThrow() {
        if (isWeakTokenSecret(tokenSecret)) {
            throw new IllegalStateException(
                    "auth token secret is insecure; set app.auth.token-secret or APP_AUTH_TOKEN_SECRET to a non-default value with length >= "
                            + MIN_TOKEN_SECRET_LENGTH);
        }
        if (isWeakBootstrapPassword(bootstrapPassword)) {
            throw new IllegalStateException(
                    "auth bootstrap password is insecure; set app.auth.bootstrap-password or APP_AUTH_BOOTSTRAP_PASSWORD to a non-default value with length >= "
                            + MIN_BOOTSTRAP_PASSWORD_LENGTH);
        }
    }

    public boolean isTokenSecretStrong() {
        return !isWeakTokenSecret(tokenSecret);
    }

    public boolean isBootstrapPasswordStrong() {
        return !isWeakBootstrapPassword(bootstrapPassword);
    }

    public boolean requireExplicitSecrets() {
        return requireExplicitSecrets;
    }

    public boolean isLegacyHeaderContextEnabled() {
        return legacyHeaderContextEnabled;
    }

    private void warnIfWeak() {
        if (isWeakTokenSecret(tokenSecret)) {
            log.warn(
                    "Auth token secret is using an insecure development value. Set app.auth.token-secret or APP_AUTH_TOKEN_SECRET before enabling production-style deployment.");
        }
        if (isWeakBootstrapPassword(bootstrapPassword)) {
            log.warn(
                    "Auth bootstrap password is using an insecure development value. Set app.auth.bootstrap-password or APP_AUTH_BOOTSTRAP_PASSWORD before enabling production-style deployment.");
        }
    }

    private boolean isWeakTokenSecret(String value) {
        return value == null
                || value.isBlank()
                || DEFAULT_TOKEN_SECRET.equals(value)
                || value.length() < MIN_TOKEN_SECRET_LENGTH;
    }

    private boolean isWeakBootstrapPassword(String value) {
        return value == null
                || value.isBlank()
                || DEFAULT_BOOTSTRAP_PASSWORD.equals(value)
                || value.length() < MIN_BOOTSTRAP_PASSWORD_LENGTH;
    }
}

