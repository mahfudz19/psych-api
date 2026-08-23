package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.feature.user.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO untuk registrasi user baru.
 * Mendukung tiga metode registrasi:
 * 1. Registrasi langsung (tanpa invitation)
 * 2. Registrasi dengan referral code
 * 3. Registrasi dengan invitation (invite code atau direct add)
 */
@Schema(description = "Request payload untuk registrasi user baru")
public record RegisterRequest(
    
    // ==================== REQUIRED FIELDS ====================
    
    @Schema(description = "Email address user", examples = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,
    
    @Schema(description = "Password (min 8 chars, must contain uppercase, lowercase, and numbers)", examples = "SecurePass123")
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "Password must be at least 8 characters and contain uppercase, lowercase, and numbers"
    )
    String password,
    
    @Schema(description = "Full name", examples = "John Doe")
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    String fullName,
    
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
     * Custom validation untuk cross-field validation.
     * Memvalidasi:
     * 1. Mutual exclusivity: inviteCode tidak boleh ada bersama invitedBy/invitedOrganizationId
     * 2. Direct add completeness: invitedBy dan invitedOrganizationId harus ada bersamaan
     * 3. Account type requirement: Jika menggunakan invitation, accountType wajib ORGANIZATION
     * 4. Invitation role constraint: invitationRole hanya boleh ada jika menggunakan direct add
     * 
     * @return true jika valid, throws ValidationException jika tidak valid
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

    // === CONSTANT UNTUK SHARED DESCRIPTION ===
    public static final String DESCRIPTION = """
        Request payload untuk registrasi user baru.
        
        **Contoh 1 - Registrasi Langsung (INDIVIDUAL):**
        ```json
        {
          "email": "user@example.com",
          "password": "SecurePass123",
          "fullName": "John Doe",
          "accountType": "INDIVIDUAL"
        }
        ```
        
        **Contoh 2 - Registrasi dengan Referral Code:**
        ```json
        {
          "email": "user@example.com",
          "password": "SecurePass123",
          "fullName": "John Doe",
          "accountType": "INDIVIDUAL",
          "referralCode": "REF123"
        }
        ```
        
        **Contoh 3 - Registrasi dengan Invite Code (ORGANIZATION):**
        ```json
        {
          "email": "user@example.com",
          "password": "SecurePass123",
          "fullName": "John Doe",
          "accountType": "ORGANIZATION",
          "inviteCode": "INVITE-ABC123"
        }
        ```
        
        **Contoh 4 - Registrasi dengan Direct Add (ORGANIZATION):**
        ```json
        {
          "email": "user@example.com",
          "password": "SecurePass123",
          "fullName": "John Doe",
          "accountType": "ORGANIZATION",
          "invitedBy": "507f1f77bcf86cd799439011",
          "invitedOrganizationId": "507f191e810c19729de860ea",
          "invitationRole": "member"
        }
        ```
        """;
}
