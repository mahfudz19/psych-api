package com.psycorp.psychapi.api.resource;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.PostRequests.CreatePostRequest;
import com.psycorp.psychapi.api.dto.PostRequests.PostListRequest;
import com.psycorp.psychapi.api.dto.PostRequests.UpdatePostRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.common.response.PaginationMeta;
import com.psycorp.psychapi.domain.model.Post;
import com.psycorp.psychapi.domain.service.PostService;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Posts", description = "API untuk mengelola posts")
public class PostResource {

    @Inject
    PostService postService;

    @GET
    @PermitAll
    @Operation(
        summary = "Get all posts with pagination, search, and filter",
        description = """
            Mengambil daftar semua posts yang published dengan fitur:
            - **Pagination**: Page number dan limit
            - **Search**: Cari di title dan content fields (case-insensitive)
            - **Filter**: Filter dengan format 'field:operator:value'
            - **Sort**: Sort by field dan order (asc/desc)
            
            ### Public Access
            Endpoint ini bisa diakses tanpa authentication.
            Hanya posts dengan status 'published' yang akan ditampilkan.
            
            ### Filter Examples
            - `status:in:draft,published` - Filter by status
            - `createdAt:gte:2024-01-01` - Filter created after date
            - `title:contains:mental health` - Search in title
            
            ### Sort Examples
            - `sortBy=createdAt&sortOrder=desc` - Sort by created date descending
            - `sortBy=title&sortOrder=asc` - Sort by title ascending
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Successful response",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = APIResponseSchema.class),
            examples = {
                @ExampleObject(
                    name = "PostListResponse",
                    summary = "List of posts with pagination",
                    value = """
                    {
                        "success": true,
                        "data": [
                            {
                                "id": "507f1f77bcf86cd799439011",
                                "title": "Understanding Mental Health",
                                "content": "Mental health is an important aspect of overall well-being...",
                                "status": "published",
                                "ownerId": "507f1f77bcf86cd799439000",
                                "createdAt": "2024-01-01T00:00:00Z",
                                "updatedAt": "2024-01-15T10:30:00Z"
                            }
                        ],
                        "message": "Posts retrieved successfully",
                        "meta": {
                            "page": 1,
                            "limit": 10,
                            "total": 100,
                            "totalPages": 10
                        },
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
    @PermitAll
    @Path("/{id}")
    @Operation(
        summary = "Get a post by ID",
        description = """
            Mengambil detail post berdasarkan ID.
            
            ### Public Access
            Endpoint ini bisa diakses tanpa authentication.
            Hanya posts dengan status 'published' yang bisa diakses.
            
            ### Path Parameter
            - **id**: Post ID dalam format ObjectId hex string (24 characters)
            
            ### Response
            Post data lengkap termasuk:
            - Title dan content
            - Status (draft/published)
            - Owner ID
            - Timestamps (createdAt, updatedAt)
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Successful response",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = APIResponseSchema.class),
            examples = {
                @ExampleObject(
                    name = "PostResponse",
                    summary = "Single post response",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "title": "Understanding Mental Health",
                            "content": "Mental health is an important aspect of overall well-being...",
                            "status": "published",
                            "ownerId": "507f1f77bcf86cd799439000",
                            "createdAt": "2024-01-01T00:00:00Z",
                            "updatedAt": "2024-01-15T10:30:00Z"
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
    @APIResponse(
        responseCode = "404",
        description = "Not Found - Post doesn't exist or is draft"
    )
    public Response getPostById(@PathParam("id") String id) {
        Post post = postService.getPostById(id);
        return ResponseHelper.ok(post, "Post retrieved successfully");
    }

    @POST
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Create a new post",
        description = """
            Membuat post baru.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid di Authorization header.
            
            ### Request Requirements
            - **Title**: Judul post (required, tidak boleh kosong)
            - **Content**: Isi post (required, tidak boleh kosong)
            - **Status**: draft atau published (optional, default: draft)
            
            ### Post Status
            - **draft**: Post belum dipublish, hanya visible untuk owner
            - **published**: Post published, visible untuk semua user
            
            ### Owner Assignment
            ownerId akan otomatis diisi berdasarkan userId dari JWT token.
            """
    )
    @RequestBody(
        description = "Create post request dengan title, content, dan status",
        required = true,
        content = @Content(
            schema = @Schema(implementation = CreatePostRequest.class),
            examples = {
                @ExampleObject(
                    name = "CreatePostRequest",
                    summary = "Create post as draft",
                    value = """
                    {
                        "title": "My New Post",
                        "content": "This is the content of my new post...",
                        "status": "draft"
                    }
                    """
                ),
                @ExampleObject(
                    name = "CreatePostPublished",
                    summary = "Create post and publish immediately",
                    value = """
                    {
                        "title": "Understanding Mental Health",
                        "content": "Mental health is an important aspect of overall well-being...",
                        "status": "published"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "201",
        description = "Post created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "PostCreated",
                    summary = "Post created successfully",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "title": "My New Post",
                            "content": "Post content here",
                            "status": "draft",
                            "ownerId": "507f1f77bcf86cd799439000",
                            "createdAt": "2024-01-15T10:30:00Z",
                            "updatedAt": "2024-01-15T10:30:00Z"
                        },
                        "message": "Post created successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    @APIResponse(
        responseCode = "422",
        description = "Validation error - Invalid input (e.g., title required, content required)"
    )
    public Response createPost(@Valid CreatePostRequest request){
        Post post = postService.createPost(request.title(), request.content(), request.status());
        return ResponseHelper.created(post, "Post created successfully");
    }

    // === UPDATE ===
    @PUT
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/{id}")
    @Operation(
        summary = "Update a post",
        description = """
            Update post yang sudah ada.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid.
            
            ### Ownership Check
            User hanya bisa update post sendiri. ownerId dari post harus sama dengan
            userId dari JWT token. Jika tidak, akan return FORBIDDEN error.
            
            ### Updatable Fields
            - **title**: Post title
            - **content**: Post content
            - **status**: draft atau published
            
            ### Note
            Semua field adalah optional. Hanya field yang diisi yang akan diupdate.
            """
    )
    @RequestBody(
        description = "Update post request dengan fields yang akan diupdate",
        required = true,
        content = @Content(
            schema = @Schema(implementation = UpdatePostRequest.class),
            examples = {
                @ExampleObject(
                    name = "UpdatePostRequest",
                    summary = "Update post title and content",
                    value = """
                    {
                        "title": "Updated Post Title",
                        "content": "Updated post content here...",
                        "status": "published"
                    }
                    """
                ),
                @ExampleObject(
                    name = "UpdatePostStatusOnly",
                    summary = "Publish a draft post",
                    value = """
                    {
                        "status": "published"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Post updated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "PostUpdated",
                    summary = "Post updated successfully",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "title": "Updated Post Title",
                            "content": "Updated post content here...",
                            "status": "published",
                            "ownerId": "507f1f77bcf86cd799439000",
                            "createdAt": "2024-01-15T10:30:00Z",
                            "updatedAt": "2024-01-15T12:00:00Z"
                        },
                        "message": "Post updated successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this post (ownerId mismatch with token)"
    )
    @APIResponse(
        responseCode = "404",
        description = "Not Found - Post doesn't exist"
    )
    public Response updatePost(@PathParam("id") String id, @Valid UpdatePostRequest request) {
        Post post = postService.updatePost(id, request.title(), request.content(), request.status());
        return ResponseHelper.ok(post, "Post updated successfully");
    }

    // === DELETE ===
    @DELETE
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/{id}")
    @Operation(
        summary = "Delete a post",
        description = """
            Menghapus post secara permanen.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid.
            
            ### Ownership Check
            User hanya bisa delete post sendiri. ownerId dari post harus sama dengan
            userId dari JWT token. Jika tidak, akan return FORBIDDEN error.
            
            ### Warning
            Ini adalah operasi destruktif yang tidak bisa di-undo.
            Pastikan post yang akan dihapus adalah post yang benar.
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Post deleted successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "PostDeleted",
                    summary = "Post deleted successfully",
                    value = """
                    {
                        "success": true,
                        "data": null,
                        "message": "Post deleted successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this post (ownerId mismatch with token)"
    )
    @APIResponse(
        responseCode = "404",
        description = "Not Found - Post doesn't exist"
    )
    public Response deletePost(@PathParam("id") String id) {
        postService.deletePost(id);
        return ResponseHelper.ok(null, "Post deleted successfully");
    }
}
