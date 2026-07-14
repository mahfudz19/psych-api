package com.psycorp.psychapi.domain.model;

import java.time.Instant;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "posts")
public class Post extends PanacheMongoEntity {

    public String title;
    public String content;
    public String status;
    public Instant createdAt;
    public Instant updatedAt;

    // Lifecycle hooks (mirip Mongoose pre-save)
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
