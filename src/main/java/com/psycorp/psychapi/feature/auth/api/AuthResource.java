package com.psycorp.psychapi.feature.auth.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.feature.auth.api.dto.request.AuthRequest;
import com.psycorp.psychapi.feature.auth.api.dto.request.LogoutRequest;
import com.psycorp.psychapi.feature.auth.api.dto.request.RegisterRequest;
import com.psycorp.psychapi.feature.auth.api.dto.request.ResendVerifyEmailRequest;
import com.psycorp.psychapi.feature.auth.api.dto.request.SessionListRequest;
import com.psycorp.psychapi.feature.auth.api.dto.request.VerifyEmailRequest;
import com.psycorp.psychapi.feature.auth.api.dto.response.LoginResponse;
import com.psycorp.psychapi.feature.auth.api.dto.response.SessionResponse;
import com.psycorp.psychapi.feature.auth.api.dto.response.UserInfoResponse;
import com.psycorp.psychapi.feature.auth.model.DeviceInfo;
import com.psycorp.psychapi.feature.auth.service.AuthService;
import com.psycorp.psychapi.feature.auth.service.DeviceDetectionService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;
import com.psycorp.psychapi.shared.response.ApiResponse;
import com.psycorp.psychapi.shared.response.CookieHelper;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;

import io.quarkus.security.Authenticated;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * REST API endpoints untuk authentication dan authorization.
 * Mendukung Bearer token authentication menggunakan JWT.
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "API endpoints untuk authentication dan authorization")
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT Bearer token authentication")
public class AuthResource {

    private final AuthService authService;

    @Inject
    DeviceDetectionService deviceService;
    
    @Context
    HttpServerRequest httpServerRequest;

    @Inject
    CookieHelper cookieHelper;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/register")
    @Operation(summary = "Register user baru", description = RegisterRequest.DESCRIPTION)
    @APIResponse(responseCode = "201", description = "Registration successful", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed or email already exists", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response register(@Valid RegisterRequest request, @Context HttpHeaders headers) {
        DeviceInfo deviceInfo = deviceService.extractDeviceInfo(headers, httpServerRequest);

        UserInfoResponse response = authService.register(
            request.email(),
            request.password(),
            request.fullName(),
            request.referralCode(),
            request.accountType(),
            request.inviteCode(),
            request.invitedBy(),
            request.invitedOrganizationId(),
            request.invitationRole(),
            deviceInfo
        );

        return ResponseHelper.created(response, "Registration successful");
    }

    @POST
    @Path("/resend-verify-email")
    @Operation(summary = "Resend verification email", description = ResendVerifyEmailRequest.DESCRIPTION)
    @APIResponse(responseCode = "201", description = "Registration successful", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed or email already exists", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response resendVerificationEmail(@Valid ResendVerifyEmailRequest request, @Context HttpHeaders headers) {
        UserInfoResponse response = authService.resendVerificationEmail(request.email());

        return ResponseHelper.created(response, "Resend successful");
    }

    @POST
    @Path("/verify-email")
    @Operation(summary = "Verify email", description = VerifyEmailRequest.DESCRIPTION)
    @APIResponse(responseCode = "201", description = "Registration successful", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed or email already exists", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response verifyEmail(@Valid VerifyEmailRequest request, @Context HttpHeaders headers) {
        DeviceInfo deviceInfo = deviceService.extractDeviceInfo(headers, httpServerRequest);

        LoginResponse response = authService.verifyEmail(
            request.email(),
            request.plainToken(),
            deviceInfo
        );

        List<NewCookie> cookies = List.of(cookieHelper.createRefreshCookie(response.refreshToken()));
        return ResponseHelper.created(response, "Verification successful", cookies);
    }

    @POST
    @Path("/login")
    public Response login(@Valid AuthRequest request, 
                        @CookieParam("refresh_token") String refreshCookie, 
                        @Context HttpHeaders headers) {
        
        DeviceInfo deviceInfo = deviceService.extractDeviceInfo(headers, httpServerRequest);
        List<NewCookie> cookies = new ArrayList<>();

        // Priority 1: Refresh dari cookie (jika ada)
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            try {
                LoginResponse response = authService.refreshToken(refreshCookie, deviceInfo);
                cookies.add(cookieHelper.createRefreshCookie(response.refreshToken()));
                return ResponseHelper.ok(response, "Refresh successful", cookies);
            } catch (ValidationException e) {
                cookies.add(cookieHelper.deleteCookie());
            }
        }

        // Priority 2: Login dengan email + password
        if (request.email() != null && request.password() != null) {
            try {
                LoginResponse response = authService.login(request.email(), request.password(), deviceInfo);
                cookies.add(cookieHelper.createRefreshCookie(response.refreshToken()));
                return ResponseHelper.ok(response, "Login successful", cookies);
            } catch (ValidationException e) {
                // Login juga gagal - return error
                throw e;
            }
        }

        // Fallback: Tidak ada valid credentials sama sekali
        throw new BadRequestException("Invalid request. Email and password are required.");
    }

    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "Get current user info", description = "Mengambil informasi user yang sedang login berdasarkan JWT token")
    @APIResponse(responseCode = "200", description = "User info retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - User not found or inactive", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response me(
        @Parameter(description = "HTTP Headers dengan Authorization header", required = true)
        @Context ContainerRequestContext requestContext
    ) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }

        return ResponseHelper.ok(UserInfoResponse.from(user), "User info retrieved successfully");
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Logout user dan revoke refresh tokens", description = LogoutRequest.DESCRIPTION)
    @Authenticated
    @APIResponse(responseCode = "200", description = "Logout successful", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid token ID format", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response logout(
        @Valid LogoutRequest request,
        @Parameter(description = "HTTP Headers dengan Authorization header", required = true)
        @Context ContainerRequestContext requestContext,
        @CookieParam("refresh_token") String refreshCookie
    ) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }

        int revokedCount = authService.logout(user.getId(), request.refreshTokenId(), refreshCookie);

        NewCookie deleteCookie = cookieHelper.deleteCookie();

        return ResponseHelper.ok(
            Map.of("revokedCount", revokedCount),
            "Logout successful",
            List.of(deleteCookie)
        );
    }

    @GET
    @Path("/sessions")
    @Operation(
        summary = "Get all user sessions",
        description = "Mengambil daftar semua sesi login user dengan pagination."
    )
    @Authenticated
    @APIResponse(
        responseCode = "200", 
        description = "Sessions retrieved successfully", 
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @APIResponse(
        responseCode = "401", 
        description = "Unauthorized", 
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
    )
    public Response getSessions(
        @BeanParam SessionListRequest request,
        @Parameter(description = "HTTP Headers dengan Authorization header", required = true)
        @Context ContainerRequestContext requestContext
    ) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        
        // Get paginated sessions
        List<SessionResponse> sessions = authService.getSessions(
            user.getId(),
            request.page(),
            request.limit(),
            request.sortBy(),
            request.sortOrder(),
            request.status()
        );

        // Get total count untuk pagination meta
        long total = authService.getSessionsCount(user.getId(), request.status());

        // Build pagination meta
        PaginationMeta meta = PaginationMeta.of(request.page(), request.limit(), total);

        // Return response
        return ResponseHelper.ok(sessions, "Sessions retrieved successfully", meta);
    }
}
