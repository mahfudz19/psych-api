package com.psycorp.psychapi.api.resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.AuthRequests.LoginRequest;
import com.psycorp.psychapi.api.dto.AuthRequests.RegisterRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.UserService;
import com.psycorp.psychapi.infrastructure.security.JwtService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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
    UserService userService;

    @Inject
    JwtService jwtService;
    
    @ConfigProperty(name = "quarkus.smallrye.jwt.token-expires-in")
    Long tokenExpiresIn;

    @POST
    @Path("/register")
    @Operation(summary = "Register user baru", description = "Membuat akun user baru (individual atau organization owner)")
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
        User user = userService.register(
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
        
        // Generate JWT token berdasarkan account type
        String token = jwtService.generateToken(user, request.accountType());
        
        return ResponseHelper.authenticationSuccess(user, token, tokenExpiresIn, "Registration successful");
    }

    // ============================================================================
    // 1.2 POST /auth/login - Authenticate user dan mendapatkan JWT token
    // ============================================================================
    @POST
    @Path("/login")
    @Operation(summary = "Login user", description = "Authenticate user dan mendapatkan JWT token")
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
        User user = userService.login(request.email(), request.password());
        
        // Generate JWT token with accountType from model (direct usage)
        String token = jwtService.generateToken(user, user.getAccountType());
        
        // Return consistent response (same format as register)
        return ResponseHelper.authenticationSuccess(
            user,
            token,
            tokenExpiresIn,
            "Login successful"
        );
    }

    // ============================================================================
    // 1.4 POST /auth/logout - Logout user
    // ============================================================================
    @POST
    @Path("/logout")
    @Operation(summary = "Logout user", description = "Logout user dan invalidate session")
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
        userService.logout(userId);
        
        return ResponseHelper.ok("Logout successful");
    }
}
