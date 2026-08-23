package com.psycorp.psychapi.feature.referral.api;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.feature.referral.api.dto.request.RegenerateReferralRequest;
import com.psycorp.psychapi.feature.referral.api.dto.request.ValidateReferralRequest;
import com.psycorp.psychapi.feature.referral.api.dto.response.ReferralCodeResponse;
import com.psycorp.psychapi.feature.referral.api.dto.response.ReferralHistoryEntryResponse;
import com.psycorp.psychapi.feature.referral.api.dto.response.ReferralHistoryResponse;
import com.psycorp.psychapi.feature.referral.api.dto.response.ReferralStatsResponse;
import com.psycorp.psychapi.feature.referral.api.dto.response.ReferrerInfoResponse;
import com.psycorp.psychapi.feature.referral.api.dto.response.ValidateReferralResponse;
import com.psycorp.psychapi.feature.referral.service.ReferralService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.shared.response.ResponseHelper;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/referral")
@Authenticated
@RolesAllowed("USER")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Referral Management", description = "API untuk mengelola referral codes")
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT Bearer token authentication")
public class ReferralResource {
    
    @Inject
    ReferralService referralService;
    
    @POST
    @Path("/regenerate")
    @Operation(summary = "Regenerate referral code", description = RegenerateReferralRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Referral code regenerated successfully", content = @Content(schema = @Schema(implementation = ReferralCodeResponse.class)))
    @APIResponse(responseCode = "400", description = "Bad request - Invalid reason or validation error")
    @APIResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded")
    public Response regenerateReferralCode(
        @Valid RegenerateReferralRequest request,
        @Context ContainerRequestContext requestContext
    ) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }
        
        // Regenerate code
        String newCode = referralService.regenerateReferralCode(user, request.reason());
        
        // Build response using DTO
        String oldCode = user.getReferralCode() != null ? maskCode(user.getReferralCode()) : null;
        ReferralCodeResponse response = ReferralCodeResponse.ofRegenerate(newCode, oldCode, request.reason());
        
        return ResponseHelper.ok(response, "Referral code regenerated successfully");
    }
    
    @POST
    @Path("/validate")
    @Operation(summary = "Validate referral code", description = ValidateReferralRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Referral code is valid", content = @Content(schema = @Schema(implementation = ValidateReferralResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid referral code")
    @APIResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded")
    public Response validateReferralCode(
        @Valid ValidateReferralRequest request,
        @Context ContainerRequestContext requestContext
    ) {
        // Get IP address dari request (untuk rate limiting)
        String ipAddress = extractIpAddress(requestContext);
        
        // Validate code
        User referrer = referralService.validateReferralCode(request.referralCode(), ipAddress);
        
        // Build response using DTO
        ReferrerInfoResponse referrerInfo = ReferrerInfoResponse.fromEntity(referrer);
        ValidateReferralResponse response = ValidateReferralResponse.of(referrerInfo);
        
        return ResponseHelper.ok(response, "Referral code is valid");
    }
    
    @GET
    @Path("/history")
    @Operation(summary = "Get referral history", description = ReferralHistoryResponse.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Referral history retrieved successfully", content = @Content(schema = @Schema(implementation = ReferralHistoryResponse.class)))
    public Response getReferralHistory(@Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        
        // Get history from service
        Map<String, Object> historyData = referralService.getReferralHistory(user);
        
        // Build response using DTOs
        @SuppressWarnings("unchecked")
        List<User.ReferralCodeHistoryEntry> archivedEntries =
            (List<User.ReferralCodeHistoryEntry>) historyData.get("archived");
        
        ReferralHistoryEntryResponse currentEntry = null;
        User.ReferralCodeHistoryEntry currentCode = (User.ReferralCodeHistoryEntry) historyData.get("current");
        if (currentCode != null && currentCode.getCode() != null) {
            currentEntry = ReferralHistoryEntryResponse.ofCurrent(currentCode.getCode(), user.getCreatedAt());
        }
        
        List<ReferralHistoryEntryResponse> archived = archivedEntries.stream()
            .map(entry -> ReferralHistoryEntryResponse.ofArchived(
                entry.getCode(),
                entry.getArchivedAt(),
                entry.getReason(),
                entry.getReplacedBy()
            ))
            .toList();
        
        ReferralHistoryResponse response = ReferralHistoryResponse.of(currentEntry, archived);
        
        return ResponseHelper.ok(response, "Referral history retrieved successfully");
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Get referral statistics", description = ReferralStatsResponse.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Referral statistics retrieved successfully", content = @Content(schema = @Schema(implementation = ReferralStatsResponse.class)))
    public Response getReferralStats(@Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        
        // Build response using DTO
        ReferralStatsResponse response = ReferralStatsResponse.fromEntity(user);
        
        return ResponseHelper.ok(response, "Referral statistics retrieved successfully");
    }


    private String maskCode(String code) {
        if (code == null || code.isEmpty()) {
            return "***";
        }
        if (code.length() <= 3) {
            return "***";
        }
        return code.substring(0, 3) + "***";
    }

    private String extractIpAddress(ContainerRequestContext requestContext) {
        String xForwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            String[] ips = xForwardedFor.split(",");
            String clientIp = ips[0].trim();
            if (!clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
                return clientIp;
            }
        }
        
        String xRealIp = requestContext.getHeaderString("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return "localhost";
    }
}
