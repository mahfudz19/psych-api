package com.psycorp.psychapi.feature.referral.api.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.user.model.User;

/**
 * Response DTO untuk informasi referrer.
 */
@Schema(description = "Informasi referrer")
@JsonInclude(Include.NON_NULL)
public record ReferrerInfoResponse(
    
    @Schema(description = "Unique ID referrer", examples = "507f1f77bcf86cd799439011")
    String id,
    
    @Schema(description = "Email referrer", examples = "john@example.com")
    String email,
    
    @Schema(description = "Nama lengkap referrer", examples = "John Doe")
    String fullName
) {
    
    /**
     * Factory method untuk convert dari User entity ke ReferrerInfoResponse.
     * @param user User entity (referrer)
     * @return ReferrerInfoResponse
     */
    public static ReferrerInfoResponse fromEntity(User user) {
        return new ReferrerInfoResponse(
            user.getId() != null ? user.getId().toHexString() : null,
            user.getEmail(),
            user.getFullName()
        );
    }
}
