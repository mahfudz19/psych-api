package com.psycorp.psychapi.domain.model;

import java.time.Instant;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

/**
 * Entity untuk menyimpan rate limit data.
 * Digunakan untuk tracking request count per key dengan TTL auto-expire.
 * 
 * @author Architect
 */
@MongoEntity(collection = "ratelimits")
public class RateLimitEntry extends PanacheMongoEntity {
    
    /**
     * Key unik untuk rate limit (e.g., "ratelimit:validation:192.168.1.1").
     */
    public String id;
    
    /**
     * Jumlah request yang telah dilakukan dalam window waktu.
     */
    public Integer count;
    
    /**
     * Waktu entri ini pertama kali dibuat.
     */
    public Instant createdAt;
    
    /**
     * Waktu entri ini terakhir di-update.
     */
    public Instant updatedAt;
    
    /**
     * Waktu entri ini akan expire (untuk TTL index MongoDB).
     * TTL index dibuat via migration script: scripts/mongodb-migration.js
     */
    public Instant expiresAt;
    
    public RateLimitEntry() {}
    
    /**
     * Constructor untuk membuat entri rate limit baru.
     * 
     * @param id Key unik untuk rate limit
     * @param count Initial count (biasanya 1)
     * @param createdAt Waktu pembuatan
     * @param updatedAt Waktu update terakhir
     * @param expiresAt Waktu expire (untuk TTL)
     */
    public RateLimitEntry(String id, Integer count, Instant createdAt, Instant updatedAt, Instant expiresAt) {
        this.id = id;
        this.count = count;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }
    
    // Getters (untuk PanacheMongoEntity, fields sudah public)
    public String getId() { return id; }
    public Integer getCount() { return count; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setCount(Integer count) { this.count = count; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
