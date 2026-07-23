package com.psycorp.psychapi.api.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.ReferralRequests.RegenerateRequest;
import com.psycorp.psychapi.api.dto.ReferralRequests.ValidateRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.ReferralService;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * REST Resource untuk referral management.
 * Menyediakan endpoints untuk regenerate, validate, dan查看 referral codes.
 * 
 * @author Architect
 */
@Path("/api/v1/referral")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Referral Management", description = "API untuk mengelola referral codes")
public class ReferralResource {
    
    @Inject
    ReferralService referralService;
    
    @POST
    @Path("/regenerate")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Regenerate referral code",
        description = """
            Regenerate referral code user yang sedang login.
            Code lama akan di-archive untuk backward compatibility.
            
            ### Rate Limits
            - Maximum 3 regenerations per day
            - Exceeded requests will return 429 Too Many Requests
            
            ### Reasons
            - `user_request` - User meminta regenerate manual
            - `security` - Code compromised atau security concern
            - `regenerated` - Auto-regenerate (system)
            
            ### Response
            - Referral code baru yang active
            - Old code tetap valid untuk backward compatibility
            """
    )
    @RequestBody(
        description = "Regenerate request dengan reason",
        required = true,
        content = @Content(
            schema = @Schema(implementation = RegenerateRequest.class),
            examples = {
                @ExampleObject(
                    name = "UserRequest",
                    summary = "User requested regeneration",
                    value = "{\"reason\": \"user_request\"}"
                ),
                @ExampleObject(
                    name = "Security",
                    summary = "Security concern",
                    value = "{\"reason\": \"security\"}"
                )
            }
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Referral code regenerated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "RegenerateSuccess",
                    summary = "Regenerate successful",
                    value = """
                    {
                        "success": true,
                        "message": "Referral code regenerated successfully",
                        "data": {
                            "referralCode": "NEW12345",
                            "oldCode": "OLD67890",
                            "reason": "user_request"
                        }
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Bad request - Invalid reason or validation error"
    )
    @APIResponse(
        responseCode = "429",
        description = "Too many requests - Rate limit exceeded"
    )
    public Response regenerateReferralCode(
        @Valid RegenerateRequest request,
        @Context SecurityContext securityContext
    ) {
        // Get current user from security context
        String userId = securityContext.getUserPrincipal().getName();
        User user = User.findById(new org.bson.types.ObjectId(userId));
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }
        
        // Regenerate code
        String newCode = referralService.regenerateReferralCode(user, request.reason());
        
        Map<String, Object> data = new HashMap<>();
        data.put("referralCode", newCode);
        data.put("oldCode", user.getReferralCode() != null ? maskCode(user.getReferralCode()) : null);
        data.put("reason", request.reason());
        
        return ResponseHelper.ok(data, "Referral code regenerated successfully");
    }
    
    @POST
    @Path("/validate")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Validate referral code",
        description = """
            Validate referral code dan return referrer information.
            Endpoint ini mengecek:
            - Current active codes
            - Archived codes (untuk backward compatibility)
            
            ### Rate Limits
            - Maximum 20 validations per minute per IP
            - Exceeded requests will return 429 Too Many Requests
            
            ### Response
            - Referrer information (id, email, fullName)
            - Whether code is from current or archive
            """
    )
    @RequestBody(
        description = "Validate request dengan referral code",
        required = true,
        content = @Content(
            schema = @Schema(implementation = ValidateRequest.class),
            examples = {
                @ExampleObject(
                    name = "ValidateCode",
                    summary = "Validate referral code",
                    value = "{\"referralCode\": \"JOHN2024\"}"
                )
            }
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Referral code is valid",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "ValidateSuccess",
                    summary = "Code is valid",
                    value = """
                    {
                        "success": true,
                        "message": "Referral code is valid",
                        "data": {
                            "valid": true,
                            "referrer": {
                                "id": "507f1f77bcf86cd799439011",
                                "email": "john@example.com",
                                "fullName": "John Doe"
                            },
                            "source": "current"
                        }
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid referral code"
    )
    @APIResponse(
        responseCode = "429",
        description = "Too many requests - Rate limit exceeded"
    )
    public Response validateReferralCode(
        @Valid ValidateRequest request,
        @Context SecurityContext securityContext,
        @Context ContainerRequestContext requestContext
    ) {
        // Get IP address dari request (untuk rate limiting)
        // Note: Di production, extract dari X-Forwarded-For header
        String ipAddress = extractIpAddress(requestContext);
        
        // Validate code
        User referrer = referralService.validateReferralCode(request.referralCode(), ipAddress);
        
        Map<String, Object> data = new HashMap<>();
        data.put("valid", true);
        
        Map<String, Object> referrerData = new HashMap<>();
        referrerData.put("id", referrer.id.toHexString());
        referrerData.put("email", referrer.getEmail());
        referrerData.put("fullName", referrer.getFullName());
        data.put("referrer", referrerData);
        
        // Note: source (current/archived) ditentukan di service tapi tidak di-expose ke client
        // untuk security reasons
        
        return ResponseHelper.ok(data, "Referral code is valid");
    }
    
    @GET
    @Path("/history")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Get referral history",
        description = """
            Get referral code history untuk user yang sedang login.
            Termasuk:
            - Current active code
            - Archived codes (jika ada)
            
            ### Response
            - List referral codes dengan status dan timestamp
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Referral history retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "HistorySuccess",
                    summary = "History retrieved",
                    value = """
                    {
                        "success": true,
                        "message": "Referral history retrieved successfully",
                        "data": {
                            "current": {
                                "code": "JOHN2024",
                                "status": "active",
                                "createdAt": "2026-07-22T10:00:00Z"
                            },
                            "archived": [
                                {
                                    "code": "OLD***",
                                    "archivedAt": "2026-07-20T08:00:00Z",
                                    "reason": "user_request",
                                    "replacedBy": "JOHN2024"
                                }
                            ]
                        }
                    }
                    """
                )
            }
        )
    )
    public Response getReferralHistory(@Context SecurityContext securityContext) {
        // Get current user
        String userId = securityContext.getUserPrincipal().getName();
        User user = User.findById(new org.bson.types.ObjectId(userId));
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }
        
        // Get history
        List<Map<String, Object>> history = referralService.getReferralHistory(user);
        
        Map<String, Object> data = new HashMap<>();
        data.put("history", history);
        
        return ResponseHelper.ok(data, "Referral history retrieved successfully");
    }
    
    @GET
    @Path("/stats")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Get referral statistics",
        description = """
            Get referral statistics untuk user yang sedang login.
            Termasuk:
            - Total referrals
            - Successful referrals
            - Referral earnings (jika ada)
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Referral statistics retrieved successfully"
    )
    public Response getReferralStats(@Context SecurityContext securityContext) {
        // Get current user
        String userId = securityContext.getUserPrincipal().getName();
        User user = User.findById(new org.bson.types.ObjectId(userId));
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }
        
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalReferrals", Objects.requireNonNullElse(user.getTotalReferrals(), 0));
        stats.put("successfulReferrals", Objects.requireNonNullElse(user.getSuccessfulReferrals(), 0));
        stats.put("referralEarnings", Objects.requireNonNullElse(user.getReferralEarnings(), 0.0));

        return ResponseHelper.ok(stats, "Referral statistics retrieved successfully");
    }
    
    /**
     * Mask code untuk security (show only first 3 chars).
     */
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
