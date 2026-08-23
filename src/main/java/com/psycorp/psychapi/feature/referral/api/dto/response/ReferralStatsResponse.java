package com.psycorp.psychapi.feature.referral.api.dto.response;

import java.util.Objects; // 1. Tambahkan import ini

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.user.model.User;

/**
 * Response DTO untuk referral statistics.
 */
@Schema(description = "Response payload untuk referral statistics")
@JsonInclude(Include.NON_NULL)
public record ReferralStatsResponse(
    
    @Schema(description = "Kode referral aktif saat ini", examples = "JOHN2024")
    String referralCode,
    
    @Schema(description = "Total jumlah referral", examples = "10")
    Integer totalReferrals,
    
    @Schema(description = "Jumlah referral yang berhasil", examples = "5")
    Integer successfulReferrals,
    
    @Schema(description = "Total earnings dari referral", examples = "50000.00")
    Double referralEarnings,
    
    @Schema(description = "Revenue share percentage (0-100)", examples = "10")
    Integer revenueSharePercentage
) {
    
    public static final String DESCRIPTION = """
        Get referral statistics untuk user yang sedang login.
        Termasuk:
        - Referral code aktif
        - Total referrals
        - Successful referrals
        - Referral earnings (jika ada)
        - Revenue share percentage
        """;

    public static ReferralStatsResponse fromEntity(User user) {
        // 2. Gunakan Objects.requireNonNullElse untuk kode yang lebih aman dan bersih
        return new ReferralStatsResponse(
            user.getReferralCode(),
            Objects.requireNonNullElse(user.getTotalReferrals(), 0),
            Objects.requireNonNullElse(user.getSuccessfulReferrals(), 0),
            Objects.requireNonNullElse(user.getReferralEarnings(), 0.0),
            Objects.requireNonNullElse(user.getRevenueSharePercentage(), 0)
        );
    }
}