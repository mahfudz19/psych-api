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

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        // === COLLECTION: USERS ===
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        
        IndexOptions uniqueSparseOptions = new IndexOptions().unique(true).sparse(true);
        users.createIndex(Indexes.ascending("referralCode"), uniqueSparseOptions);
        
        Document pendingFilter = new Document("status", "pending");
        IndexOptions userTtlOptions = new IndexOptions()
                .name("pending_expiredAt_ttl")
                .expireAfter(0L, TimeUnit.SECONDS)
                .partialFilterExpression(pendingFilter);

        users.createIndex(Indexes.ascending("expiredAt"), userTtlOptions);
        
        // === COLLECTION: REFRESH_TOKENS ===
        MongoCollection<Document> refreshTokens = mongoDatabase.getCollection("refresh_tokens");
        IndexOptions ttlOptions = new IndexOptions()
                .name("expiresAt_ttl")
                .expireAfter(0L, TimeUnit.SECONDS);
        refreshTokens.createIndex(Indexes.ascending("expiresAt"), ttlOptions);
        
        // === COLLECTION: RATE_LIMITS ===
        MongoCollection<Document> rateLimits = mongoDatabase.getCollection("ratelimits");
        IndexOptions rateLimitTtlOptions = new IndexOptions()
                .name("expiresAt_ttl")
                .expireAfter(0L, TimeUnit.SECONDS);
        rateLimits.createIndex(Indexes.ascending("expiresAt"), rateLimitTtlOptions);

        MongoCollection<Document> plans = mongoDatabase.getCollection("subscription_plans");
        plans.createIndex(Indexes.ascending("code"),new IndexOptions().unique(true).name("unique_plan_code"));

        // === COLLECTION: SUBSCRIPTIONS ===
        MongoCollection<Document> subscriptions = mongoDatabase.getCollection("subscriptions");
        subscriptions.createIndex(Indexes.compoundIndex(Indexes.ascending("subscriberId"),Indexes.ascending("subscriberType"),Indexes.ascending("status")),new IndexOptions().name("subscriber_status_idx"));
    }

    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        users.dropIndex("referralCode_1");
        users.dropIndex("pending_expiredAt_ttl");
        
        MongoCollection<Document> refreshTokens = mongoDatabase.getCollection("refresh_tokens");
        refreshTokens.dropIndex("expiresAt_ttl");

        MongoCollection<Document> rateLimits = mongoDatabase.getCollection("ratelimits");
        rateLimits.dropIndex("expiresAt_ttl");

        // Rollback untuk koleksi baru
        MongoCollection<Document> plans = mongoDatabase.getCollection("subscription_plans");
        plans.dropIndex("unique_plan_code");

        MongoCollection<Document> subscriptions = mongoDatabase.getCollection("subscriptions");
        subscriptions.dropIndex("subscriber_status_idx");
    }
}