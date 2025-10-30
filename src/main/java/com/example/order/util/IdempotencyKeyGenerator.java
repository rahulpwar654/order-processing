package com.example.order.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for generating idempotency keys from request objects.
 */
public class IdempotencyKeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a deterministic idempotency key from an object by computing SHA-256 hash
     * of its JSON representation.
     *
     * @param request The request object to generate key from
     * @return SHA-256 hash as hex string, or null if generation fails
     */
    public static String generateKey(Object request) {
        try {
            // Convert request to JSON for consistent representation
            String json = objectMapper.writeValueAsString(request);

            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            return HexFormat.of().formatHex(hash);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request for idempotency key generation", e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return null;
        }
    }
}

