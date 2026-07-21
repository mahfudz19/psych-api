package com.psycorp.psychapi.api.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.AuthRequests.LoginRequest;
import com.psycorp.psychapi.api.dto.AuthRequests.RegisterRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.AuthService;
import com.psycorp.psychapi.domain.service.AuthService.AuthenticationResult;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "API untuk authentication dan authorization")
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @PermitAll
    @Operation(
        summary = "Register user baru",
        description = """
            Membuat akun user baru (individual atau organization owner).
            
            ### Request Requirements
            - **Email**: Harus valid dan belum terdaftar
            - **Password**: Minimal 8 karakter, mengandung uppercase, lowercase, dan number
            - **Full Name**: Nama lengkap user
            - **Account Type**: INDIVIDUAL atau ORGANIZATION
            
            ### Invitation System
            Untuk organization account, bisa menggunakan:
            - **Invite Code**: Code invitation dari organization
            - **Direct Add**: Owner menambahkan member langsung dengan invitedBy dan invitedOrganizationId
            
            ### Response
            - User data yang sudah terdaftar
            - JWT token untuk langsung authenticate
            - Token expiration time
            """
    )
    @RequestBody(
        description = "Registration request dengan user data",
        required = true,
        content = @Content(
            schema = @Schema(implementation = RegisterRequest.class),
            examples = {
                @ExampleObject(
                    name = "IndividualRegistration",
                    summary = "Individual account registration",
                    value = """
                    {
                        "email": "user@example.com",
                        "password": "Password123!",
                        "fullName": "John Doe",
                        "accountType": "INDIVIDUAL"
                    }
                    """
                ),
                @ExampleObject(
                    name = "OrganizationRegistration",
                    summary = "Organization account registration with invitation",
                    value = """
                    {
                        "email": "admin@company.com",
                        "password": "Password123!",
                        "fullName": "Company Admin",
                        "accountType": "ORGANIZATION",
                        "inviteCode": "INV-ORG001-ABC"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "201",
        description = "Registration successful",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "RegistrationSuccess",
                    summary = "Registration successful",
                    value = """
                    {
                        "success": true,
                        "message": "Registration successful",
                        "data": {
                            "user": {
                                "id": "usr_123",
                                "email": "user@example.com",
                                "fullName": "John Doe",
                                "roles": ["USER"],
                                "organizationId": null,
                                "organizationRole": null,
                                "subscriptionTier": "free",
                                "status": "active",
                                "createdAt": "2026-07-18T10:00:00Z"
                            },
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "expiresIn": 604800
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
        responseCode = "400",
        description = "Validation error - Invalid input (e.g., email required, password too weak)"
    )
    @APIResponse(
        responseCode = "409",
        description = "Conflict - Email already registered"
    )
    public Response register(@Valid RegisterRequest request) {
        // Register user
        AuthenticationResult result = authService.register(
            request.email(),
            request.password(),
            request.fullName(),
            request.referralCode(),
            request.accountType(),
            request.inviteCode(),
            request.invitedBy(),
            request.invitedOrganizationId(),
            request.invitationRole()
        );
        
        return ResponseHelper.authenticationSuccess(
            result.user(),
            result.token(),
            result.expiresIn(),
            "Registration successful"
        );
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(
        summary = "Login user",
        description = """
            Authenticate user dan mendapatkan JWT token.
            
            ### Request Requirements
            - **Email**: Email yang sudah terdaftar
            - **Password**: Password yang sesuai
            
            ### Response
            - User data yang sudah authenticated
            - JWT token untuk mengakses protected endpoints
            - Token expiration time (default: 7 hari)
            """
    )
    @RequestBody(
        description = "Login request dengan email dan password",
        required = true,
        content = @Content(
            schema = @Schema(implementation = LoginRequest.class),
            examples = {
                @ExampleObject(
                    name = "LoginRequest",
                    summary = "Login dengan email dan password",
                    value = """
                    {
                        "email": "user@example.com",
                        "password": "Password123!"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Login successful",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "LoginSuccess",
                    summary = "Login successful",
                    value = """
                    {
                        "success": true,
                        "message": "Login successful",
                        "data": {
                            "user": {
                                "id": "usr_123",
                                "email": "user@example.com",
                                "fullName": "John Doe",
                                "profilePicture": "https://example.com/avatar.jpg",
                                "roles": ["USER"],
                                "organizationId": null,
                                "organizationRole": null,
                                "subscriptionTier": "free"
                            },
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "expiresIn": 604800
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
        description = "Unauthorized - Invalid credentials (wrong email or password)"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - Account suspended or inactive"
    )
    public Response login(@Valid LoginRequest request) {
        // Authenticate user
        AuthenticationResult result = authService.login(request.email(), request.password());
        
        return ResponseHelper.authenticationSuccess(
            result.user(),
            result.token(),
            result.expiresIn(),
            "Login successful"
        );
    }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Get current user",
        description = """
            Mengembalikan informasi user yang sedang login dari JWT token.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid di Authorization header.
            
            ### How to Use
            1. Login terlebih dahulu untuk mendapatkan token
            2. Sertakan token di Authorization header: `Authorization: Bearer <token>`
            3. Endpoint akan return user data berdasarkan token
            
            ### Response
            User data lengkap termasuk:
            - ID, email, fullName
            - Roles dan account type
            - Organization info (jika ada)
            - Subscription info
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "User info retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "GetUserSuccess",
                    summary = "Get current user successful",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "507f1f77bcf86cd799439011",
                            "email": "user@example.com",
                            "fullName": "John Doe",
                            "profilePicture": "https://example.com/avatar.jpg",
                            "roles": ["USER"],
                            "accountType": "INDIVIDUAL",
                            "organizationId": null,
                            "organizationRole": null,
                            "subscriptionTier": "free",
                            "status": "active",
                            "createdAt": "2026-07-18T10:00:00Z"
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
        // Check if user is authenticated
        if (securityContext.getUserPrincipal() == null) {
            return ResponseHelper.unauthorized("Authentication required");
        }
        
        // Get user from database using userId dari SecurityContext
        // userId sudah di-extract dari JWT token oleh JwtAuthenticationFilter
        User user = authService.getCurrentUserFromToken(securityContext);
        
        return ResponseHelper.ok(user);
    }

    @POST
    @Path("/logout")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Logout user",
        description = """
            Logout user dan invalidate session.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid di Authorization header.
            
            ### What Happens
            - Token akan di-invalidate (jika ada token blacklist mechanism)
            - Client harus login ulang untuk mendapatkan token baru
            
            ### Note
            Saat ini logout bersifat client-side (hapus token dari client).
            Server-side token invalidation akan diimplementasikan di masa depan.
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Logout successful",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "LogoutSuccess",
                    summary = "Logout successful",
                    value = """
                    {
                        "success": true,
                        "message": "Logout successful",
                        "data": null,
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
    public Response logout(@Context SecurityContext securityContext) {
        // Extract userId jika token valid (untuk audit trail jika perlu)
        String userId = null;
        if (securityContext.getUserPrincipal() != null) {
            userId = securityContext.getUserPrincipal().getName();
        }
        
        // Server-side cleanup (optional, untuk audit trail)
        if (userId != null) {
            authService.logout(userId);
        }
        
        return ResponseHelper.logoutSuccess("Logout successful");
    }
}
