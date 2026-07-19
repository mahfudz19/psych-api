package com.psycorp.psychapi.api.resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    // // ============================================================================
    // // 1.2 POST /auth/login - Authenticate user dan mendapatkan JWT token
    // // ============================================================================
    // @POST
    // @Path("/login")
    // @Operation(summary = "Login user", description = "Authenticate user dan mendapatkan JWT token")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Login successful",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "LoginSuccess",
    //                 summary = "Login successful",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "message": "Login successful",
    //                     "data": {
    //                         "user": {
    //                             "id": "usr_123",
    //                             "email": "user@example.com",
    //                             "fullName": "John Doe",
    //                             "profilePicture": "https://example.com/avatar.jpg",
    //                             "roles": ["USER"],
    //                             "organizationId": null,
    //                             "organizationRole": null,
    //                             "subscriptionTier": "free"
    //                         },
    //                         "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    //                         "refreshToken": "refresh_token_here",
    //                         "expiresIn": 3600
    //                     }
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response login(@Valid LoginRequest request) {
    //     // Verify password dan dapatkan user
    //     User user = userService.login(request.email(), request.password());
        
    //     // Generate JWT token
    //     String token = jwtService.generateToken(
    //         user.id.toHexString(),
    //         user.getEmail(),
    //         user.getRoles()
    //     );
        
    //     // Generate refresh token (simplified - dalam production bisa disimpan di database)
    //     String refreshToken = jwtService.generateTokenWithExpiry(
    //         user.id.toHexString(),
    //         user.getEmail(),
    //         user.getRoles(),
    //         86400L // 24 jam
    //     );
        
    //     // Build response
    //     Map<String, Object> responseData = Map.of(
    //         "user", user,
    //         "token", token,
    //         "refreshToken", refreshToken,
    //         "expiresIn", 3600
    //     );
        
    //     return ResponseHelper.ok(responseData, "Login successful");
    // }

    // // ============================================================================
    // // 1.3 POST /auth/refresh - Memperbarui JWT token yang expired
    // // ============================================================================
    // @POST
    // @Path("/refresh")
    // @Operation(summary = "Refresh JWT token", description = "Memperbarui JWT token yang expired menggunakan refresh token")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Token refreshed successfully",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "RefreshSuccess",
    //                 summary = "Token refreshed successfully",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "data": {
    //                         "token": "new_jwt_token_here",
    //                         "refreshToken": "new_refresh_token_here",
    //                         "expiresIn": 3600
    //                     }
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response refreshToken(@Valid RefreshTokenRequest request) {
    //     // Validate refresh token
    //     if (!jwtService.validateToken(request.refreshToken())) {
    //         return ResponseHelper.unauthorized("Invalid or expired refresh token");
    //     }
        
    //     // Extract claims dari refresh token
    //     String userId = jwtService.getUserIdFromToken(request.refreshToken());
    //     String email = jwtService.getEmailFromToken(request.refreshToken());
        
    //     // Get user dari database untuk mendapatkan roles terbaru
    //     User user = userService.getUserById(userId);
        
    //     // Generate new JWT token
    //     String newToken = jwtService.generateToken(
    //         userId,
    //         email,
    //         user.getRoles()
    //     );
        
    //     // Generate new refresh token
    //     String newRefreshToken = jwtService.generateTokenWithExpiry(
    //         userId,
    //         email,
    //         user.getRoles(),
    //         86400L // 24 jam
    //     );
        
    //     // Build response
    //     Map<String, Object> responseData = Map.of(
    //         "token", newToken,
    //         "refreshToken", newRefreshToken,
    //         "expiresIn", 3600
    //     );
        
    //     return ResponseHelper.ok(responseData, "Token refreshed successfully");
    // }

    // // ============================================================================
    // // 1.4 POST /auth/logout - Logout user dan invalidate refresh token
    // // ============================================================================
    // @POST
    // @Path("/logout")
    // @Operation(summary = "Logout user", description = "Logout user dan invalidate refresh token")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Logout successful",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "LogoutSuccess",
    //                 summary = "Logout successful",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "message": "Logout successful"
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response logout(@Valid LogoutRequest request) {
    //     // Validate refresh token
    //     if (!jwtService.validateToken(request.refreshToken())) {
    //         return ResponseHelper.unauthorized("Invalid or expired refresh token");
    //     }
        
    //     // Extract user ID dari token
    //     String userId = jwtService.getUserIdFromToken(request.refreshToken());
        
    //     // Logout logic (dalam production, invalidate token di database/redis)
    //     userService.logout(userId, request.allDevices());
        
    //     return ResponseHelper.ok("Logout successful");
    // }

    // // ============================================================================
    // // 1.5 POST /auth/switch-account - Switch context antara individual dan organization
    // // ============================================================================
    // @POST
    // @Path("/switch-account")
    // @Operation(summary = "Switch account context", description = "Switch context antara individual dan organization account")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Account context switched successfully",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "SwitchAccountSuccess",
    //                 summary = "Account context switched successfully",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "data": {
    //                         "token": "new_jwt_token_with_org_context",
    //                         "expiresIn": 3600,
    //                         "context": {
    //                             "type": "organization",
    //                             "organizationId": "org_456",
    //                             "organizationName": "PT Company Name",
    //                             "organizationRole": "owner"
    //                         }
    //                     }
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response switchAccount(@Valid SwitchAccountRequest request, @Context HttpHeaders headers) {
    //     // Get current user dari token
    //     String authHeader = headers.getHeaderString("Authorization");
    //     if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    //         return ResponseHelper.unauthorized("Authentication required");
    //     }
        
    //     // Extract token dari header (Bearer token)
    //     String token = authHeader.replace("Bearer ", "");
    //     String userId = jwtService.getUserIdFromToken(token);
        
    //     // Get user dari database
    //     User user = userService.getUserById(userId);
        
    //     // Generate new token dengan context baru
    //     String newToken;
    //     if ("organization".equals(request.accountType()) && request.organizationId() != null) {
    //         // Switch ke organization context
    //         newToken = jwtService.generateTokenWithContext(
    //             userId,
    //             user.getEmail(),
    //             user.getRoles(),
    //             request.organizationId(),
    //             user.getOrganizationRole(),
    //             user.getOrganizationName()
    //         );
    //     } else {
    //         // Switch ke individual context
    //         newToken = jwtService.generateToken(
    //             userId,
    //             user.getEmail(),
    //             user.getRoles()
    //         );
    //     }
        
    //     // Build response
    //     Map<String, Object> contextData = Map.of(
    //         "type", request.accountType(),
    //         "organizationId", request.organizationId() != null ? request.organizationId() : null,
    //         "organizationName", user.getOrganizationName(),
    //         "organizationRole", user.getOrganizationRole()
    //     );
        
    //     Map<String, Object> responseData = Map.of(
    //         "token", newToken,
    //         "expiresIn", 3600,
    //         "context", contextData
    //     );
        
    //     return ResponseHelper.ok(responseData, "Account context switched successfully");
    // }

    // // ============================================================================
    // // 1.6 POST /auth/forgot-password - Request password reset email
    // // ============================================================================
    // @POST
    // @Path("/forgot-password")
    // @Operation(summary = "Forgot password", description = "Request password reset email")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Password reset email sent (if email exists)",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "ForgotPasswordSuccess",
    //                 summary = "Password reset email sent",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "message": "Password reset email sent (if email exists)"
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response forgotPassword(@Valid ForgotPasswordRequest request) {
    //     // Send reset email (dalam production, kirim email dengan reset token)
    //     userService.forgotPassword(request.email());
        
    //     // Return success message (tanpa mengungkap apakah email terdaftar atau tidak)
    //     return ResponseHelper.ok("Password reset email sent (if email exists)");
    // }

    // // ============================================================================
    // // 1.7 POST /auth/reset-password - Reset password dengan token dari email
    // // ============================================================================
    // @POST
    // @Path("/reset-password")
    // @Operation(summary = "Reset password", description = "Reset password dengan token dari email")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Password reset successful",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "ResetPasswordSuccess",
    //                 summary = "Password reset successful",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "message": "Password reset successful"
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response resetPassword(@Valid ResetPasswordRequest request) {
    //     // Reset password dengan token
    //     userService.resetPasswordWithToken(request.token(), request.newPassword());
        
    //     return ResponseHelper.ok("Password reset successful");
    // }

    // // ============================================================================
    // // 1.8 POST /auth/change-password - Change password untuk authenticated user
    // // ============================================================================
    // @POST
    // @Path("/change-password")
    // @Operation(summary = "Change password", description = "Change password untuk authenticated user")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Password changed successfully",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "ChangePasswordSuccess",
    //                 summary = "Password changed successfully",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "message": "Password changed successfully"
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response changePassword(@Valid ChangePasswordRequest request, @Context HttpHeaders headers) {
    //     // Get current user dari token
    //     String authHeader = headers.getHeaderString("Authorization");
    //     if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    //         return ResponseHelper.unauthorized("Authentication required");
    //     }
        
    //     // Extract token dari header (Bearer token)
    //     String token = authHeader.replace("Bearer ", "");
    //     String userId = jwtService.getUserIdFromToken(token);
        
    //     // Change password
    //     userService.updatePassword(userId, request.currentPassword(), request.newPassword());
        
    //     return ResponseHelper.ok("Password changed successfully");
    // }

    // // ============================================================================
    // // 1.9 GET /auth/verify-email - Verify email dengan token dari email
    // // ============================================================================
    // @GET
    // @Path("/verify-email")
    // @Operation(summary = "Verify email", description = "Verify email dengan token dari email")
    // @Parameter(name = "token", description = "Verification token dari email", required = true)
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Email verified successfully",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "VerifyEmailSuccess",
    //                 summary = "Email verified successfully",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "message": "Email verified successfully",
    //                     "data": {
    //                         "user": {
    //                             "id": "usr_123",
    //                             "email": "user@example.com",
    //                             "emailVerified": true
    //                         }
    //                     }
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response verifyEmail(@QueryParam("token") String token) {
    //     // Verify email dengan token
    //     User user = userService.verifyEmail(token);
        
    //     Map<String, Object> responseData = Map.of(
    //         "user", Map.of(
    //             "id", user.id.toHexString(),
    //             "email", user.getEmail(),
    //             "emailVerified", true
    //         )
    //     );
        
    //     return ResponseHelper.ok(responseData, "Email verified successfully");
    // }

    // // ============================================================================
    // // 1.10 POST /auth/resend-verification - Request ulang email verifikasi
    // // ============================================================================
    // @POST
    // @Path("/resend-verification")
    // @Operation(summary = "Resend verification email", description = "Request ulang email verifikasi")
    // @APIResponse(
    //     responseCode = "200",
    //     description = "Verification email sent",
    //     content = @Content(
    //         mediaType = "application/json",
    //         schema = @Schema(implementation = ApiResponse.class),
    //         examples = {
    //             @ExampleObject(
    //                 name = "ResendVerificationSuccess",
    //                 summary = "Verification email sent",
    //                 value = """
    //                 {
    //                     "success": true,
    //                     "message": "Verification email sent"
    //                 }
    //                 """
    //             )
    //         }
    //     )
    // )
    // public Response resendVerification(@Valid ResendVerificationRequest request, @Context HttpHeaders headers) {
    //     // Get current user (optional - bisa tanpa auth jika user belum login)
    //     String userId = null;
    //     String authHeader = headers.getHeaderString("Authorization");
    //     if (authHeader != null && authHeader.startsWith("Bearer ")) {
    //         String token = authHeader.replace("Bearer ", "");
    //         userId = jwtService.getUserIdFromToken(token);
    //     }
        
    //     // Resend verification email
    //     userService.resendVerificationEmail(request.email(), userId);
        
    //     return ResponseHelper.ok("Verification email sent");
    // }
}
