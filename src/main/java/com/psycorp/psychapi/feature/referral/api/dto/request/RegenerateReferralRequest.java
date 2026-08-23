package com.psycorp.psychapi.feature.referral.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO untuk regenerate referral code.
 */
@Schema(description = "Request payload untuk regenerate referral code")
public record RegenerateReferralRequest(
    
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
) {
    public static final String DESCRIPTION = """
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
            """;
}
