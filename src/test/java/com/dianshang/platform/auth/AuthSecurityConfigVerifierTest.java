package com.dianshang.platform.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthSecurityConfigVerifierTest {

    @Test
    void shouldAllowWeakDevelopmentDefaultsWhenExplicitSecretsNotRequired() {
        AuthSecurityConfigVerifier verifier = new AuthSecurityConfigVerifier(
                AuthSecurityConfigVerifier.DEFAULT_TOKEN_SECRET,
                AuthSecurityConfigVerifier.DEFAULT_BOOTSTRAP_PASSWORD,
                false
        );

        assertThatCode(() -> verifier.run(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWeakTokenSecretWhenExplicitSecretsRequired() {
        AuthSecurityConfigVerifier verifier = new AuthSecurityConfigVerifier(
                AuthSecurityConfigVerifier.DEFAULT_TOKEN_SECRET,
                "StrongBootstrapPassword123",
                true
        );

        assertThatThrownBy(verifier::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_AUTH_TOKEN_SECRET");
    }

    @Test
    void shouldRejectWeakBootstrapPasswordWhenExplicitSecretsRequired() {
        AuthSecurityConfigVerifier verifier = new AuthSecurityConfigVerifier(
                "strong-token-secret-value-for-prod-123456",
                "short123",
                true
        );

        assertThatThrownBy(verifier::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_AUTH_BOOTSTRAP_PASSWORD");
    }

    @Test
    void shouldPassWhenProductionSecretsAreConfigured() {
        AuthSecurityConfigVerifier verifier = new AuthSecurityConfigVerifier(
                "strong-token-secret-value-for-prod-123456",
                "StrongBootstrapPassword123",
                true
        );

        assertThatCode(verifier::verifyOrThrow).doesNotThrowAnyException();
    }
}
