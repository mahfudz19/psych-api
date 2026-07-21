package com.psycorp.psychapi.infrastructure.security;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.psycorp.psychapi.config.JwtConfig;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JwtService {

    @Inject
    JwtConfig jwtConfig;

    public record TokenClaims(
        String userId,
        String email,
        List<String> roles,
        Boolean isSuperAdmin,
        Instant issuedAt,
        Instant expiresAt
    ) {}

    public String generateToken(User user, boolean isSuperAdmin) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (user.id == null) {
            throw new IllegalStateException("User ID is null - user not persisted?");
        }
        
        try {
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(jwtConfig.expiresIn());

            Algorithm algorithm = Algorithm.HMAC256(jwtConfig.sign().secret());
            var jwtCreator = JWT.create()
                // Standard claims (RFC 7519)
                .withIssuer(jwtConfig.issuer())
                .withSubject(user.id.toHexString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                
                // Essential custom claims
                .withClaim("email", user.getEmail())
                .withClaim("roles", user.getRoles());

            if (isSuperAdmin) {
                jwtCreator.withClaim("isSuperAdmin", true);
            }

            return jwtCreator.sign(algorithm);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token: " + e.getMessage(), e);
        }
    }

    public String generateToken(User user) {
        return generateToken(user, false);
    }

    public TokenClaims parseClaims(String token) {
        if (token == null || token.isEmpty()) {
            throw new ValidationException("INVALID_TOKEN", "Token cannot be null or empty");
        }

        try {
            // Extract payload dari token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ValidationException("INVALID_TOKEN_FORMAT", "Invalid JWT token format");
            }

            // Decode payload (base64url)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // Parse JSON manually untuk essential claims
            String userId = extractStringClaim(payload, "sub");
            String email = extractStringClaim(payload, "email");
            List<String> roles = extractListClaim(payload, "roles");
            Long issuedAt = extractLongClaim(payload, "iat");
            Long expiresAt = extractLongClaim(payload, "exp");
            Boolean isSuperAdmin = extractBooleanClaim(payload, "isSuperAdmin");

            // Validate expiration
            if (jwtConfig.verify().expiresAt() && expiresAt != null && Instant.now().isAfter(Instant.ofEpochSecond(expiresAt))) {
                throw new ValidationException("TOKEN_EXPIRED", "JWT token has expired");
            }

            return new TokenClaims(userId, email, roles,
                isSuperAdmin != null ? isSuperAdmin : false,
                issuedAt != null ? Instant.ofEpochSecond(issuedAt) : null,
                expiresAt != null ? Instant.ofEpochSecond(expiresAt) : null);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("INVALID_TOKEN", "Invalid JWT token: " + e.getMessage());
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }

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
                        return false;
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

    public User extractUserFromToken(String token) {
        TokenClaims claims = parseClaims(token);
        
        User user = new User();
        user.id = new ObjectId(claims.userId());
        user.setEmail(claims.email());
        user.setRoles(claims.roles());
        
        return user;
    }

    public String getUserIdFromToken(String token) {
        TokenClaims claims = parseClaims(token);
        return claims.userId();
    }

    private String extractStringClaim(String payload, String claimName) {
        String regex = "\"" + claimName + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Long extractLongClaim(String payload, String claimName) {
        String regex = "\"" + claimName + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(payload);
        if (matcher.find()) {
            String value = matcher.group(1);
            return Long.valueOf(value);
        }
        return null;
    }

    private List<String> extractListClaim(String payload, String claimName) {
        String regex = "\"" + claimName + "\"\\s*:\\s*\\[([^\\]]*)\\]";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(payload);
        if (matcher.find()) {
            String listContent = matcher.group(1);
            // Parse array elements
            java.util.regex.Pattern elementPattern = java.util.regex.Pattern.compile("\"([^\"]*)\"");
            java.util.regex.Matcher elementMatcher = elementPattern.matcher(listContent);
            java.util.List<String> result = new java.util.ArrayList<>();
            while (elementMatcher.find()) {
                result.add(elementMatcher.group(1));
            }
            return result;
        }
        return null;
    }

    private Boolean extractBooleanClaim(String payload, String claimName) {
        String regex = "\"" + claimName + "\"\\s*:\\s*(true|false)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(payload);
        return matcher.find() ? Boolean.valueOf(matcher.group(1)) : null;
    }
}
