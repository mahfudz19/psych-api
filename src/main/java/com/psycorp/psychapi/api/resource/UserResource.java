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

import com.psycorp.psychapi.api.dto.UserRequests.CreateUserRequest;
import com.psycorp.psychapi.api.dto.UserRequests.UpdateUserRequest;
import com.psycorp.psychapi.api.dto.UserRequests.UserListRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.common.response.PaginationMeta;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.UserService;

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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * REST API untuk User Management.
 * 
 * Endpoint ini menangani:
 * - List users dengan pagination, search, dan filter
 * - Get user by ID
 * - Get current user profile
 * - Create user (admin only)
 * - Update user profile
 * - Delete user (soft dan permanent)
 * 
 * ### Access Control
 * - **GET /users** - ADMIN only
 * - **GET /users/{id}** - ADMIN only
 * - **GET /users/me** - USER (own profile)
 * - **POST /users** - ADMIN only
 * - **PUT /users/{id}** - USER (own profile)
 * - **DELETE /users/{id}** - ADMIN only
 * - **DELETE /users/{id}/soft** - USER (own account)
 */
@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "API untuk mengelola users")
public class UserResource {

    @Inject
    UserService userService;

    // === GET ALL (dengan pagination) ===
    @GET
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Get all users with pagination, search, and filter",
        description = """
            Mengambil daftar semua users dengan fitur:
            - **Pagination**: Page number dan limit
            - **Search**: Cari di email, fullName, phone, dan bio
            - **Filter**: Filter dengan format 'field:operator:value'
            - **Sort**: Sort by field dan order (asc/desc)
            
            ### Access Control
            Endpoint ini hanya bisa diakses oleh user dengan role **ADMIN**.
            
            ### Filter Examples
            - `status:in:active,suspended` - Filter status active atau suspended
            - `createdAt:gte:2024-01-01` - Filter created after date
            - `roles:eq:USER` - Filter by role
            
            ### Sort Examples
            - `sortBy=createdAt&sortOrder=desc` - Sort by created date descending
            - `sortBy=email&sortOrder=asc` - Sort by email ascending
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
                                "roles": ["USER"],
                                "subscriptionTier": "free",
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
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't have ADMIN role"
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
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "Bearer")
    @Path("/{id}")
    @Operation(
        summary = "Get a user by ID",
        description = """
            Mengambil detail user berdasarkan ID.
            
            ### Access Control
            Endpoint ini hanya bisa diakses oleh user dengan role **ADMIN**.
            
            ### Path Parameter
            - **id**: User ID dalam format ObjectId hex string (24 characters)
            
            ### Response
            User data lengkap termasuk:
            - Personal info (email, fullName, phone, bio)
            - Account info (roles, status, subscriptionTier)
            - Organization info (jika ada)
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
                    name = "UserResponse",
                    summary = "Single user response",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "john@example.com",
                            "fullName": "John Doe",
                            "profilePicture": "https://example.com/avatar.jpg",
                            "phone": "+1234567890",
                            "bio": "Hello, I'm John!",
                            "status": "active",
                            "roles": ["USER"],
                            "accountType": "INDIVIDUAL",
                            "organizationId": null,
                            "organizationRole": null,
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
        description = "Forbidden - User doesn't have ADMIN role"
    )
    @APIResponse(
        responseCode = "404",
        description = "Not Found - User doesn't exist"
    )
    public Response getUserById(@PathParam("id") String id) {
        User user = userService.getUserById(id);
        return ResponseHelper.ok(user, "User retrieved successfully");
    }

    // === GET CURRENT USER (ME) ===
    @GET
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/me")
    @Operation(
        summary = "Get current user profile",
        description = """
            Mengambil informasi profile user yang sedang login berdasarkan JWT token.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid di Authorization header.
            
            ### How to Use
            1. Login terlebih dahulu untuk mendapatkan token
            2. Sertakan token di Authorization header: `Authorization: Bearer <token>`
            3. Endpoint akan return user data berdasarkan userId dari token
            
            ### Response
            User profile lengkap termasuk:
            - Personal info (email, fullName, profilePicture, phone, bio)
            - Account info (roles, status, accountType)
            - Organization info (jika ORGANIZATION account)
            - Subscription info (tier, expiry)
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Successful response",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "CurrentUserResponse",
                    summary = "Current user profile",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "john@example.com",
                            "fullName": "John Doe",
                            "profilePicture": "https://example.com/avatar.jpg",
                            "phone": "+1234567890",
                            "bio": "Hello!",
                            "roles": ["USER"],
                            "accountType": "INDIVIDUAL",
                            "organizationId": null,
                            "organizationRole": null,
                            "subscriptionTier": "free",
                            "status": "active",
                            "createdAt": "2024-01-01T00:00:00Z"
                        },
                        "message": "User profile retrieved successfully",
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
        description = "Unauthorized - Invalid or missing JWT token"
    )
    public Response getCurrentUser(@Context SecurityContext securityContext) {
        // Extract userId from authenticated SecurityContext
        java.security.Principal principal = securityContext.getUserPrincipal();
        
        // Check if principal is null (no token or invalid token)
        if (principal == null) {
            return ResponseHelper.unauthorized("Authentication required. Please provide a valid JWT token in Authorization header.");
        }
        
        String userId = principal.getName();
        
        // Get user by ID
        User user = userService.getUserById(userId);
        
        return ResponseHelper.ok(user, "User profile retrieved successfully");
    }

    // === CREATE ===
    @POST
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Create a new user (Admin only)",
        description = """
            Membuat user baru melalui admin panel.
            
            ### Access Control
            Endpoint ini hanya bisa diakses oleh user dengan role **ADMIN**.
            
            ### Request Requirements
            - **Email**: Harus valid dan belum terdaftar
            - **Password**: Minimal 8 karakter
            - **Full Name**: Nama lengkap user
            
            ### Note
            Untuk user registration biasa, gunakan endpoint `/api/v1/auth/register`.
            Endpoint ini digunakan untuk admin membuat user secara manual.
            """
    )
    @RequestBody(
        description = "Create user request dengan user data",
        required = true,
        content = @Content(
            schema = @Schema(implementation = CreateUserRequest.class),
            examples = {
                @ExampleObject(
                    name = "CreateUserRequest",
                    summary = "Create user request",
                    value = """
                    {
                        "email": "john@example.com",
                        "password": "Password123!",
                        "fullName": "John Doe",
                        "phone": "+1234567890",
                        "bio": "Hello, I'm John!",
                        "referredBy": "507f1f77bcf86cd799439011"
                    }
                    """
                )
            }
        )
    )
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
                            "referralCode": "JOH1721484000",
                            "createdAt": "2024-01-15T10:30:00Z",
                            "updatedAt": "2024-01-15T10:30:00Z"
                        },
                        "message": "User created successfully",
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
        description = "Forbidden - User doesn't have ADMIN role"
    )
    @APIResponse(
        responseCode = "409",
        description = "Conflict - Email already exists"
    )
    @APIResponse(
        responseCode = "422",
        description = "Validation error - Invalid input (e.g., email required, password too weak)"
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
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/{id}")
    @Operation(
        summary = "Update user profile",
        description = """
            Update profile user.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid.
            
            ### Ownership Check
            User hanya bisa update profile sendiri. ID di path parameter harus sama dengan
            userId dari JWT token. Jika tidak, akan return FORBIDDEN error.
            
            ### Updatable Fields
            - **email**: Email address (harus valid dan belum digunakan user lain)
            - **fullName**: Full name
            - **phone**: Phone number
            - **bio**: Short bio/description
            - **status**: Account status (active, inactive, suspended, deleted)
            
            ### Note
            Password tidak bisa diupdate melalui endpoint ini.
            """
    )
    @RequestBody(
        description = "Update user request dengan fields yang akan diupdate",
        required = true,
        content = @Content(
            schema = @Schema(implementation = UpdateUserRequest.class),
            examples = {
                @ExampleObject(
                    name = "UpdateUserRequest",
                    summary = "Update user request",
                    value = """
                    {
                        "email": "newemail@example.com",
                        "fullName": "John Doe Updated",
                        "phone": "+9876543210",
                        "bio": "Updated bio",
                        "status": "active"
                    }
                    """
                )
            }
        )
    )
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
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this resource (ID mismatch with token)"
    )
    @APIResponse(
        responseCode = "404",
        description = "Not Found - User doesn't exist"
    )
    @APIResponse(
        responseCode = "409",
        description = "Conflict - Email already exists"
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
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "Bearer")
    @Path("/{id}")
    @Operation(
        summary = "Delete a user permanently (Admin only)",
        description = """
            Menghapus user secara permanen dari database.
            
            ### Access Control
            Endpoint ini hanya bisa diakses oleh user dengan role **ADMIN**.
            
            ### Warning
            Ini adalah operasi destruktif yang tidak bisa di-undo.
            Untuk non-aktifkan user tanpa menghapus data, gunakan:
            - Update status menjadi 'inactive' atau 'suspended'
            - Atau soft delete endpoint `/users/{id}/soft`
            
            ### What Gets Deleted
            - User account dan semua data terkait
            - Posts yang dibuat user (atau transferred ke admin)
            - Organization membership (jika ada)
            """
    )
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
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't have ADMIN role"
    )
    @APIResponse(
        responseCode = "404",
        description = "Not Found - User doesn't exist"
    )
    public Response deleteUser(@PathParam("id") String id) {
        userService.deleteUser(id);
        return ResponseHelper.ok(null, "User deleted successfully");
    }

    // === SOFT DELETE ===
    @DELETE
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/{id}/soft")
    @Operation(
        summary = "Soft delete a user account (own account)",
        description = """
            Soft delete user account (mark as deleted tanpa menghapus data).
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid.
            
            ### Ownership Check
            User hanya bisa soft delete account sendiri. ID di path parameter harus sama dengan
            userId dari JWT token. Jika tidak, akan return FORBIDDEN error.
            
            ### What Happens
            - Status user diubah menjadi 'deleted'
            - deletedAt timestamp diisi
            - Data tetap ada di database untuk audit/backup
            - User tidak bisa login lagi
            
            ### Difference from Hard Delete
            - **Soft Delete**: Data tetap ada, bisa di-restore
            - **Hard Delete**: Data dihapus permanen (endpoint `/users/{id}`)
            """
    )
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
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this resource (ID mismatch with token)"
    )
    @APIResponse(
        responseCode = "404",
        description = "Not Found - User doesn't exist"
    )
    public Response softDeleteUser(@PathParam("id") String id) {
        User user = userService.softDeleteUser(id);
        return ResponseHelper.ok(user, "User soft deleted successfully");
    }
}
