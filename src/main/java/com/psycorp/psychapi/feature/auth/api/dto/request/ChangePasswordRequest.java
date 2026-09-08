package com.psycorp.psychapi.feature.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Kata sandi lama wajib diisi")
    String oldPassword,

    @NotBlank(message = "Kata sandi baru wajib diisi")
    @Size(min = 8, message = "Kata sandi baru minimal 8 karakter")
    String newPassword
) {
    public static final String DESCRIPTION = "Request body untuk mengubah kata sandi user yang sedang login";
}