package com.psycorp.psychapi.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTOs untuk referral management endpoints.
 * 
 * @author Architect
 */
public final class ReferralRequests {
    
    private ReferralRequests() {}
    
    /**
     * Request untuk regenerate referral code.
     */
    @Schema(description = "Request untuk regenerate referral code")
    public record RegenerateRequest(
        @Schema(
            description = """
                Alasan regenerasi referral code.
                
                **Valid values:**
                - `user_request` - User meminta regenerate manual
                - `security` - Code compromised atau security concern
                - `regenerated` - Auto-regenerate (system)
                """,
            examples = {"user_request", "security", "regenerated"},
            required = true,
            defaultValue = "user_request"
        )
        @NotBlank(message = "Reason is required")
        @Pattern(
            regexp = "^(user_request|security|regenerated)$",
            message = "Reason must be one of: user_request, security, regenerated"
        )
        String reason
    ) {}
    
    /**
     * Request untuk validate referral code (public endpoint).
     */
    @Schema(description = "Request untuk validate referral code")
    public record ValidateRequest(
        @Schema(
            description = "Referral code yang akan divalidasi",
            examples = {"JOHN2024", "ABC12345"},
            required = true,
            pattern = "^[A-Z0-9]{6,20}$",
            maxLength = 20
        )
        @NotBlank(message = "Referral code is required")
        @Pattern(
            regexp = "^[A-Z0-9]{6,20}$",
            message = "Invalid referral code format. Must be 6-20 uppercase alphanumeric characters"
        )
        String referralCode
    ) {}
}
