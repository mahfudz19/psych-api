package com.psycorp.psychapi.api.resource;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.PostRequests.PostListRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.PaginationMeta;
import com.psycorp.psychapi.domain.model.Post;
import com.psycorp.psychapi.domain.service.PostService;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
@Tag(name = "Posts", description = "API untuk mengelola posts")
public class PostResource {

    @Inject
    PostService postService;

    // === GET ALL (dengan pagination) ===
    @GET
    @Operation(summary = "Get all posts with pagination, search, and filter")
    @APIResponse(
        responseCode = "200",
        description = "Successful response",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = APIResponseSchema.class),
            examples = {
                @ExampleObject(
                    name = "PostResponse",
                    summary = "Single Post Response",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "123",
                            "title": "My Post",
                            "content": "Post content",
                            "status": "published",
                            "createdAt": "2024-01-01T00:00:00Z",
                            "updatedAt": "2024-01-01T00:00:00Z"
                        },
                        "message": "Post retrieved successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    public Response getAllPosts(@BeanParam PostListRequest request) {
        List<Post> posts = postService.getAllPosts(request);
        long total = postService.getTotalPostsCount(request);
        int totalPages = (int) Math.ceil((double) total / request.limit());

        PaginationMeta meta = new PaginationMeta(
            request.page(),
            request.limit(),
            total,
            totalPages
        );
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
