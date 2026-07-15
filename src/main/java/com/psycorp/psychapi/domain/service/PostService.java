package com.psycorp.psychapi.domain.service;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.api.dto.PostRequests.PostListRequest;
import com.psycorp.psychapi.common.util.FilterParser;
import com.psycorp.psychapi.common.util.SearchBuilder;
import com.psycorp.psychapi.common.util.SortBuilder;
import com.psycorp.psychapi.domain.model.Post;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;

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
        Bson finalFilter = combineFilters(searchFilter, customFilter);
        
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
        Bson finalFilter = combineFilters(searchFilter, customFilter);
        
        return Post.count(finalFilter);
    }
    
    // Helper method untuk combine filters
    private Bson combineFilters(Bson... filters) {
        List<Bson> validFilters = new ArrayList<>();
        for (Bson filter : filters) {
            if (filter != null) {
                validFilters.add(filter);
            }
        }
        
        if (validFilters.isEmpty()) {
            return new Document(); // empty = select all
        }
        
        return validFilters.size() == 1
            ? validFilters.get(0)
            : Filters.and(validFilters);
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
