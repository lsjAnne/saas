package com.dianshang.platform.auth;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class PasswordHashService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    public boolean matches(String password, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = derive(password.toCharArray(), salt, iterations, expected.length * 8);
        return constantTimeEquals(expected, actual);
    }

    public boolean needsRehash(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return true;
        }
        String[] parts = encodedPassword.split("\\$");
        return parts.length != 4 || !"pbkdf2".equals(parts[0]) || !String.valueOf(ITERATIONS).equals(parts[1]);
    }

    public boolean isStrongPassword(String password) {
        if (password == null || password.isBlank() || password.length() < 12) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char ch : password.toCharArray()) {
            if (Character.isLetter(ch)) {
                hasLetter = true;
            }
            if (Character.isDigit(ch)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now();
    }

    private byte[] derive(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("password hash derive failed", exception);
        }
    }

    private boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }
        int diff = 0;
        for (int index = 0; index < expected.length; index++) {
            diff |= expected[index] ^ actual[index];
        }
        return diff == 0;
    }
}
