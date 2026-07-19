package com.psycorp.psychapi.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.domain.model.User.AccountType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

        @Schema(description = "Referred by code that identify user", examples = "usr_123")
        String referredBy,
        
        @Schema(description = "Account type", examples = "INDIVIDUAL")
        AccountType accountType
    ) {}

    /**
     * Request untuk login user.
     */
    public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        
        @NotBlank(message = "Password is required")
        String password
    ) {}

    /**
     * Request untuk refresh JWT token.
     */
    public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
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
