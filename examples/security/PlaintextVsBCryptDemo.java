package com.backendlearninglab.examples.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * EDUCATIONAL DEMO: Password Storage Security Evolution.
 * <p>
 * This class demonstrates:
 * 1. Plaintext storage (Catastrophic vulnerability).
 * 2. Unsalted Fast Hash (MD5 / SHA-256) (Vulnerable to GPU brute-force and rainbow tables).
 * 3. BCrypt Key Derivation Function (The industry standard with adaptive work factor).
 */
public class PlaintextVsBCryptDemo {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println("=== PASSWORD HASHING: PLAINTEXT VS SHA-256 VS BCRYPT ===");

        String rawPassword = "CustomerSecret2026!";

        // 1. Plaintext
        demonstratePlaintextVulnerability(rawPassword);

        // 2. Unsalted SHA-256
        demonstrateFastHashVulnerability(rawPassword);

        // 3. BCrypt (Adaptive Cost + Embedded Salt)
        demonstrateBCryptSecurity(rawPassword);
    }

    private static void demonstratePlaintextVulnerability(String password) {
        System.out.println("\n[1] Plaintext Storage (CATASTROPHIC):");
        System.out.println("  Database column stores: \"" + password + "\"");
        System.out.println("  DANGER: A database leak or SQL injection exposes customer credentials instantly!");
    }

    private static void demonstrateFastHashVulnerability(String password) throws NoSuchAlgorithmException {
        System.out.println("\n[2] Fast Unsalted SHA-256 (FLAWED):");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        System.out.println("  SHA-256 Output: " + hexString);
        System.out.println("  DANGER: SHA-256 is designed to be FAST. Modern GPUs calculate over 100 Billion");
        System.out.println("  hashes/sec. Attackers use precomputed Rainbow Tables to invert hashes in seconds.");
    }

    private static void demonstrateBCryptSecurity(String password) {
        System.out.println("\n[3] BCrypt Adaptive Work Factor (PROFESSIONAL STANDARD):");

        // Strength 12 = 2^12 (4,096) rounds of key derivation
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        long start = System.currentTimeMillis();
        String hash1 = encoder.encode(password);
        long durationMs = System.currentTimeMillis() - start;

        String hash2 = encoder.encode(password);

        System.out.println("  BCrypt Hash 1: " + hash1);
        System.out.println("  BCrypt Hash 2: " + hash2);
        System.out.println("  Hashing Time:  " + durationMs + " ms");

        /*
         * CRITICAL LESSONS:
         * 1. Notice hash1 and hash2 are COMPLETELY DIFFERENT even for identical passwords!
         *    Why? BCrypt generates a random 16-byte salt every time.
         * 2. The string format encodes: $2a$ [algorithm] $12$ [work factor] [22-char salt] [31-char hash]
         * 3. Slowness is a FEATURE: 100ms per attempt throttles offline brute-force attacks!
         */

        boolean match = encoder.matches(password, hash1);
        System.out.println("  encoder.matches(\"" + password + "\", hash1) -> " + match + " (Verified successfully!)");
    }
}
