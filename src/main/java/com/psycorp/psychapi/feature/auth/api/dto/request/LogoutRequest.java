package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Schema(description = "Request payload untuk logout user dengan specific refresh tokens")
@JsonInclude(Include.NON_NULL)
public record LogoutRequest(
    @Schema(
        description = "MongoDB ObjectId (24-character hex) dari target session yang akan di-revoke. Jika kosong, revoke current session dari cookie.",
        examples = "507f1f77bcf86cd799439011"
    )
    String targetSessionId
) {
    public static final String DESCRIPTION = """
        Logout user dengan revoke session token.
        - **Logout Current Session**: Kosongkan `targetSessionId`. Akan revoke session dari Cookie.
        - **Logout Specific Session**: Isi `targetSessionId` dengan MongoDB ObjectId session target.
        """;
}
