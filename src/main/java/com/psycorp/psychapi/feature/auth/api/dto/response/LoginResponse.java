package com.psycorp.psychapi.feature.auth.api.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.user.api.dto.response.UserResponse;

/**
 * Response DTO untuk login dan refresh token.
 */
@Schema(description = "Response payload untuk login atau refresh token")
@JsonInclude(Include.NON_NULL)
public record LoginResponse(
    @Schema(description = "Informasi user yang login")
    UserResponse user,
    
    @Schema(description = "JWT access token", examples = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    String accessToken,
    
    @Schema(description = "JWT refresh token", examples = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4gZXhhbXBsZQ==")
    String refreshToken,
    
    @Schema(description = "Token expiry time dalam milliseconds (dari epoch)", examples = "3600000")
    Long expiresIn,
    
    @Schema(description = "Tipe token", examples = "Bearer")
    String tokenType
) {
    /**
     * Factory method untuk response login.
     * @param user Informasi user yang login
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param expiresIn Token expiry time dalam milliseconds
     * @return LoginResponse dengan tokenType "Bearer"
     */
    public static LoginResponse of(UserResponse user, String accessToken, String refreshToken, Long expiresIn) {
        return new LoginResponse(user, accessToken, refreshToken, expiresIn, "Bearer");
    }
    
    /**
     * Factory method untuk response refresh token.
     * @param accessToken JWT access token baru
     * @param refreshToken JWT refresh token baru
     * @param expiresIn Token expiry time dalam milliseconds
     * @return LoginResponse tanpa user info dan tokenType "Bearer"
     */
    public static LoginResponse ofRefresh(String accessToken, String refreshToken, Long expiresIn) {
        return new LoginResponse(null, accessToken, refreshToken, expiresIn, "Bearer");
    }
}
