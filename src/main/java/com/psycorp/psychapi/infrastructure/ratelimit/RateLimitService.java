package com.psycorp.psychapi.infrastructure.ratelimit;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.psycorp.psychapi.domain.model.RateLimitEntry;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service untuk rate limiting menggunakan MongoDB.
 * Mendukung distributed rate limiting untuk Cloud Run environment.
 * 
 * @author Architect
 */
@ApplicationScoped
public class RateLimitService {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    
    // Configuration constants
    private static final int VALIDATION_LIMIT = 20;
    private static final int VALIDATION_WINDOW = 60; // seconds
    private static final int GENERATION_LIMIT = 5;
    private static final int GENERATION_WINDOW = 3600; // seconds
    private static final int REGENERATION_LIMIT = 3;
    private static final int REGENERATION_WINDOW = 86400; // seconds
    
    /**
     * Check rate limit untuk code validation.
     * 
     * @param ipAddress IP address dari client
     * @throws RateLimitExceededException jika limit exceeded
     */
    public void checkCodeValidationLimit(String ipAddress) {
        String key = "ratelimit:validation:" + normalizeIp(ipAddress);
        incrementAndCheck(key, VALIDATION_LIMIT, VALIDATION_WINDOW);
    }
    
    /**
     * Check rate limit untuk code generation.
     * 
     * @param userId User ID yang generate code
     * @throws RateLimitExceededException jika limit exceeded
     */
    public void checkCodeGenerationLimit(String userId) {
        String key = "ratelimit:generation:" + userId;
        incrementAndCheck(key, GENERATION_LIMIT, GENERATION_WINDOW);
    }
    
    /**
     * Check rate limit untuk code regeneration.
     * 
     * @param userId User ID yang regenerate code
     * @throws RateLimitExceededException jika limit exceeded
     */
    public void checkCodeRegenerationLimit(String userId) {
        String key = "ratelimit:regeneration:" + userId;
        incrementAndCheck(key, REGENERATION_LIMIT, REGENERATION_WINDOW);
    }
    
    /**
     * Increment counter dan check apakah limit exceeded.
     * Menggunakan atomic findOneAndUpdate untuk race condition prevention.
     * 
     * @param key Key unik untuk rate limit
     * @param limit Maximum requests allowed dalam window
     * @param windowSeconds Window waktu dalam detik
     * @throws RateLimitExceededException jika limit exceeded
     */
    private void incrementAndCheck(String key, int limit, int windowSeconds) {
        Instant now = Instant.now();
        
        try {
            // Atomic find-and-update dengan upsert
            RateLimitEntry entry = (RateLimitEntry) RateLimitEntry.mongoCollection().findOneAndUpdate(
                Filters.and(
                    Filters.eq("_id", key),
                    Filters.gt("expiresAt", now)
                ),
                Updates.combine(
                    Updates.inc("count", 1),
                    Updates.set("updatedAt", now)
                ),
                new FindOneAndUpdateOptions()
                    .upsert(true)
                    .returnDocument(ReturnDocument.AFTER)
            );
            
            // Jika entri baru, set TTL
            if (entry != null && entry.getCount() != null && entry.getCount() == 1) {
                Instant expiresAt = now.plusSeconds(windowSeconds);
                RateLimitEntry.mongoCollection().updateOne(
                    Filters.eq("_id", key),
                    Updates.combine(
                        Updates.set("createdAt", now),
                        Updates.set("expiresAt", expiresAt)
                    )
                );
            }
            
            // Check if limit exceeded
            if (entry != null && entry.getCount() != null && entry.getCount() > limit) {
                log.warn("Rate limit exceeded: key={}, count={}, limit={}, windowSeconds={}",
                    key, entry.getCount(), limit, windowSeconds);
                
                throw new RateLimitExceededException(
                    "Too many requests. Try again in " + windowSeconds + " seconds."
                );
            }
            
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate limit check failed: key={}", key, e);
            // Fail open - allow request if rate limiting fails
            // Ini untuk mencegah DoS jika MongoDB down
        }
    }
    
    /**
     * Normalize IP address untuk privacy dan konsistensi.
     * Mask last octet untuk IPv4.
     * 
     * @param ipAddress IP address dari client
     * @return Normalized IP address
     */
    private String normalizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }
        
        // Handle IPv6 (simple normalization)
        if (ipAddress.contains(":")) {
            // Take first 4 segments untuk IPv6
            String[] parts = ipAddress.split(":");
            if (parts.length >= 4) {
                return String.join(":", parts[0], parts[1], parts[2], parts[3]) + "::";
            }
            return ipAddress;
        }
        
        // Mask last octet untuk IPv4 (privacy)
        return ipAddress.replaceAll("\\.\\d+$", ".0");
    }
    
    /**
     * Reset rate limit untuk key tertentu (untuk admin purposes).
     * 
     * @param key Key yang akan di-reset
     */
    public void resetRateLimit(String key) {
        try {
            RateLimitEntry.delete("id", key);
            log.info("Rate limit reset: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to reset rate limit: key={}", key, e);
        }
    }
    
    /**
     * Get current count untuk key tertentu (untuk monitoring).
     * 
     * @param key Key untuk check
     * @return Current count, atau 0 jika tidak ada
     */
    public int getCurrentCount(String key) {
        try {
            RateLimitEntry entry = RateLimitEntry.findById(key);
            if (entry != null && entry.getExpiresAt() != null && entry.getExpiresAt().isAfter(Instant.now())) {
                return entry.getCount() != null ? entry.getCount() : 0;
            }
            return 0;
        } catch (Exception e) {
            log.warn("Failed to get current count: key={}", key, e);
            return 0;
        }
    }
}
