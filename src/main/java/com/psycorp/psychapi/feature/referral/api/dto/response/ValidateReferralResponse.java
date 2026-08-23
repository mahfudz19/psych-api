package com.psycorp.psychapi.feature.referral.api.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response DTO untuk validate referral code.
 */
@Schema(description = "Response payload untuk validasi referral code")
@JsonInclude(Include.NON_NULL)
public record ValidateReferralResponse(
    
    @Schema(description = "Status validitas referral code", examples = "true")
    Boolean valid,
    
    @Schema(description = "Informasi referrer")
    ReferrerInfoResponse referrer
) {
    
    /**
     * Factory method untuk response validate referral code.
     * @param referrer Informasi referrer
     * @return ValidateReferralResponse
     */
    public static ValidateReferralResponse of(ReferrerInfoResponse referrer) {
        return new ValidateReferralResponse(true, referrer);
    }
}
