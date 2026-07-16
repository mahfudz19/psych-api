package com.psycorp.psychapi.api.resource;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.UserRequests.CreateUserRequest;
import com.psycorp.psychapi.api.dto.UserRequests.UpdateUserRequest;
import com.psycorp.psychapi.api.dto.UserRequests.UserListRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.common.response.PaginationMeta;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.UserService;

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

/**
 * REST API untuk User operations.
 * Mendukung CRUD operations dengan pagination, search, filter, dan explicit delete flag.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "API untuk mengelola users")
public class UserResource {

    @Inject
    UserService userService;

    // === GET ALL (dengan pagination) ===
    @GET
    @Operation(summary = "Get all users with pagination, search, and filter")
    @APIResponse(
        responseCode = "200",
        description = "Successful response",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = APIResponseSchema.class),
            examples = {
                @ExampleObject(
                    name = "UserListResponse",
                    summary = "List of users with pagination",
                    value = """
                    {
                        "success": true,
                        "data": [
                            {
                                "id": "507f1f77bcf86cd799439011",
                                "email": "john@example.com",
                                "fullName": "John Doe",
                                "phone": "+1234567890",
                                "bio": "Hello, I'm John!",
                                "status": "active",
                                "createdAt": "2024-01-01T00:00:00Z",
                                "updatedAt": "2024-01-15T10:30:00Z"
                            }
                        ],
                        "message": "Users retrieved successfully",
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
    public Response getAllUsers(@BeanParam UserListRequest request) {
        List<User> users = userService.getAllUsers(request.search(), request.filter(), request.sortBy(), request.sortOrder(), request.page(), request.limit());
        long total = userService.getTotalUsersCount(request.search(), request.filter());
        int totalPages = (int) Math.ceil((double) total / request.limit());

        PaginationMeta meta = new PaginationMeta(
            request.page(),
            request.limit(),
            total,
            totalPages
        );
        return ResponseHelper.ok(users, "Users retrieved successfully", meta);
    }

    // === GET BY ID ===
    @GET
    @Path("/{id}")
    @Operation(summary = "Get a user by ID")
    @APIResponse(
        responseCode = "200",
        description = "Successful response",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = APIResponseSchema.class),
            examples = {
                @ExampleObject(
                    name = "UserResponse",
                    summary = "Single user response",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "john@example.com",
                            "fullName": "John Doe",
                            "phone": "+1234567890",
                            "bio": "Hello, I'm John!",
                            "status": "active",
                            "roles": ["USER"],
                            "subscriptionTier": "free",
                            "createdAt": "2024-01-01T00:00:00Z",
                            "updatedAt": "2024-01-15T10:30:00Z"
                        },
                        "message": "User retrieved successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                ),
                @ExampleObject(
                    name = "UserNotFound",
                    summary = "User not found",
                    value = """
                    {
                        "success": false,
                        "data": null,
                        "message": "User with id 507f1f77bcf86cd799439011 not found",
                        "meta": null,
                        "code": "USER_NOT_FOUND",
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    public Response getUserById(@PathParam("id") String id) {
        User user = userService.getUserById(id);
        return ResponseHelper.ok(user, "User retrieved successfully");
    }

    // === CREATE ===
    @POST
    @Operation(summary = "Create a new user")
    @APIResponse(
        responseCode = "201",
        description = "User created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "UserCreated",
                    summary = "User created successfully",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "john@example.com",
                            "fullName": "John Doe",
                            "phone": "+1234567890",
                            "bio": "Hello, I'm John!",
                            "provider": "local",
                            "roles": ["USER"],
                            "status": "active",
                            "subscriptionTier": "free",
                            "createdAt": "2024-01-15T10:30:00Z",
                            "updatedAt": "2024-01-15T10:30:00Z"
                        },
                        "message": "User created successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                ),
                @ExampleObject(
                    name = "ValidationError",
                    summary = "Validation error",
                    value = """
                    {
                        "success": false,
                        "data": null,
                        "message": "Email is required, Password must be at least 8 characters",
                        "meta": null,
                        "code": "VALIDATION_ERROR",
                        "errors": [
                            {"field": "email", "message": "Email is required"},
                            {"field": "password", "message": "Password must be at least 8 characters"}
                        ]
                    }
                    """
                )
            }
        )
    )
    public Response createUser(@Valid CreateUserRequest request){
        User user = userService.createUser(
            request.email(), 
            request.password(), 
            request.fullName(), 
            request.phone(), 
            request.bio(), 
            request.referredBy()
        );
        return ResponseHelper.created(user, "User created successfully");
    }

    // === UPDATE (Typed Parameters) ===
    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a user with typed parameters")
    @APIResponse(
        responseCode = "200",
        description = "User updated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "UserUpdated",
                    summary = "User updated successfully",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "newemail@example.com",
                            "fullName": "John Doe Updated",
                            "phone": "+9876543210",
                            "bio": "Updated bio",
                            "status": "active",
                            "updatedAt": "2024-01-15T10:30:00Z"
                        },
                        "message": "User updated successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    public Response updateUser(@PathParam("id") String id, @Valid UpdateUserRequest request) {
        User user = userService.updateUser(
            id,
            request.email(),
            request.fullName(),
            request.phone(),
            request.bio(),
            request.status()
        );
        return ResponseHelper.ok(user, "User updated successfully");
    }

    // === DELETE ===
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a user permanently")
    @APIResponse(
        responseCode = "200",
        description = "User deleted successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "UserDeleted",
                    summary = "User deleted successfully",
                    value = """
                    {
                        "success": true,
                        "data": null,
                        "message": "User deleted successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    public Response deleteUser(@PathParam("id") String id) {
        userService.deleteUser(id);
        return ResponseHelper.ok(null, "User deleted successfully");
    }

    // === SOFT DELETE ===
    @DELETE
    @Path("/{id}/soft")
    @Operation(summary = "Soft delete a user (mark as deleted)")
    @APIResponse(
        responseCode = "200",
        description = "User soft deleted successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "UserSoftDeleted",
                    summary = "User soft deleted successfully",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "john@example.com",
                            "fullName": "John Doe",
                            "status": "deleted",
                            "deletedAt": "2024-01-15T10:30:00Z",
                            "updatedAt": "2024-01-15T10:30:00Z"
                        },
                        "message": "User soft deleted successfully",
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    public Response softDeleteUser(@PathParam("id") String id) {
        User user = userService.softDeleteUser(id);
        return ResponseHelper.ok(user, "User soft deleted successfully");
    }
}
