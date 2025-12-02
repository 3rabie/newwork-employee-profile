package com.newwork.employee.security;

import com.newwork.employee.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long jwtExpiration;
    private final String jwtSecret;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration) {
        this.jwtSecret = jwtSecret;
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
    }

    /**
     * Validates JWT configuration on startup.
     * Logs warnings for weak default secrets.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (jwtSecret.contains("secret-key-change") || jwtSecret.contains("change-in-production") || jwtSecret.contains("test-jwt-secret")) {
            log.warn("========================================");
            log.warn("SECURITY WARNING: Using default/weak JWT secret!");
            log.warn("Set JWT_SECRET environment variable to a strong random value (min 256 bits)");
            log.warn("This is acceptable for development/testing but MUST be changed in production");
            log.warn("========================================");
        } else if (jwtSecret.length() < 32) {
            log.warn("WARNING: JWT secret is shorter than recommended 256 bits (32 characters)");
        }

        log.info("JWT authentication configured with expiration: {}ms", jwtExpiration);
    }

    /**
     * Generate JWT token for a user
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("employeeId", user.getEmployeeId())
                .claim("role", user.getRole().name())
                .claim("managerId", user.getManager() != null ? user.getManager().getId().toString() : null)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID from JWT token
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    /**
     * Extract email from JWT token
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String getEmployeeIdFromToken(String token) {
        return getClaims(token).get("employeeId", String.class);
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getManagerIdFromToken(String token) {
        return getClaims(token).get("managerId", String.class);
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
