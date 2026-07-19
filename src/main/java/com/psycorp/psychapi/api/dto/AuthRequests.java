package com.psycorp.psychapi.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.domain.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class AuthRequests {
    
    private AuthRequests() {}

    public record RegisterRequest(
        @Schema(description = "Email user", examples = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        
        @Schema(description = "Password user", examples = "Password123!")
        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                 message = "Password must be at least 8 characters, contain uppercase, lowercase, and number")
        String password,
        
        @Schema(description = "Full name user", examples = "John Doe")
        @NotBlank(message = "Full name is required")
        String fullName,

        @Schema(description = "Account type", examples = "INDIVIDUAL")
        @NotNull(message = "Account type is required")
        AccountType accountType,

        // === REFERRAL SYSTEM ===
        @Schema(description = "Referral code from existing user", examples = "JOHN2024")
        @Pattern(regexp = "^[A-Z0-9]{6,20}$", message = "Invalid referral code format")
        String referralCode,

        // === INVITATION SYSTEM ===
        // Option A: Invite dengan code (user self-register)
        @Schema(description = "Organization invitation code", examples = "INV-ORG001-ABC")
        @Pattern(regexp = "^[A-Z0-9\\-]{6,30}$", message = "Invalid invitation code format")
        String inviteCode,

        // Option B: Direct add (owner adds member directly)
        @Schema(description = "User ID who invited (for direct add)", examples = "507f1f77bcf86cd799439011")
        @Pattern(regexp = "^[a-fA-F0-9]{24}$", message = "Invalid invitedBy format. Must be a valid ObjectId")
        String invitedBy,

        @Schema(description = "Organization ID (for direct add)", examples = "507f1f77bcf86cd799439012")
        @Pattern(regexp = "^[a-fA-F0-9]{24}$", message = "Invalid invitedOrganizationId format. Must be a valid ObjectId")
        String invitedOrganizationId,

        @Schema(description = "Invitation role (for direct add)", examples = "member")
        @Pattern(regexp = "^(member|admin)$", message = "Invalid invitation role. Must be 'member' or 'admin'")
        String invitationRole
    ) {        
        public RegisterRequest {
            // Normalize inputs
            boolean hasInviteCode = inviteCode != null && !inviteCode.trim().isEmpty();
            boolean hasInvitedBy = invitedBy != null && !invitedBy.trim().isEmpty();
            boolean hasInvitedOrganizationId = invitedOrganizationId != null && !invitedOrganizationId.trim().isEmpty();
            boolean hasDirectAdd = hasInvitedBy || hasInvitedOrganizationId;
            
            // 1. Mutual exclusivity: inviteCode DAN direct add tidak boleh bersamaan
            if (hasInviteCode && hasDirectAdd) {
                throw new ValidationException("INVALID_REGISTRATION",
                    "Cannot use both inviteCode and direct add (invitedBy/invitedOrganizationId) at the same time");
            }
            
            // 2. invitedBy dan invitedOrganizationId harus ada bersamaan (jika salah satu ada)
            if (hasInvitedBy != hasInvitedOrganizationId) {
                throw new ValidationException("INVALID_REGISTRATION",
                    "invitedBy and invitedOrganizationId must be provided together");
            }
            
            // 3. accountType harus ORGANIZATION jika ada invitation (code atau direct add)
            if ((hasInviteCode || hasDirectAdd) && accountType != AccountType.ORGANIZATION) {
                throw new ValidationException("INVALID_ACCOUNT_TYPE",
                    "Account type must be ORGANIZATION when using invitation");
            }
            
            // 4. invitationRole hanya boleh ada jika ada direct add
            if ((invitationRole != null && !invitationRole.trim().isEmpty()) && !hasDirectAdd) {
                throw new ValidationException("INVALID_REGISTRATION",
                    "invitationRole can only be set when using direct add invitation");
            }
        }
    }

    public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Schema(description = "User email", examples = "regular.member@example.com")
        String email,
        
        @NotBlank(message = "Password is required")
        @Schema(description = "User password", examples = "password123")
        String password
    ) {}

    public record RefreshTokenRequest(
    ) {}

    /**
     * Request untuk logout user.
     */
    public record LogoutRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken,
        
        Boolean allDevices
    ) {
        public LogoutRequest {
            // Default allDevices ke false jika tidak disediakan
            if (allDevices == null) {
                allDevices = false;
            }
        }
    }

    /**
     * Request untuk switch account context (individual <-> organization).
     */
    public record SwitchAccountRequest(
        @NotBlank(message = "Account type is required")
        @Pattern(regexp = "^(individual|organization)$", message = "Account type must be 'individual' or 'organization'")
        String accountType,
        
        String organizationId
    ) {
        public SwitchAccountRequest {
            // organizationId wajib jika accountType = "organization"
            if ("organization".equals(accountType) && (organizationId == null || organizationId.isBlank())) {
                throw new IllegalArgumentException("organizationId is required when switching to organization account");
            }
        }
    }

    /**
     * Request untuk forgot password (request reset token via email).
     */
    public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
    ) {}

    /**
     * Request untuk reset password dengan token dari email.
     */
    public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String token,
        
        @NotBlank(message = "New password is required")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
                 message = "Password must be at least 8 characters, contain uppercase, lowercase, and number")
        String newPassword
    ) {}

    /**
     * Request untuk change password (authenticated user).
     */
    public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        
        @NotBlank(message = "New password is required")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
                 message = "Password must be at least 8 characters, contain uppercase, lowercase, and number")
        String newPassword
    ) {}

    /**
     * Request untuk resend verification email.
     */
    public record ResendVerificationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
    ) {}
}
