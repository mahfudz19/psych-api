package com.psycorp.psychapi.feature.auth.api.dto.response;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.auth.model.DeviceInfo;
import com.psycorp.psychapi.feature.auth.model.RefreshToken;

/**
 * Response wrapper untuk session data.
 */
@Schema(description = "Session data dengan device info dan metadata")
@JsonInclude(Include.NON_NULL)
public record SessionResponse(
    
    @Schema(description = "Session ID (ObjectId)", examples = "507f1f77bcf86cd799439011")
    String id,
    
    @Schema(description = "Device identifier", examples = "dev_abc12345")
    String deviceId,
    
    @Schema(description = "Device information (browser, OS, location)")
    DeviceInfo deviceInfo,
    
    @Schema(description = "Session status", examples = "active")
    String status,
    
    @Schema(description = "Session created timestamp in ISO-8601 format")
    Instant createdAt,
    
    @Schema(description = "Last activity timestamp in ISO-8601 format")
    Instant lastActive,
    
    @Schema(description = "Token expiry timestamp in ISO-8601 format")
    Instant expiresAt,
    
    @Schema(description = "Flag untuk session yang sedang aktif", examples = "true")
    boolean isCurrentSession
) {
    
    /**
     * Factory method untuk convert dari RefreshToken entity.
     * @param token RefreshToken entity
     * @param currentTokenId ID dari token yang sedang aktif (untuk comparison)
     * @return SessionResponse
     */
    public static SessionResponse fromEntity(RefreshToken token, String currentTokenId) {
        String tokenId = token.getId() != null ? token.getId().toHexString() : null;
        Instant lastActive = token.getDeviceInfo() != null 
            ? token.getDeviceInfo().getLastActive() 
            : null;
        boolean isCurrent = tokenId != null && tokenId.equals(currentTokenId);
        
        return new SessionResponse(
            tokenId,
            token.getDeviceId(),
            token.getDeviceInfo(),
            token.getStatus(),
            token.getCreatedAt(),
            lastActive,
            token.getExpiresAt(),
            isCurrent
        );
    }
}
