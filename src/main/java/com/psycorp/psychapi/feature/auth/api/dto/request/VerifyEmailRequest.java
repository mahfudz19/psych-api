package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO untuk verifikasi email dengan token.
 * Digunakan untuk magic link verification flow.
 */
@Schema(description = "Request payload untuk verifikasi email")
public record VerifyEmailRequest(
    @Schema(description = "Email address user", examples = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,
    
    @Schema(description = "Verification token dari email link", 
            examples = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Verification token is required")
    String plainToken
) {
    public static final String DESCRIPTION = """
        Request payload untuk verifikasi email menggunakan token.
        
        **Contoh - Magic Link Verification:**
        ```json
        {
          "plainToken": "550e8400-e29b-41d4-a716-446655440000"
        }
        ```
        
        Token ini didapatkan dari link di email verifikasi yang dikirim setelah registrasi.
        Token hanya bisa digunakan sekali (single-use) dan expired setelah 24 jam.
        """;
}
