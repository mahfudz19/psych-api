package com.psycorp.psychapi.infrastructure.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JwtService {
    public enum JwtClaim {
        // Standard claims (RFC 7519)
        ISSUER("iss", String.class),
        ISSUED_AT("iat", Long.class),
        EXPIRES_AT("exp", Long.class),
        
        // Custom claims - User identity
        USER_ID("sub", String.class),
        EMAIL("email", String.class),
        ROLES("roles", List.class),
        FULL_NAME("fullName", String.class),
        PROFILE_PICTURE("profilePicture", String.class),
        
        // Custom claims - Account context
        ACCOUNT_TYPE("accountType", String.class),
        ORGANIZATION_ID("organizationId", String.class),
        ORGANIZATION_ROLE("organizationRole", String.class),
        ORGANIZATION_NAME("organizationName", String.class),
        
        // Custom claims - Subscription
        SUBSCRIPTION_TIER("subscriptionTier", String.class),
        SUBSCRIPTION_EXPIRY("subscriptionExpiry", Long.class),
        
        // Custom claims - Token metadata
        STATUS("status", String.class),
        JTI("jti", String.class);
        
        private final String key;
        private final Class<?> type;
        
        JwtClaim(String key, Class<?> type) {
            this.key = key;
            this.type = type;
        }
        
        public String getKey() {
            return key;
        }

        public Class<?> getType() {
            return type;
        }
    }

    @ConfigProperty(name = "quarkus.smallrye.jwt.token-expires-in", defaultValue = "604800")
    Long tokenExpiresIn;

    @ConfigProperty(name = "quarkus.smallrye.jwt.issuer", defaultValue = "psych-api")
    String issuer;

    public String generateToken(User user, AccountType accountType) {
        return generateToken(user, accountType, tokenExpiresIn);
    }

    public String generateToken(User user, AccountType accountType, long expiresIn) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (accountType == null) {
            throw new IllegalArgumentException("AccountType cannot be null");
        }
        
        if (user.id == null) {
            throw new IllegalStateException("User ID is null - user not persisted?");
        }
        
        if (accountType == AccountType.ORGANIZATION) {
            return generateTokenForOrganization(
                user,
                user.getOrganizationId(),
                user.getOrganizationRole(),
                user.getOrganizationName(),
                expiresIn
            );
        } else {
            return generateTokenForIndividual(user, expiresIn);
        }
    }

    public String generateTokenForIndividual(User user, long expiresIn) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expiresIn);

        var claims = Jwt.claims()
            // Standard claims (RFC 7519)
            .issuer(issuer)
            .subject(user.id.toHexString())
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("jti", UUID.randomUUID().toString())
            
            // User info claims (hanya yang tidak null)
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles())
            .claim("fullName", user.getFullName());
        
        // Claim yang mungkin null - hanya ditambahkan jika ada value
        if (user.getProfilePicture() != null) {
            claims.claim("profilePicture", user.getProfilePicture());
        }
        
        // Account context - individual
        claims.claim("accountType", "individual")
                .claim("organizationId", "")
                .claim("organizationRole", "")
                .claim("organizationName", "");
        
        // Subscription info
        claims.claim("subscriptionTier", user.getSubscriptionTier());
        if (user.getSubscriptionExpiry() != null) {
            claims.claim("subscriptionExpiry", user.getSubscriptionExpiry().getEpochSecond());
        }
        
        // Account status
        claims.claim("status", user.getStatus());
        
        return claims.sign();
    }

    public String generateTokenForOrganization(
            User user,
            ObjectId organizationId,
            String organizationRole,
            String organizationName,
            long expiresIn) {
        
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expiresIn);

        var claims = Jwt.claims()
            // Standard claims (RFC 7519)
            .issuer(issuer)
            .subject(user.id.toHexString())
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("jti", UUID.randomUUID().toString())
            
            // User info claims (hanya yang tidak null)
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles())
            .claim("fullName", user.getFullName());
        
        // Claim yang mungkin null - hanya ditambahkan jika ada value
        if (user.getProfilePicture() != null) {
            claims.claim("profilePicture", user.getProfilePicture());
        }
        
        // Organization context
        claims.claim("accountType", "organization");
        if (organizationId != null) {
            claims.claim("organizationId", organizationId.toHexString());
        } else {
            claims.claim("organizationId", "");
        }
        
        if (organizationRole != null) {
            claims.claim("organizationRole", organizationRole);
        } else {
            claims.claim("organizationRole", "");
        }
        
        if (organizationName != null) {
            claims.claim("organizationName", organizationName);
        } else {
            claims.claim("organizationName", "");
        }
        
        // Subscription info
        claims.claim("subscriptionTier", user.getSubscriptionTier());
        if (user.getSubscriptionExpiry() != null) {
            claims.claim("subscriptionExpiry", user.getSubscriptionExpiry().getEpochSecond());
        }
        
        // Account status
        claims.claim("status", user.getStatus());
        
        return claims.sign();
    }

    private Map<String, Object> extractClaims(String token) {
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
            
            // Check expiration
            if (payload.contains("\"exp\"")) {
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

    public Object getClaim(JwtClaim claim, String token) {
        Map<String, Object> claims = extractClaims(token);
        return claims.get(claim.getKey());
    }
}
