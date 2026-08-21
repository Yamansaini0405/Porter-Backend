package com.porterclone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and validates JWT access tokens. Stateless — no server-side session.
 * Refresh tokens are handled separately (hashed + stored in DB) so they can be revoked.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("role", role))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractClaim(token, Claims::getSubject));
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
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
