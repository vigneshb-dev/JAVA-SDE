package com.backendlearninglab.examples.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * EDUCATIONAL DEMO: Manual RFC 7519 JSON Web Token (JWT) Anatomy.
 * <p>
 * Demonstrates:
 * 1. How a JWT is composed of Header, Payload, and Signature.
 * 2. Why JWT is NOT encrypted by default (Claims are readable by anyone).
 * 3. How HMAC-SHA256 signature guarantees cryptographic tamper-proofing.
 */
public class ManualJwtDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== MANUAL JWT CONSTRUCTION & TAMPER VERIFICATION ===");

        String secretKey = "SuperSecretBackendLearningLabEncryptionKey2026!";

        // 1. Construct Header & Payload JSON
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"alice.smith@example.com\",\"role\":\"USER\",\"exp\":1794000000}";

        // 2. Base64URL Encode Header & Payload
        Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        String encodedHeader = urlEncoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = urlEncoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // 3. Compute Cryptographic Signature: HMAC-SHA256(header + "." + payload, secretKey)
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = computeHmacSha256(signingInput, secretKey);

        // 4. Assemble the complete Compact JWT
        String jwtToken = signingInput + "." + signature;
        System.out.println("\n[1] Generated Complete JWT Token:");
        System.out.println("  " + jwtToken);

        // 5. Prove that JWT is NOT encrypted: Anyone can decode the payload!
        System.out.println("\n[2] Proof that JWT is NOT Encrypted (Claims are Public):");
        String[] parts = jwtToken.split("\\.");
        String decodedPayload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        System.out.println("  Decoded Payload JSON: " + decodedPayload);
        System.out.println("  -> CAUTION: Never put sensitive data (passwords, credit card numbers) in a JWT!");

        // 6. Tampering Demonstration: Attacker changes role from USER to ADMIN
        System.out.println("\n[3] Tamper Detection in Action:");
        String tamperedPayloadJson = "{\"sub\":\"alice.smith@example.com\",\"role\":\"ADMIN\",\"exp\":1794000000}";
        String tamperedEncodedPayload = urlEncoder.encodeToString(tamperedPayloadJson.getBytes(StandardCharsets.UTF_8));
        String tamperedToken = encodedHeader + "." + tamperedEncodedPayload + "." + signature;

        System.out.println("  Original Token Valid? " + verifyToken(jwtToken, secretKey));
        System.out.println("  Tampered Token Valid? " + verifyToken(tamperedToken, secretKey) + " (REJECTED by signature mismatch!)");
    }

    private static String computeHmacSha256(String data, String secret) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] rawHmac = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }

    private static boolean verifyToken(String token, String secret) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = computeHmacSha256(signingInput, secret);

            // Constant-time comparison prevents timing attacks!
            return MessageDigestEqual(parts[2], expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean MessageDigestEqual(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
