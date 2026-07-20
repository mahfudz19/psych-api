package com.psycorp.psychapi.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.domain.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    
    private AuthRequests() {}

    @Schema(description = "Request untuk registrasi user baru")
    public record RegisterRequest(
        @Schema(
            description = "Email user untuk login dan notifikasi",
            examples = {"user@example.com", "admin@company.com"},
            required = true,
            format = "email"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format. Must be a valid email address like user@example.com")
        String email,
        
        @Schema(
            description = """
                Password user untuk authentication.
                
                **Requirements:**
                - Minimal 8 karakter
                - Harus mengandung minimal 1 huruf uppercase
                - Harus mengandung minimal 1 huruf lowercase
                - Harus mengandung minimal 1 angka
                """,
            examples = {"Password123!", "SecureP@ss2024"},
            required = true,
            format = "password"
        )
        @NotBlank(message = "Password is required")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters, contain at least one uppercase letter, one lowercase letter, and one number"
        )
        String password,
        
        @Schema(
            description = "Full name user yang akan ditampilkan",
            examples = {"John Doe", "Company Admin"},
            required = true,
            maxLength = 100
        )
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Schema(
            description = """
                Tipe akun yang akan dibuat.
                
                - **INDIVIDUAL**: Personal account untuk penggunaan pribadi
                - **ORGANIZATION**: Organization account untuk bisnis/perusahaan dengan fitur collaboration
                """,
            examples = {"INDIVIDUAL", "ORGANIZATION"},
            required = true,
            defaultValue = "INDIVIDUAL"
        )
        @NotNull(message = "Account type is required")
        AccountType accountType,

        // === REFERRAL SYSTEM ===
        @Schema(
            description = "Referral code dari user yang sudah ada untuk mendapatkan referral benefit",
            examples = {"JOHN2024", "REF123ABC"},
            required = false,
            pattern = "^[A-Z0-9]{6,20}$",
            maxLength = 20
        )
        @Pattern(regexp = "^[A-Z0-9]{6,20}$", message = "Invalid referral code format. Must be 6-20 uppercase alphanumeric characters")
        String referralCode,

        // === INVITATION SYSTEM ===
        // Option A: Invite dengan code (user self-register)
        @Schema(
            description = "Organization invitation code untuk join organization yang sudah ada",
            examples = {"INV-ORG001-ABC", "INVITE123"},
            required = false,
            pattern = "^[A-Z0-9\\-]{6,30}$",
            maxLength = 30
        )
        @Pattern(regexp = "^[A-Z0-9\\-]{6,30}$", message = "Invalid invitation code format. Must be 6-30 alphanumeric characters with hyphens allowed")
        String inviteCode,

        // Option B: Direct add (owner adds member directly)
        @Schema(
            description = "User ID dari owner/admin yang mengundang (untuk direct add invitation)",
            examples = {"507f1f77bcf86cd799439011"},
            required = false,
            pattern = "^[a-fA-F0-9]{24}$",
            format = "objectId"
        )
        @Pattern(regexp = "^[a-fA-F0-9]{24}$", message = "Invalid invitedBy format. Must be a valid 24-character ObjectId hex string")
        String invitedBy,

        @Schema(
            description = "Organization ID yang mengundang (untuk direct add invitation)",
            examples = {"507f1f77bcf86cd799439012"},
            required = false,
            pattern = "^[a-fA-F0-9]{24}$",
            format = "objectId"
        )
        @Pattern(regexp = "^[a-fA-F0-9]{24}$", message = "Invalid invitedOrganizationId format. Must be a valid 24-character ObjectId hex string")
        String invitedOrganizationId,

        @Schema(
            description = """
                Role user dalam organization (hanya untuk direct add invitation).
                
                - **member**: Regular member dengan access terbatas
                - **admin**: Organization admin dengan access management
                """,
            examples = {"member", "admin"},
            required = false,
            defaultValue = "member"
        )
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

    @Schema(description = "Request untuk login user")
    public record LoginRequest(
        @Schema(
            description = "Email user yang sudah terdaftar",
            examples = {"user@example.com", "admin@company.com"},
            required = true,
            format = "email"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format. Must be a valid email address like user@example.com")
        String email,
        
        @Schema(
            description = "Password user untuk authentication",
            examples = {"Password123!", "SecureP@ss2024"},
            required = true,
            format = "password"
        )
        @NotBlank(message = "Password is required")
        String password
    ) {}
}
