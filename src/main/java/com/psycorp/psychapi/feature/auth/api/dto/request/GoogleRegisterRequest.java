package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.feature.user.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request payload untuk registrasi user baru via Google SSO")
public record GoogleRegisterRequest(
    
    // ==================== GOOGLE REQUIRED FIELD ====================
    
    @Schema(description = "Google ID Token dari frontend", examples = "eyJhbGciOiJSUzI1NiIs...")
    @NotBlank(message = "Token is required")
    String token,
    
    @Schema(description = "Account type: INDIVIDUAL or ORGANIZATION", examples = "ORGANIZATION")
    @NotNull(message = "Account type is required")  
    AccountType accountType,
    
    // ==================== REFERRAL SYSTEM (Optional) ====================
    
    @Schema(description = "Referral code (6-20 uppercase alphanumeric)", examples = "REF123")
    @Pattern(
        regexp = "^[A-Z0-9]{6,20}$",
        message = "Referral code must be 6-20 uppercase alphanumeric characters"
    )
    String referralCode,
    
    // ==================== INVITATION SYSTEM - Option A (Optional) ====================
    
    @Schema(description = "Invite code (6-30 alphanumeric with hyphens)", examples = "INVITE-ABC123")
    @Pattern(
        regexp = "^[A-Za-z0-9-]{6,30}$",
        message = "Invite code must be 6-30 alphanumeric characters"
    )
    String inviteCode,
    
    // ==================== INVITATION SYSTEM - Option B (Optional) ====================
    
    @Schema(description = "Inviter user ID (24-char hex ObjectId)", examples = "507f1f77bcf86cd799439011")
    @Pattern(
        regexp = "^[0-9a-fA-F]{24}$",
        message = "Invited by must be a valid 24-character ObjectId"
    )
    String invitedBy,
    
    @Schema(description = "Invited organization ID (24-char hex ObjectId)", examples = "507f191e810c19729de860ea")
    @Pattern(
        regexp = "^[0-9a-fA-F]{24}$",
        message = "Invited organization ID must be a valid 24-character ObjectId"
    )
    String invitedOrganizationId,
    
    @Schema(description = "Invitation role: member or admin (only for direct add)", examples = "member")
    @Pattern(
        regexp = "^(member|admin)$",
        message = "Invitation role must be member or admin"
    )
    String invitationRole
) {
    
    /**
     * Custom validation untuk cross-field validation, persis seperti RegisterRequest.
     */
    public boolean isValid() {
        boolean hasInviteCode = inviteCode != null && !inviteCode.isBlank();
        boolean hasInvitedBy = invitedBy != null && !invitedBy.isBlank();
        boolean hasInvitedOrgId = invitedOrganizationId != null && !invitedOrganizationId.isBlank();
        boolean hasInvitationRole = invitationRole != null && !invitationRole.isBlank();
        
        // 1. Mutual Exclusivity
        if (hasInviteCode && (hasInvitedBy || hasInvitedOrgId)) {
            throw new ValidationException("INVALID_INVITATION", "Cannot use both inviteCode and direct add (invitedBy/invitedOrganizationId)");
        }
        
        // 2. Direct Add Completeness
        if (hasInvitedBy != hasInvitedOrgId) {
            throw new ValidationException("MISSING_INVITATION_FIELD", "invitedBy and invitedOrganizationId must be provided together");
        }
        
        // 3. Account Type Requirement
        if ((hasInviteCode || hasInvitedBy) && accountType != AccountType.ORGANIZATION) {
            throw new ValidationException("INVALID_ACCOUNT_TYPE", "Account type must be ORGANIZATION when using invitation");
        }

        // 4. Invitation Role Constraint
        if (hasInvitationRole && !hasInvitedBy) {
            throw new ValidationException("INVALID_INVITATION_ROLE", "invitationRole can only be specified with direct add (invitedBy)");
        }
        
        return true;
    }

    public static final String DESCRIPTION = """
        Request payload untuk registrasi user baru menggunakan Google SSO.
        Mengharuskan token dari Google, dan mendukung logika pendaftaran yang sama dengan registrasi manual (Referral, Invite Code, Direct Add).
        """;
}