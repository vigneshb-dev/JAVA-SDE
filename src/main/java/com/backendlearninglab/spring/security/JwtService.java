package com.backendlearninglab.spring.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service managing RFC 7519 JSON Web Token (JWT) generation, parsing, and cryptographic verification.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li><b>Signing Key:</b> A minimum 256-bit secret key used by HMAC-SHA256 (HS256) to create the signature.</li>
 *   <li><b>Claims:</b> Custom key-value attributes stored inside the token payload (e.g. userId, role).</li>
 *   <li><b>Stateless Expiration:</b> Tokens are self-contained. The expiration time is encoded directly in the payload
 *       and checked mathematically, requiring NO database lookup.</li>
 * </ul>
 */
@Service
public class JwtService {

    // 256-bit secret key (In production, load via environment variable or secret manager)
    private static final String SECRET_STRING = "OrderFlowSuperSecureSecretKeyThatIsAtLeast256BitsLong2026!";
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    // Token valid for 24 hours
    private static final Duration JWT_EXPIRATION = Duration.ofHours(24);

    public String generateToken(UserDetails userDetails, Long userId, String role) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("role", role);
        return buildToken(extraClaims, userDetails.getUsername(), JWT_EXPIRATION);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, Duration expiration) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256) // Digitally sign with HMAC-SHA256
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        // Parses and cryptographically verifies signature using signingKey
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
