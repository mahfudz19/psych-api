package com.psycorp.psychapi.api.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.AuthRequests.LoginRequest;
import com.psycorp.psychapi.api.dto.AuthRequests.RegisterRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.config.JwtConfig;
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

    @Inject
    JwtConfig jwtConfig;

    @POST
    @Path("/register")
    @PermitAll
    @Operation(summary = "Register user baru", description = "Membuat akun user baru (individual atau organization owner)")
    @APIResponse(
        responseCode = "201",
        description = "Registration successful",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = com.psycorp.psychapi.common.response.ApiResponse.class),
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
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                        }
                    }
                    """
                )
            }
        )
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
            jwtConfig.expiresIn(), 
            "Registration successful"
        );
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Login user", description = "Authenticate user dan mendapatkan JWT token")
    @APIResponse(
        responseCode = "200",
        description = "Login successful",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = com.psycorp.psychapi.common.response.ApiResponse.class),
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
                            "expiresIn": 3600
                        }
                    }
                    """
                )
            }
        )
    )
    public Response login(@Valid LoginRequest request) {
        // Authenticate user
        AuthenticationResult result = authService.login(request.email(), request.password());
        
        return ResponseHelper.authenticationSuccess(
            result.user(),
            result.token(),
            jwtConfig.expiresIn(),
            "Login successful"
        );
    }

    @GET
    @Path("/me")
    @PermitAll
    @Operation(summary = "Get current user", description = "Mengembalikan informasi user yang sedang login dari JWT token")
    @APIResponse(
        responseCode = "200",
        description = "User info retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = com.psycorp.psychapi.common.response.ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "GetUserSuccess",
                    summary = "Get current user successful",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "id": "usr_123",
                            "email": "user@example.com",
                            "roles": ["USER"]
                        }
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Invalid or missing token"
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
    @Operation(summary = "Logout user", description = "Logout user dan invalidate session")
    @APIResponse(
        responseCode = "200",
        description = "Logout successful",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = com.psycorp.psychapi.common.response.ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "LogoutSuccess",
                    summary = "Logout successful",
                    value = """
                    {
                        "success": true,
                        "message": "Logout successful"
                    }
                    """
                )
            }
        )
    )
    public Response logout(@Context SecurityContext securityContext) {
        // Extract userId dari authenticated context (JWT token)
        String userId = securityContext.getUserPrincipal().getName();
        
        // Logout logic
        authService.logout(userId);
        
        return ResponseHelper.ok("Logout successful");
    }
}
