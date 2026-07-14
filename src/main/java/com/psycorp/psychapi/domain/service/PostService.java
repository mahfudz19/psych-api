package com.psycorp.psychapi.domain.service;

import java.util.List;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.domain.model.Post;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostService {

    // === READ ALL (dengan pagination) ===
    public List<Post> getAllPosts(int page, int limit) {
        return Post.findAll()
                .page(page - 1, limit) // Panache page 0-based
                .list();
    }

    public long getTotalPostsCount() {
        return Post.count();
    }

    // === READ BY ID ===
    public Post getPostById(String id) {
        Post post = Post.findById(new ObjectId(id));
        if (post == null) {
            throw new NotFoundException("POST_NOT_FOUND", "Post with id " + id + " not found");
        }
        return post;
    }

    // === CREATE ===
    public Post createPost(String title, String content, String status) {
        Post post = new Post();
        post.title = title;
        post.content = content;
        post.status = status != null ? status : "draft";
        // prePersist() akan dipanggil otomatis oleh Panache
        post.persist();
        return post;
    }

    // === UPDATE ===
    public Post updatePost(String id, String title, String content, String status) {
        Post post = getPostById(id); // akan throw NotFoundException jika tidak ada
        post.title = title;
        post.content = content;
        post.status = status;
        // preUpdate() akan dipanggil otomatis oleh Panache
        post.update();
        return post;
    }

    // === DELETE ===
    public boolean deletePost(String id) {
        Post post = Post.findById(new ObjectId(id));
        if (post == null) {
            return false;
        }
        post.delete();
        return true;
    }
}
