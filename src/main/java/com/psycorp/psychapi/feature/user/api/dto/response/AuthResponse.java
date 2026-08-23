package com.psycorp.psychapi.feature.user.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record AuthResponse(
    UserResponse user,
    String token,
    String tokenType,
    Long expiresIn
) {
    public static AuthResponse of(UserResponse user, String token, String tokenType, Long expiresIn) {
        return new AuthResponse(user, token, tokenType, expiresIn);
    }
}
