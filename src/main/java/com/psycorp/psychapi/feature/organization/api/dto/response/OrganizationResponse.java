package com.psycorp.psychapi.feature.organization.api.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record OrganizationResponse(
    String id,
    String name,
    String description,
    String website,
    String phone,
    String email,
    String address,
    String ownerId,
    String plan,
    Boolean status,
    Instant trialStartsAt,
    Instant trialEndsAt,
    Integer seats,
    Integer seatsUsed,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrganizationResponse fromEntity(com.psycorp.psychapi.feature.organization.model.Organization org) {
        return new OrganizationResponse(
            org.getId() != null ? org.getId().toHexString() : null,
            org.getName(),
            org.getDescription(),
            org.getWebsite(),
            org.getPhone(),
            org.getEmail(),
            org.getAddress(),
            org.getOwnerId() != null ? org.getOwnerId().toHexString() : null,
            org.getPlan(),
            org.getStatus(),
            org.getTrialStartsAt(),
            org.getTrialEndsAt(),
            org.getSeats(),
            org.getSeatsUsed(),
            org.getCreatedAt(),
            org.getUpdatedAt()
        );
    }
}
