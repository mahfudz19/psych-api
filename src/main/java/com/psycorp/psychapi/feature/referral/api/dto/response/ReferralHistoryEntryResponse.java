package com.psycorp.psychapi.feature.referral.api.dto.response;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response DTO untuk entry history referral code.
 */
@Schema(description = "Entry history referral code")
@JsonInclude(Include.NON_NULL)
public record ReferralHistoryEntryResponse(
    
    @Schema(description = "Referral code", examples = "JOHN2024")
    String code,
    
    @Schema(description = "Status referral code", examples = "active")
    String status,
    
    @Schema(description = "Tanggal pembuatan code", examples = "2026-07-22T10:00:00Z")
    Instant createdAt,
    
    @Schema(description = "Tanggal code di-archive", examples = "2026-07-20T08:00:00Z")
    Instant archivedAt,
    
    @Schema(description = "Alasan archiving/regenerasi", examples = "user_request")
    String reason,
    
    @Schema(description = "Referral code pengganti", examples = "JOHN2024")
    String replacedBy
) {
    
    /**
     * Factory method untuk entry current/active code.
     * @param code Referral code
     * @param createdAt Tanggal pembuatan
     * @return ReferralHistoryEntryResponse untuk current code
     */
    public static ReferralHistoryEntryResponse ofCurrent(String code, Instant createdAt) {
        return new ReferralHistoryEntryResponse(code, "active", createdAt, null, null, null);
    }
    
    /**
     * Factory method untuk entry archived code.
     * @param code Referral code yang di-archive
     * @param archivedAt Tanggal archiving
     * @param reason Alasan archiving
     * @param replacedBy Code pengganti
     * @return ReferralHistoryEntryResponse untuk archived code
     */
    public static ReferralHistoryEntryResponse ofArchived(String code, Instant archivedAt, String reason, String replacedBy) {
        return new ReferralHistoryEntryResponse(code, "archived", null, archivedAt, reason, replacedBy);
    }
}
