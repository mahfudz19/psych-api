package com.psycorp.psychapi.api.resource;

import java.util.List;

import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.PaginationMeta;
import com.psycorp.psychapi.domain.model.Post;
import com.psycorp.psychapi.domain.service.PostService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    @Inject
    PostService postService;

    // === GET ALL (dengan pagination) ===
    @GET
    public Response getAllPosts(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        List<Post> posts = postService.getAllPosts(page, limit);
        long total = postService.getTotalPostsCount();
        int totalPages = (int) Math.ceil((double) total / limit);

        PaginationMeta meta = new PaginationMeta(page, limit, total, totalPages);
        return ResponseHelper.ok(posts, "Posts retrieved successfully", meta);
    }

    // === GET BY ID ===
    @GET
    @Path("/{id}")
    public Response getPostById(@PathParam("id") String id) {
        Post post = postService.getPostById(id);
        return ResponseHelper.ok(post, "Post retrieved successfully");
    }

    // === CREATE ===
    @POST
    public Response createPost(@QueryParam("title") String title,
                               @QueryParam("content") String content,
                               @QueryParam("status") String status) {
        Post post = postService.createPost(title, content, status);
        return ResponseHelper.created(post, "Post created successfully");
    }

    // === UPDATE ===
    @PUT
    @Path("/{id}")
    public Response updatePost(@PathParam("id") String id,
                               @QueryParam("title") String title,
                               @QueryParam("content") String content,
                               @QueryParam("status") String status) {
        Post post = postService.updatePost(id, title, content, status);
        return ResponseHelper.ok(post, "Post updated successfully");
    }

    // === DELETE ===
    @DELETE
    @Path("/{id}")
    public Response deletePost(@PathParam("id") String id) {
        boolean deleted = postService.deletePost(id);
        if (!deleted) {
            return ResponseHelper.notFound("POST_NOT_FOUND", "Post with id " + id + " not found");
        }
        return ResponseHelper.noContent();
    }
}
