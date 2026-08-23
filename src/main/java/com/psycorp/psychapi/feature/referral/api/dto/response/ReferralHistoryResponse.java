package com.psycorp.psychapi.feature.referral.api.dto.response;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response DTO untuk referral history.
 */
@Schema(description = "Response payload untuk referral history")
@JsonInclude(Include.NON_NULL)
public record ReferralHistoryResponse(
    
    @Schema(description = "Referral code current yang active")
    ReferralHistoryEntryResponse current,
    
    @Schema(description = "Daftar referral codes yang di-archive")
    List<ReferralHistoryEntryResponse> archived
) {
    
    public static final String DESCRIPTION = """
        Get referral code history untuk user yang sedang login.
        Termasuk:
        - Current active code
        - Archived codes (jika ada)
        """;
    
    public static ReferralHistoryResponse of(ReferralHistoryEntryResponse current, List<ReferralHistoryEntryResponse> archived) {
        return new ReferralHistoryResponse(current, archived);
    }
}
