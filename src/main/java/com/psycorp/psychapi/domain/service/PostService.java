package com.psycorp.psychapi.domain.service;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.psycorp.psychapi.api.dto.PostRequests.PostListRequest;
import com.psycorp.psychapi.common.util.FilterCombiner;
import com.psycorp.psychapi.common.util.FilterParser;
import com.psycorp.psychapi.common.util.ObjectIdValidator;
import com.psycorp.psychapi.common.util.SearchBuilder;
import com.psycorp.psychapi.common.util.SortBuilder;
import com.psycorp.psychapi.domain.model.Post;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostService {

    // Fields yang bisa di-search untuk Post
    private static final String[] SEARCH_FIELDS = {"title", "content"};

    // === READ ALL (dengan pagination, search, filter, sort) ===
    public List<Post> getAllPosts(PostListRequest request) {
        // 1. Build search filter di title dan content
        Bson searchFilter = SearchBuilder.build(request.search(), SEARCH_FIELDS);
        
        // 2. Parse custom filter (contoh: "status:in:draft,published")
        Bson customFilter = FilterParser.parse(request.filter());
        
        // 3. Combine semua filters dengan $and
        Bson finalFilter =  FilterCombiner.combine(searchFilter, customFilter);
        
        // 4. Build sort
        Bson sort = SortBuilder.build(request.sortBy(), request.sortOrder());
        
        // 5. Execute query dengan pagination
        PanacheQuery<Post> query = Post.find(finalFilter, sort);
        query.page(request.page() - 1, request.limit());
        
        return query.list();
    }

    public long getTotalPostsCount(PostListRequest request) {
        // Build search filter
        Bson searchFilter = SearchBuilder.build(request.search(), SEARCH_FIELDS);
        
        // Parse custom filter
        Bson customFilter = FilterParser.parse(request.filter());
        
        // Combine filters
        Bson finalFilter = FilterCombiner.combine(searchFilter, customFilter);
        
        return Post.count(finalFilter);
    }

    // === READ BY ID ===
    public Post getPostById(String id) {
        // ✅ Validate ObjectId format
        ObjectId objectId = ObjectIdValidator.validate(id);
        
        Post post = Post.findById(objectId);
        if (post == null) {
            throw new NotFoundException("POST_NOT_FOUND", "Post with id " + id + " not found");
        }
        return post;
    }

    // === CREATE ===
    public Post createPost(String title, String content, String status) {
        validatePostData(title, content, status);
    
        Post post = Post.create(title, content, status);
        post.persist();
        return post;
    }

    // === UPDATE ===
    public Post updatePost(String id, String title, String content, String status) {
        Post post = getPostById(id);
 
        validatePostData(title, content, status);
        
        post.update(title, content, status);
        post.update();
        return post;
    }

    // === DELETE ===
    public boolean deletePost(String id) {
        Post post = getPostById(id);
        post.delete();
        return true;
    }

    private void validatePostData(String title, String content, String status) {
        List<String> errors = new ArrayList<>();
        
        if (title == null || title.isBlank()) {
            errors.add("Title is required");
        } else if (title.length() < 3) {
            errors.add("Title must be at least 3 characters");
        } else if (title.length() > 200) {
            errors.add("Title must not exceed 200 characters");
        }
        
        if (content != null && content.length() > 10000) {
            errors.add("Content must not exceed 10000 characters");
        }
        
        if (status != null && !status.matches("^(draft|published|archived)$")) {
            errors.add("Status must be one of: draft, published, archived");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", String.join(", ", errors));
        }
    }

}
