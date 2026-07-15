package com.psycorp.psychapi.domain.model;

import java.time.Instant;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "posts")
public class Post extends PanacheMongoEntity {

    // ✅ Private fields - encapsulation
    private String title;
    private String content;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    // ✅ No-args constructor (required by Panache)
    public Post() {}

    // ✅ Getters (Panache needs these for serialization)
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ✅ Setters
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ✅ Factory method untuk create (opsional, untuk clean code)
    public static Post create(String title, String content, String status) {
        Post post = new Post();
        post.title = title;
        post.content = content;
        post.status = status != null ? status : "draft";
        post.createdAt = Instant.now();
        post.updatedAt = Instant.now();
        return post;
    }

    // ✅ Update method (opsional)
    public void update(String title, String content, String status) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (status != null) this.status = status;
        this.updatedAt = Instant.now();
    }
}
