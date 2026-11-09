package com.eventhub.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Enterprise-grade cryptographic password utility using PBKDF2 with HMAC-SHA512.
 * Uses 120,000 iterations, 16-byte random salt, and 512-bit key length.
 * Verifications use constant-time byte comparison to eliminate side-channel timing attacks.
 */
@Component
public class PasswordHasher {

    private static final Logger log = LoggerFactory.getLogger(PasswordHasher.class);

    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 512;
    private static final int SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a secure random 16-byte cryptographic salt.
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Computes a PBKDF2-HMAC-SHA512 hash and encodes as "iterations:saltBase64:hashBase64".
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a raw plaintext password against a stored hashed password string.
     */
    public boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            log.warn("[SECURITY] Stored hash has invalid token format");
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            byte[] actualHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception ex) {
            log.error("[SECURITY] Password verification failed: {}", ex.getMessage());
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Cryptographic algorithm unavailable: " + ALGORITHM, ex);
        }
    }
}
