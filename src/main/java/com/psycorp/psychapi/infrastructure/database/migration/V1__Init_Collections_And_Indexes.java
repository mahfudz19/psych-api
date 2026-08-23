package com.psycorp.psychapi.infrastructure.database.migration;

import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * Migration V1: Inisialisasi indexes untuk semua collection.
 * - Users: unique sparse index pada referralCode
 * - Refresh Tokens: TTL index pada expiresAt (auto-delete setelah expired)
 */
@ChangeUnit(id = "V1__Init_Collections_And_Indexes", order = "001", author = "mahfudz")
public class V1__Init_Collections_And_Indexes {

    /**
     * Execution method untuk membuat indexes pada semua collection.
     *
     * @param mongoDatabase Database MongoDB untuk operasi indexing
     */
    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        // === COLLECTION: USERS ===
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        
        // 1. Create unique sparse index pada referralCode
        IndexOptions uniqueSparseOptions = new IndexOptions()
                .unique(true)
                .sparse(true);
        
        users.createIndex(
                Indexes.ascending("referralCode"),
                uniqueSparseOptions
        );
        
        // 2. Create Partial TTL index pada expiredAt (HANYA untuk status pending)
        // Dokumen dihapus saat expiredAt < now, TAPI hanya jika status == "pending"
        Document pendingFilter = new Document("status", "pending");
        IndexOptions userTtlOptions = new IndexOptions()
                .name("pending_expiredAt_ttl")
                .expireAfter(0L, TimeUnit.SECONDS)
                .partialFilterExpression(pendingFilter);

        users.createIndex(
                Indexes.ascending("expiredAt"),
                userTtlOptions
        );
        
        // === COLLECTION: REFRESH_TOKENS ===
        MongoCollection<Document> refreshTokens = mongoDatabase.getCollection("refresh_tokens");
        
        // 2. Create TTL index pada expiresAt
        // Dokumen akan otomatis dihapus MongoDB saat expiresAt < now
        IndexOptions ttlOptions = new IndexOptions()
                .name("expiresAt_ttl")
                .expireAfter(0L, TimeUnit.SECONDS);
        
        refreshTokens.createIndex(
                Indexes.ascending("expiresAt"),
                ttlOptions
        );
        
        // === COLLECTION: RATE_LIMITS ===
        MongoCollection<Document> rateLimits = mongoDatabase.getCollection("ratelimits");
        
        IndexOptions rateLimitTtlOptions = new IndexOptions()
                .name("expiresAt_ttl")
                .expireAfter(0L, TimeUnit.SECONDS);
        
        rateLimits.createIndex(
                Indexes.ascending("expiresAt"),
                rateLimitTtlOptions
        );
    }

    /**
     * Rollback execution untuk menghapus indexes jika migration gagal.
     *
     * @param mongoDatabase Database MongoDB untuk operasi rollback
     */
    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        // Drop users index
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        users.dropIndex("referralCode_1");
        users.dropIndex("pending_expiredAt_ttl");
        
        // Drop refresh_tokens TTL index
        MongoCollection<Document> refreshTokens = mongoDatabase.getCollection("refresh_tokens");
        refreshTokens.dropIndex("expiresAt_ttl");
    }
}
