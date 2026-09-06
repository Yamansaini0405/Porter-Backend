package com.porterclone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and validates JWTs. Stateless — no server-side session.
 *
 * Two distinct token types are issued, distinguished by the "type" claim:
 *  - ACCESS: normal, full-privilege token for an existing user (subject = userId, carries "role").
 *  - REGISTRATION: short-lived token issued right after OTP verification for a brand-new phone
 *    number, before an account exists yet (subject = phone, carries role="PENDING"). It only grants
 *    access to POST /api/v1/auth/profile — see SecurityConfig — and cannot be used anywhere else
 *    because it has no userId/real role.
 *
 * Refresh tokens (for renewing ACCESS tokens) are handled separately (hashed + stored in DB) so
 * they can be revoked.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_PHONE = "phone";

    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REGISTRATION = "REGISTRATION";
    public static final String ROLE_PENDING = "PENDING";

    private static final long REGISTRATION_TOKEN_EXPIRY_MS = Duration.ofMinutes(15).toMillis();

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    /** Full access token for a known, existing user. */
    public String generateAccessToken(Long userId, String role) {
        return buildToken(
                String.valueOf(userId),
                Map.of(CLAIM_ROLE, role, CLAIM_TYPE, TYPE_ACCESS),
                accessTokenExpiryMs
        );
    }

    /**
     * Short-lived token proving "this phone number just verified its OTP" for a phone that has
     * no account yet. Only usable against POST /api/v1/auth/profile.
     */
    public String generateRegistrationToken(String phone) {
        return buildToken(
                phone,
                Map.of(CLAIM_ROLE, ROLE_PENDING, CLAIM_TYPE, TYPE_REGISTRATION, CLAIM_PHONE, phone),
                REGISTRATION_TOKEN_EXPIRY_MS
        );
    }

    private String buildToken(String subject, Map<String, Object> claims, long expiryMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractClaim(token, Claims::getSubject));
    }

    public String extractPhone(String token) {
        return extractAllClaims(token).get(CLAIM_PHONE, String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get(CLAIM_ROLE, String.class);
    }

    public String extractType(String token) {
        return extractAllClaims(token).get(CLAIM_TYPE, String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
    }
}