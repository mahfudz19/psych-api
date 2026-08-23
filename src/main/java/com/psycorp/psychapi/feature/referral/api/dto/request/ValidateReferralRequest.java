package com.psycorp.psychapi.feature.referral.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO untuk validate referral code.
 */
@Schema(description = "Request payload untuk validate referral code")
public record ValidateReferralRequest(
    
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
) {
    public static final String DESCRIPTION = """
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
        """;
}
