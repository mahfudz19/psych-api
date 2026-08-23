package com.psycorp.psychapi.feature.referral.api.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response DTO untuk informasi referral code.
 */
@Schema(description = "Informasi referral code")
@JsonInclude(Include.NON_NULL)
public record ReferralCodeResponse(
    
    @Schema(description = "Referral code yang active", examples = "JOHN2024")
    String referralCode,
    
    @Schema(description = "Referral code lama yang di-archive (jika ada)", examples = "OLD12***")
    String oldCode,
    
    @Schema(description = "Alasan regenerasi", examples = "user_request")
    String reason
) {
    
    /**
     * Factory method untuk response regenerate referral code.
     * @param referralCode Referral code baru
     * @param oldCode Referral code lama (masked)
     * @param reason Alasan regenerasi
     * @return ReferralCodeResponse
     */
    public static ReferralCodeResponse ofRegenerate(String referralCode, String oldCode, String reason) {
        return new ReferralCodeResponse(referralCode, oldCode, reason);
    }
}
