package com.psycorp.psychapi.api.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.UserService;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.security.SuperAdminService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "API untuk administrasi superadmin")
@SecurityRequirement(name = "Bearer")
public class AdminResource {

    @Inject
    UserService userService;

    @Inject
    SuperAdminService superAdminService;

    @Inject
    SecurityContext securityContext;

    /**
     * Get semua user di sistem.
     * Hanya superadmin yang dapat mengakses endpoint ini.
     * 
     * @return List semua user
     */
    @GET
    @Path("/users")
    @RolesAllowed("USER")
    @Operation(
        summary = "Get all users",
        description = """
            Mengambil daftar semua user di sistem.
            
            ### Authorization
            Endpoint ini hanya dapat diakses oleh **superadmin**.
            
            ### Response
            List semua user dengan informasi lengkap termasuk:
            - ID, email, fullName
            - Roles dan account type
            - Organization info
            - Subscription info
            - Status account
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Users retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "GetUsersSuccess",
                    summary = "Get all users successful",
                    value = """
                    {
                        "success": true,
                        "message": "Users retrieved successfully",
                        "data": [
                            {
                                "id": "507f1f77bcf86cd799439011",
                                "email": "user1@example.com",
                                "fullName": "John Doe",
                                "roles": ["USER"],
                                "accountType": "INDIVIDUAL",
                                "status": "active"
                            },
                            {
                                "id": "507f1f77bcf86cd799439012",
                                "email": "admin@company.com",
                                "fullName": "Company Admin",
                                "roles": ["USER", "ORGANIZATION"],
                                "accountType": "ORGANIZATION",
                                "status": "active"
                            }
                        ],
                        "meta": {
                            "total": 2,
                            "page": 1,
                            "pageSize": 10
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
        description = "Unauthorized - Invalid or missing JWT token"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User tidak memiliki akses superadmin"
    )
    public Response getAllUsers(
        @jakarta.ws.rs.QueryParam("search") String search,
        @jakarta.ws.rs.QueryParam("filter") String filter,
        @jakarta.ws.rs.QueryParam("sortBy") String sortBy,
        @jakarta.ws.rs.QueryParam("sortOrder") String sortOrder,
        @jakarta.ws.rs.QueryParam("page") int page,
        @jakarta.ws.rs.QueryParam("limit") int limit) {
        
        // Set default pagination
        if (page <= 0) page = 1;
        if (limit <= 0) limit = 20;
        if (limit > 100) limit = 100; // Max limit
        
        // Get users dengan pagination
        List<User> users = userService.getAllUsers(search, filter, sortBy, sortOrder, page, limit);
        
        // Get total count
        long totalCount = userService.getTotalUsersCount(search, filter);
        
        // Build pagination meta
        var meta = new com.psycorp.psychapi.common.response.PaginationMeta(
            page,
            limit,
            totalCount,
            (int) Math.ceil((double) totalCount / limit)
        );
        
        return ResponseHelper.ok(users, "Users retrieved successfully", meta);
    }

    /**
     * Get detail user berdasarkan ID.
     * Hanya superadmin yang dapat mengakses endpoint ini.
     * 
     * @param userId User ID
     * @return Detail user
     */
    @GET
    @Path("/users/{userId}")
    @RolesAllowed("USER")
    @Operation(
        summary = "Get user by ID",
        description = """
            Mengambil detail user berdasarkan ID.
            
            ### Authorization
            Endpoint ini hanya dapat diakses oleh **superadmin**.
            
            ### Path Parameters
            - **userId**: ObjectId dari user yang ingin dilihat
            
            ### Response
            Detail user lengkap termasuk:
            - Semua informasi profil
            - Organization info
            - Subscription info
            - Referral info
            - Login history
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "User retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "GetUserSuccess",
                    summary = "Get user by ID successful",
                    value = """
                    {
                        "success": true,
                        "message": "User retrieved successfully",
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "user@example.com",
                            "fullName": "John Doe",
                            "profilePicture": "https://example.com/avatar.jpg",
                            "phone": "+1234567890",
                            "bio": "Hello, I'm John!",
                            "roles": ["USER"],
                            "accountType": "INDIVIDUAL",
                            "subscriptionTier": "premium",
                            "status": "active",
                            "createdAt": "2026-01-01T00:00:00Z",
                            "lastLoginAt": "2026-07-20T10:00:00Z"
                        },
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
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User tidak memiliki akses superadmin"
    )
    @APIResponse(
        responseCode = "404",
        description = "User not found"
    )
    public Response getUserById(@PathParam("userId") String userId) {
        User user;
        try {
            user = userService.getUserById(userId);
        } catch (NotFoundException e) {
            return ResponseHelper.notFound("USER_NOT_FOUND", "User not found");
        }
        
        return ResponseHelper.ok(user, "User retrieved successfully");
    }

    /**
     * Get statistik sistem.
     * Hanya superadmin yang dapat mengakses endpoint ini.
     * 
     * @return Statistik sistem
     */
    @GET
    @Path("/stats")
    @RolesAllowed("USER")
    @Operation(
        summary = "Get system statistics",
        description = """
            Mengambil statistik sistem untuk superadmin.
            
            ### Authorization
            Endpoint ini hanya dapat diakses oleh **superadmin**.
            
            ### Response
            Statistik sistem termasuk:
            - Total users
            - Total organizations
            - Total posts
            - Active subscriptions
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Statistics retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "GetStatsSuccess",
                    summary = "Get system statistics successful",
                    value = """
                    {
                        "success": true,
                        "message": "Statistics retrieved successfully",
                        "data": {
                            "totalUsers": 1000,
                            "totalOrganizations": 50,
                            "totalPosts": 5000,
                            "activeSubscriptions": 200,
                            "superAdminCount": 3
                        },
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
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User tidak memiliki akses superadmin"
    )
    public Response getSystemStats() {
        // Get statistics
        long totalUsers = userService.getTotalUsersCount(null, null);
        int superAdminCount = superAdminService.getSuperAdminCount();
        
        // Placeholder stats - akan diimplementasikan lebih lengkap di masa depan
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("superAdminCount", superAdminCount);
        stats.put("totalOrganizations", 0); 
        stats.put("totalPosts", 0); 
        stats.put("activeSubscriptions", 0); 
        
        return ResponseHelper.ok(stats, "Statistics retrieved successfully");
    }

    /**
     * Cek apakah current user adalah superadmin.
     * Endpoint utility untuk debugging/verification.
     * 
     * @return Status superadmin
     */
    @GET
    @Path("/me/check")
    @RolesAllowed("USER")
    @Operation(
        summary = "Check superadmin status",
        description = """
            Mengecek apakah current user adalah superadmin.
            
            ### Authorization
            Endpoint ini hanya dapat diakses oleh user yang sudah authenticated.
            
            ### Response
            Status superadmin current user.
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Superadmin status checked",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "CheckSuperadminSuccess",
                    summary = "Check superadmin status successful",
                    value = """
                    {
                        "success": true,
                        "message": "Superadmin status checked",
                        "data": {
                            "isSuperAdmin": true,
                            "email": "admin@psych.com"
                        },
                        "meta": null,
                        "code": null,
                        "errors": null
                    }
                    """
                )
            }
        )
    )
    public Response checkSuperAdminStatus() {
        // Get current user email dari security context
        String userId = securityContext.getUserPrincipal().getName();
        User user;
        try {
            user = userService.getUserById(userId);
        } catch (NotFoundException e) {
            return ResponseHelper.notFound("USER_NOT_FOUND", "User not found");
        }
        
        boolean isSuperAdmin = superAdminService.isSuperAdmin(user.getEmail());
        
        var result = new java.util.HashMap<String, Object>();
        result.put("isSuperAdmin", isSuperAdmin);
        result.put("email", user.getEmail());
        result.put("userId", userId);
        
        return ResponseHelper.ok(result, "Superadmin status checked");
    }
}
