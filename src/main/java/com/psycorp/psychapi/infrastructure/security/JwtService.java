package com.psycorp.psychapi.infrastructure.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Utility class untuk generate dan validate JWT tokens.
 *
 * CATATAN:
 * - Secret dan config lainnya otomatis dibaca dari application.yml
 *   oleh SmallRye JWT Build library
 * - Method validateToken() menggunakan simplified parsing untuk use case dasar.
 *   Untuk production, gunakan JwtConsumer untuk proper validation dengan signature verification.
 */
@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "quarkus.smallrye.jwt.token-expires-in", defaultValue = "3600")
    Long tokenExpiresIn;

    @ConfigProperty(name = "quarkus.smallrye.jwt.issuer", defaultValue = "psych-api")
    String issuer;

    public String generateToken(String userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(tokenExpiresIn);

        JwtClaimsBuilder claimsBuilder = Jwt.claims()
            .issuer(issuer)
            .subject(userId)
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("email", email)
            .claim("roles", roles);

        return claimsBuilder.sign();
    }

    public String generateTokenWithContext(
            String userId, 
            String email, 
            List<String> roles,
            String organizationId,
            String organizationRole,
            String organizationName) {
        
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(tokenExpiresIn);

        JwtClaimsBuilder claimsBuilder = Jwt.claims()
            .issuer(issuer)
            .subject(userId)
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("email", email)
            .claim("roles", roles);

        // Add organization context jika ada
        if (organizationId != null && !organizationId.isEmpty()) {
            claimsBuilder.claim("organizationId", organizationId);
        }
        if (organizationRole != null && !organizationRole.isEmpty()) {
            claimsBuilder.claim("organizationRole", organizationRole);
        }
        if (organizationName != null && !organizationName.isEmpty()) {
            claimsBuilder.claim("organizationName", organizationName);
        }

        return claimsBuilder.sign();
    }

    public String generateTokenWithClaims(
            String userId, 
            String email, 
            List<String> roles,
            Map<String, Object> customClaims) {
        
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(tokenExpiresIn);

        JwtClaimsBuilder claimsBuilder = Jwt.claims()
            .issuer(issuer)
            .subject(userId)
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("email", email)
            .claim("roles", roles);

        // Add custom claims
        if (customClaims != null && !customClaims.isEmpty()) {
            for (Map.Entry<String, Object> entry : customClaims.entrySet()) {
                claimsBuilder.claim(entry.getKey(), entry.getValue());
            }
        }

        return claimsBuilder.sign();
    }

    public String generateTokenWithExpiry(
            String userId, 
            String email, 
            List<String> roles,
            Long expiresInSeconds) {
        
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expiresInSeconds);

        return Jwt.claims()
            .issuer(issuer)
            .subject(userId)
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("email", email)
            .claim("roles", roles)
            .sign();
    }

    /**
     * Extract claims dari JWT token.
     *
     * @param token JWT token string
     * @return Map of claims dari token
     * @throws ValidationException jika token invalid atau format salah
     */
    public Map<String, Object> extractClaims(String token) {
        if (token == null || token.isEmpty()) {
            throw new ValidationException("INVALID_TOKEN", "Token cannot be null or empty");
        }
        
        try {
            Map<String, Object> claims = new HashMap<>();
            
            // Parse token parts (header.payload.signature)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ValidationException("INVALID_TOKEN_FORMAT", "Invalid JWT token format");
            }

            // Decode payload (base64url)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                        
            claims.put("raw_payload", payload);
            
            return claims;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("INVALID_TOKEN", "Invalid JWT token: " + e.getMessage());
        }
    }

    public boolean validateToken(String token) {
        try {
            // Parse token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // Decode payload
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // Check expiration (simplified - extract exp claim)
            // For production, use JwtConsumer for proper validation
            if (payload.contains("\"exp\"")) {
                // Extract exp value using simple string manipulation
                String expStr = payload.replaceAll(".*\"exp\":(\\d+).*", "$1");
                try {
                    long exp = Long.parseLong(expStr);
                    if (Instant.now().isAfter(Instant.ofEpochSecond(exp))) {
                        return false; // Token expired
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Map<String, Object> claims = extractClaims(token);
        return (String) claims.get("sub");
    }

    public String getEmailFromToken(String token) {
        Map<String, Object> claims = extractClaims(token);
        return (String) claims.get("email");
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Map<String, Object> claims = extractClaims(token);
        Object rolesObj = claims.get("roles");
        
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        
        return List.of();
    }

    public String getOrganizationIdFromToken(String token) {
        Map<String, Object> claims = extractClaims(token);
        return (String) claims.get("organizationId");
    }

    public String getOrganizationRoleFromToken(String token) {
        Map<String, Object> claims = extractClaims(token);
        return (String) claims.get("organizationRole");
    }
}
