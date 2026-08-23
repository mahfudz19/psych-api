package com.psycorp.psychapi.feature.auth.model;

import java.time.Instant;
import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.psycorp.psychapi.shared.util.DocumentUpdater;
import com.psycorp.psychapi.shared.util.MongoFilter;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

/**
 * Refresh Token document untuk menyimpan refresh tokens.
 * Collection: refresh_tokens
 */
@MongoEntity(collection = "refresh_tokens")
public class RefreshToken extends PanacheMongoEntity {
    
    // === TOKEN INFO ===
    private String tokenHash;      // SHA-256 hash dari refresh token
    private String tokenPrefix;    // First 8 chars (untuk identification)
    
    // === USER & DEVICE ===
    private ObjectId userId;       // Reference ke users collection
    private String deviceId;       // Device fingerprint (dev_abc123)
    private DeviceInfo deviceInfo; // Embedded document untuk session display
    
    // === STATUS & LIFECYCLE ===
    private String status;         // "active", "revoked", "expired", "rotated"
    private Instant expiresAt;     // Token expiry time
    private Instant usedAt;        // Last time token was used untuk refresh
    private Instant rotatedAt;     // Time when token was rotated
    
    // === REVOCATION INFO ===
    private String revokeReason;   // "LOGOUT", "USER_REQUESTED", "TOKEN_REUSE_DETECTED", "SECURITY_ALERT"
    private ObjectId replacedBy;   // Reference ke new token (setelah rotation)
    
    // === TIMESTAMPS ===
    private Instant createdAt;
    private Instant updatedAt;
    
    // === STATUS ENUM ===
    public enum TokenStatus {
        ACTIVE("active"),
        REVOKED("revoked"),
        EXPIRED("expired"),
        ROTATED("rotated");
        
        private final String value;
        
        TokenStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static TokenStatus fromValue(String value) {
            for (TokenStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid TokenStatus: " + value);
        }
    }
    
    // === REVOKE REASON ENUM ===
    public enum RevokeReason {
        LOGOUT("LOGOUT"),
        USER_REQUESTED("USER_REQUESTED"),
        TOKEN_REUSE_DETECTED("TOKEN_REUSE_DETECTED"),
        SECURITY_ALERT("SECURITY_ALERT");
        
        private final String value;
        
        RevokeReason(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static RevokeReason fromValue(String value) {
            for (RevokeReason reason : values()) {
                if (reason.value.equalsIgnoreCase(value)) {
                    return reason;
                }
            }
            return null;
        }
    }
    
    // Constructor
    public RefreshToken() {}
    
    // === GETTERS ===
    public ObjectId getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public String getTokenPrefix() { return tokenPrefix; }
    public ObjectId getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public DeviceInfo getDeviceInfo() { return deviceInfo; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getRotatedAt() { return rotatedAt; }
    public String getRevokeReason() { return revokeReason; }
    public ObjectId getReplacedBy() { return replacedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    // === SETTERS ===
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }
    public void setUserId(ObjectId userId) { this.userId = userId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setDeviceInfo(DeviceInfo deviceInfo) { this.deviceInfo = deviceInfo; }
    public void setStatus(String status) { this.status = status; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public void setRotatedAt(Instant rotatedAt) { this.rotatedAt = rotatedAt; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
    public void setReplacedBy(ObjectId replacedBy) { this.replacedBy = replacedBy; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    // === HELPER METHODS ===
    
    /**
     * Check apakah token masih aktif.
     */
    public boolean isActive() {
        return TokenStatus.ACTIVE.getValue().equals(status) 
            && expiresAt.isAfter(Instant.now());
    }
    
    /**
     * Check apakah token sudah expired.
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
    
    /**
     * Check apakah token sudah di-revoke.
     */
    public boolean isRevoked() {
        return TokenStatus.REVOKED.getValue().equals(status);
    }
    
    /**
     * Check apakah token sudah di-rotate.
     */
    public boolean isRotated() {
        return TokenStatus.ROTATED.getValue().equals(status);
    }
    
    /**
     * Mark token sebagai revoked dengan reason.
     */
    public void revoke(RevokeReason reason) {
        this.status = TokenStatus.REVOKED.getValue();
        this.revokeReason = reason.getValue();
        this.updatedAt = Instant.now();
        executeUpdate(Updates.combine(
            Updates.set("status", this.status),
            Updates.set("revokeReason", this.revokeReason)
        ));
    }
    
    /**
     * Mark token sebagai rotated dan set reference ke token baru.
     */
    public void rotate(ObjectId newTokenId) {
        this.status = TokenStatus.ROTATED.getValue();
        this.replacedBy = newTokenId;
        this.rotatedAt = Instant.now();
        this.updatedAt = Instant.now();
        executeUpdate(Updates.combine(
            Updates.set("status", this.status),
            Updates.set("replacedBy", this.replacedBy),
            Updates.set("rotatedAt", this.rotatedAt)
        ));
    }
    
    /**
     * Update usedAt timestamp.
     */
    public void markAsUsed() {
        this.usedAt = Instant.now();
        this.updatedAt = Instant.now();
        executeUpdate(Updates.set("usedAt", this.usedAt));
    }
    
    /**
     * Execute update dengan auto-update updatedAt.
     */
    public void executeUpdate(Bson update) {
        Bson updateWithTimestamp = Updates.combine(
            update,
            Updates.set("updatedAt", Instant.now())
        );
        RefreshToken.mongoCollection().updateOne(Filters.eq("_id", this.id), updateWithTimestamp);
    }
    
    /**
     * Static find by token hash.
     */
    public static RefreshToken findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResult();
    }
    
    /**
     * Static find all active tokens by user.
     */
    public static java.util.List<RefreshToken> findActiveByUserId(ObjectId userId) {
        return list("userId = ?1 and status = ?2", userId, TokenStatus.ACTIVE.getValue());
    }
    
    /**
     * Static find all tokens by user (including revoked).
     */
    public static java.util.List<RefreshToken> findByUserId(ObjectId userId) {
        return list("userId", userId);
    }
    
    /**
     * Static count active tokens by user.
     */
    public static long countActiveByUserId(ObjectId userId) {
        return count("userId = ?1 and status = ?2", userId, TokenStatus.ACTIVE.getValue());
    }

    /**
     * Revoke multiple tokens by their IDs (bulk operation).
     * 
     * @param tokenIds Array of token ObjectId to revoke
     * @param reason Reason for revocation
     * @return Number of tokens successfully revoked
     */
    public static long revokeByIds(ObjectId[] tokenIds, RevokeReason reason) {
        Bson filter = MongoFilter.and(
            Filters.in("_id", List.of(tokenIds)),
            Filters.in("status", TokenStatus.ACTIVE.getValue()),
            Filters.gt("expiresAt", Instant.now())
        );
        
        Bson update = DocumentUpdater.update()
            .set("status", TokenStatus.REVOKED.getValue())
            .set("revokeReason", reason.getValue())
            .set("updatedAt", Instant.now())
            .build();
        
        return mongoCollection().updateMany(filter, update).getModifiedCount();
    }

    /**
     * Revoke ALL tokens for a user (security emergency).
     * 
     * @param userId User whose tokens to revoke
     * @param reason Reason for revocation
     * @return Number of tokens successfully revoked
     */
    public static long revokeAllByUserId(ObjectId userId, RevokeReason reason) {
        Bson filter = MongoFilter.and(
            Filters.eq("userId", userId),
            Filters.in("status", TokenStatus.ACTIVE.getValue()),
            Filters.gt("expiresAt", Instant.now())
        );
        
        Bson update = DocumentUpdater.update()
            .set("status", TokenStatus.REVOKED.getValue())
            .set("revokeReason", reason.getValue())
            .set("updatedAt", Instant.now())
            .build();
        
        return mongoCollection().updateMany(filter, update).getModifiedCount();
    }

}
