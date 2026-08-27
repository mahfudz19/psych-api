package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload untuk meminta tautan reset password")
public record ForgotPasswordRequest(
    @Schema(description = "Email address user", examples = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email
) {
    public static final String DESCRIPTION = """
        Request payload untuk meminta tautan pengaturan ulang kata sandi (Forgot Password).
        
        **Contoh Payload:**
        ```json
        {
          "email": "user@example.com"
        }
        ```
        """;
}