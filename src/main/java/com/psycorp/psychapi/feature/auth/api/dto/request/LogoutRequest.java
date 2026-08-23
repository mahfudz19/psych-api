package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Schema(description = "Request payload untuk logout user dengan specific refresh tokens")
@JsonInclude(Include.NON_NULL)
public record LogoutRequest(
    
    @Schema(
        description = "Array of MongoDB ObjectId (24-character hexadecimal) dari refresh tokens yang akan di-revoke",
        examples = "[\"507f1f77bcf86cd799439011\"]"
    )
    String[] refreshTokenId
) {
    
    public static final String DESCRIPTION = """
        Logout user dengan revoke refresh token.
        
        **Mode Penggunaan:**
        - **Logout Current Session (Default)**: Tidak perlu query params. Akan logout session dari access token yang dikirim di Authorization header.
        - **Logout Specific Tokens**: Gunakan query param `refreshTokenId` dengan list of MongoDB ObjectId (24-character hexadecimal).
        
        **Note:** Access token WAJIB dikirim di Authorization header untuk semua mode.
        """;
}
