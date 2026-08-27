package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload untuk mengeksekusi reset password baru")
public record ResetPasswordRequest(
    @Schema(description = "Reset password token dari link email", examples = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Token is required")
    String token,

    @Schema(description = "Kata sandi baru (minimal 8 karakter)", examples = "P@ssword123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String newPassword
) {
    public static final String DESCRIPTION = """
        Request payload untuk mengatur ulang kata sandi menggunakan token dari email.
        
        **Contoh Payload:**
        ```json
        {
          "token": "550e8400-e29b-41d4-a716-446655440000",
          "newPassword": "MyNewSecurePassword123!"
        }
        ```
        """;
}