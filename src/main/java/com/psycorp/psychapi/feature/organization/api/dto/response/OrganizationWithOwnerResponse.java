package com.psycorp.psychapi.feature.organization.api.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.user.model.User;

@JsonInclude(Include.NON_NULL)
public record OrganizationWithOwnerResponse(
    OrganizationResponse organization,
    OwnerInfo owner
) {
    public record OwnerInfo(
        String id,
        String organizationId,
        User.OrganizationRole organizationRole,
        String organizationName,
        List<User.Role> roles
    ) {}

    public static OrganizationWithOwnerResponse of(Organization org, User user) {
        OrganizationResponse orgResponse = OrganizationResponse.fromEntity(org);
        OwnerInfo ownerInfo = new OwnerInfo(
            user.getId() != null ? user.getId().toHexString() : null,
            user.getOrganizationId() != null ? user.getOrganizationId().toHexString() : null,
            user.getOrganizationRole(),
            user.getOrganizationName(),
            user.getRoles()
        );
        return new OrganizationWithOwnerResponse(orgResponse, ownerInfo);
    }
}
