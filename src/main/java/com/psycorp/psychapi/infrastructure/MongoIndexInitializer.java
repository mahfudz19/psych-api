package com.psycorp.psychapi.infrastructure;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Inisialisasi index MongoDB saat aplikasi start.
 * Index dibuat secara idempotent (tidak error jika sudah ada).
 * 
 * @author Architect
 */
@ApplicationScoped
public class MongoIndexInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);
    
    /**
     * Dipanggil otomatis saat aplikasi Quarkus start.
     * Membuat index yang diperlukan untuk referral code system.
     * 
     * @param ev StartupEvent dari Quarkus
     * @param mongoClient MongoClient dari Quarkus MongoDB extension
     */
    void onStart(@Observes StartupEvent ev, MongoClient mongoClient) {
        log.info("🚀 Starting MongoDB index initialization...");
        
        try {
            // Get database name dari configuration (default: "psych")
            String databaseName = System.getProperty("quarkus.mongodb.database", "psych");
            
            createUsersIndexes(mongoClient.getDatabase(databaseName));
            createRateLimitsIndexes(mongoClient.getDatabase(databaseName));
            
            log.info("✅ MongoDB index initialization completed successfully");
            
        } catch (Exception e) {
            log.error("❌ MongoDB index initialization failed: {}", e.getMessage(), e);
            log.warn("⚠️  Application will continue, but indexes may need to be created manually");
        }
    }
    
    /**
     * Create index pada collection users untuk referralCode.
     * Unique sparse index untuk mencegah duplicate referral code.
     * 
     * @param database MongoDB database
     */
    private void createUsersIndexes(MongoDatabase database) {
        log.info("📌 Initializing indexes on collection 'users'...");
        
        MongoCollection<Document> usersCollection = database.getCollection("users");
        
        // Check existing indexes
        List<String> existingIndexes = usersCollection.listIndexes()
            .map(index -> index.getString("name"))
            .into(new java.util.ArrayList<>());
        
        // Create unique sparse index on referralCode with graceful fallback
        String indexName = "idx_referral_code_unique";
        if (existingIndexes.contains(indexName)) {
            log.info("✅ Index '{}' already exists on users collection", indexName);
        } else {
            IndexOptions options = new IndexOptions()
                .name(indexName)
                .unique(true)
                .sparse(true);
            
            try {
                usersCollection.createIndex(Indexes.ascending("referralCode"), options);
                log.info("✅ Created unique sparse index '{}' on users.referralCode", indexName);
            } catch (DuplicateKeyException e) {
                log.error("❌ Duplicate referralCode detected: {}", e.getMessage());
                log.warn("⚠️  Cannot create unique index due to duplicate data");
                log.warn("⚠️  Creating NON-UNIQUE index as fallback");
                log.warn("⚠️  Query to find duplicates:");
                log.warn("⚠️  db.users.aggregate([{$group:{_id:\"$referralCode\",c:{$sum:1}}},{$match:{c:{$gt:1}}}])");
                
                // Fallback: create non-unique index
                IndexOptions fallbackOptions = new IndexOptions()
                    .name("idx_referral_code")
                    .sparse(true);
                
                usersCollection.createIndex(Indexes.ascending("referralCode"), fallbackOptions);
                log.info("✅ Created NON-UNIQUE fallback index 'idx_referral_code' on users.referralCode");
            }
        }
        
    }
    
    /**
     * Create index pada collection ratelimits untuk TTL auto-cleanup.
     * 
     * @param database MongoDB database
     */
    private void createRateLimitsIndexes(MongoDatabase database) {
        log.info("📌 Initializing indexes on collection 'ratelimits'...");
        
        MongoCollection<Document> ratelimitsCollection = database.getCollection("ratelimits");
        
        // Check existing indexes
        List<String> existingIndexes = ratelimitsCollection.listIndexes()
            .map(index -> index.getString("name"))
            .into(new java.util.ArrayList<>());
        
        // Create TTL index on expiresAt
        String ttlIndexName = "expiresAt_1";
        if (existingIndexes.contains(ttlIndexName)) {
            log.info("✅ TTL index '{}' already exists on ratelimits collection", ttlIndexName);
        } else {
            IndexOptions options = new IndexOptions()
                .name(ttlIndexName)
                .expireAfter(0L, TimeUnit.SECONDS);
            
            ratelimitsCollection.createIndex(Indexes.ascending("expiresAt"), options);
            log.info("✅ Created TTL index '{}' on ratelimits.expiresAt", ttlIndexName);
        }
    }
    
}
