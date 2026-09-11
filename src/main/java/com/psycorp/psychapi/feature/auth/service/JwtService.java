package com.psycorp.psychapi.feature.auth.service;

import java.security.Key;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;

import com.psycorp.psychapi.feature.user.model.User;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "app.jwt.secret")
    String jwtSecret;

    // Access token: 15 menit (hardcoded)
    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 15 * 60;
    
    // Refresh token: 7 hari (hardcoded)
    private static final long REFRESH_TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000;

    /**
     * Generate access token untuk user.
     * Durasi: 15 menit
     */
    public String generateAccessToken(ObjectId userId, String email, List<User.Role> roles) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        long expirySeconds = nowSeconds + ACCESS_TOKEN_EXPIRY_SECONDS;

        JwtClaims claims = new JwtClaims();
        claims.setJwtId(java.util.UUID.randomUUID().toString());
        claims.setIssuer("psych-api");
        claims.setClaim("iat", nowSeconds);
        claims.setClaim("exp", expirySeconds);
        claims.setSubject(userId.toHexString());
        claims.setClaim("email", email);
        claims.setClaim("type", "access");
        claims.setClaim("roles", roles);

        return createJwt(claims);
    }

    public long getAccessTokenExpiry() {
        return ACCESS_TOKEN_EXPIRY_SECONDS;
    }

    /**
     * Generate refresh token string (random).
     * Durasi: 7 hari
     * Note: Refresh token adalah random string, BUKAN JWT.
     */
    public String generateRefreshToken() {
        return java.util.UUID.randomUUID().toString() 
            + java.util.UUID.randomUUID().toString()
            + java.util.UUID.randomUUID().toString();
    }

    /**
     * Get expiry time untuk refresh token (7 hari dari sekarang).
     */
    public Instant getRefreshTokenExpiry() {
        return Instant.now().plusMillis(REFRESH_TOKEN_EXPIRY_MS);
    }

    /**
     * Validate access token dan return userId.
     */
    public ObjectId validateAccessToken(String token) {
        try {
            JwtClaims claims = parseJwt(token);
            
            // Validate token type
            String type = claims.getStringClaimValue("type");
            if (!"access".equals(type)) {
                throw new InvalidJwtException("Invalid token type: " + type, null, null);
            }
            
            // Validate expiration - get exp claim as Long
            Long exp = claims.getExpirationTime() != null ? claims.getExpirationTime().getValue() : null;
            if (exp != null && exp < System.currentTimeMillis() / 1000) {
                throw new InvalidJwtException("Token has expired", null, null);
            }
            
            String subject = claims.getSubject();
            return new ObjectId(subject);
            
        } catch (InvalidJwtException e) {
            throw new NotAuthorizedException("Invalid token: " + e.getMessage(), e);
        } catch (MalformedClaimException e) {
            throw new NotAuthorizedException("Malformed token claim: " + e.getMessage(), e);
        }
    }

    /**
     * Extract userId dari token tanpa validasi (untuk debugging).
     */
    public ObjectId getUserIdFromToken(String token) {
        try {
            JwtClaims claims = parseJwt(token);
            String subject = claims.getSubject();
            return new ObjectId(subject);
        } catch (InvalidJwtException e) {
            throw new NotAuthorizedException("Invalid token", e);
        } catch (MalformedClaimException e) {
            throw new NotAuthorizedException("Malformed token claim", e);
        }
    }

    /**
     * Create JWT dari claims.
     */
    private String createJwt(JwtClaims claims) {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(getSigningKey());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        jws.setKeyIdHeaderValue("psych-api-key");

        try {
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new RuntimeException("Error creating JWT", e);
        }
    }

    /**
     * Parse JWT dan return claims.
     */
    private JwtClaims parseJwt(String token) throws InvalidJwtException, MalformedClaimException {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
            .setVerificationKey(getSigningKey())
            .build();

        return jwtConsumer.processToClaims(token);
    }

    /**
     * Get signing key dari secret.
     */
    private Key getSigningKey() {
        // Lazy initialization untuk menghindari circular dependency
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            jwtSecret = Arc.container()
                .instance(org.eclipse.microprofile.config.Config.class)
                .get()
                .getValue("app.jwt.secret", String.class);
        }
        // Gunakan SecretKeySpec langsung dari string secret
        return new SecretKeySpec(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HMACSHA256");
    }
}
