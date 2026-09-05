package com.psycorp.psychapi.feature.organization.service;

import java.util.List;
import java.util.Objects;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.MembersListRequest;
import com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.UpdateMemberRoleRequest;
import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.shared.util.DocumentUpdater;
import com.psycorp.psychapi.shared.util.MongoFilter;
import com.psycorp.psychapi.shared.util.ValidationUtils;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrganizationMemberService {

    // Fields yang bisa di-search untuk member
    private static final String[] SEARCH_FIELDS = {"fullName", "email"};

    // Valid organization member roles
    private static final String[] VALID_MEMBER_ROLES = {"member", "admin"};

    @Inject
    OrganizationService organizationService;

    public PanacheQuery<User> findMembers(String orgId, User currentUser, MembersListRequest request) {
        Organization organization = getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, currentUser, "owner", "admin");

        Bson baseFilter = Filters.eq("organizationId", new ObjectId(orgId));
        Bson requestFilter = MongoFilter.fromRequest(request, SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, requestFilter);
        Bson sort = MongoFilter.sort(request);

        return User.find(finalFilter, sort);
    }

    public long countMembers(String orgId, User currentUser, MembersListRequest request) {
        Organization organization = getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, currentUser, "owner", "admin");

        Bson baseFilter = Filters.eq("organizationId", new ObjectId(orgId));
        Bson requestFilter = MongoFilter.fromRequest(request, SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, requestFilter);

        return User.count(finalFilter);
    }

    public User getMemberById(String orgId, String memberId, User currentUser) {
        ObjectId organizationId = ValidationUtils.validateObjectId(orgId);
        ObjectId targetMemberId = ValidationUtils.validateObjectId(memberId);

        // Validate requester adalah member organization ini
        if (currentUser.getOrganizationId() == null || !currentUser.getOrganizationId().equals(organizationId)) {
            throw new ValidationException("UNAUTHORIZED",
                "User does not have access to this organization");
        }

        User user = User.findById(targetMemberId);
        if (user == null) {
            throw new NotFoundException("USER_NOT_FOUND", "User with id " + memberId + " not found");
        }

        if (user.getOrganizationId() == null || !user.getOrganizationId().equals(organizationId)) {
            throw new ValidationException("UNAUTHORIZED",
                "User is not a member of this organization");
        }

        return user;
    }

    public User updateMemberRole(String orgId, User currentUser, String memberId, UpdateMemberRoleRequest request) {
        // 1. Validate organization exists dan user adalah owner
        Organization organization = getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, currentUser, "owner");

        // 2. Validate new role
        validateMemberRole(request.role());

        // 3. Get member
        User member = getMemberById(orgId, memberId, currentUser);

        // 4. Cannot change owner role
        if ("owner".equals(member.getOrganizationRole())) {
            throw new ValidationException("CANNOT_CHANGE_OWNER",
                "Cannot change the role of organization owner");
        }

        // 5. Update role menggunakan DocumentUpdater
        DocumentUpdater updater = DocumentUpdater.update()
            .set("organizationRole", request.role());
        member.executeUpdate(updater.build());

        return member;
    }

    public void removeMember(String orgId, User currentUser, String memberId) {
        // 1. Validate organization exists dan remover punya akses
        Organization organization = getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, currentUser, "owner", "admin");

        // 2. Get member
        User member = getMemberById(orgId, memberId, currentUser);

        // 3. Cannot remove owner
        if ("owner".equals(member.getOrganizationRole())) {
            throw new ValidationException("CANNOT_REMOVE_OWNER",
                "Cannot remove organization owner");
        }

        // 4. Admin tidak bisa remove admin lain atau owner
        if ("admin".equals(currentUser.getOrganizationRole()) && "admin".equals(member.getOrganizationRole())) {
            throw new ValidationException("INSUFFICIENT_ROLE",
                "Admin cannot remove another admin");
        }

        // 5. Clear organization info dari member
        clearOrganizationInfo(member);

        // 6. Decrement seats used
        decrementSeatsUsed(organization);
    }

    public User joinOrganization(String orgId, User currentUser) {
        // 1. Validate organization exists
        Organization organization = getOrganizationById(orgId);

        // 2. Validasi: Pastikan user belum tergabung dalam organisasi manapun
        if (currentUser.getOrganizationId() != null) {
            throw new ValidationException("ALREADY_IN_ORGANIZATION", 
                "User is already a member of an organization. Please leave your current organization first.");
        }

        // 3. Siapkan role ORGANIZATION untuk user
        List<String> roles = currentUser.getRoles();
        if (roles == null) {
            roles = new java.util.ArrayList<>();
        }
        if (!roles.contains("ORGANIZATION")) {
            roles.add("ORGANIZATION");
        }

        // 4. Update data User menggunakan DocumentUpdater
        DocumentUpdater updater = DocumentUpdater.update()
            .set("organizationId", organization.getId())
            .set("organizationName", organization.getName())
            .set("organizationRole", "member") // Default role saat join
            .set("accountType", User.AccountType.ORGANIZATION)
            .set("roles", roles);

        currentUser.executeUpdate(updater.build());

        // 5. Increment seats used pada Organization
        incrementSeatsUsed(organization);

        // 6. Update state object di memory agar return valuenya sesuai (untuk response API)
        currentUser.setOrganizationId(organization.getId());
        currentUser.setOrganizationName(organization.getName());
        currentUser.setOrganizationRole("member");
        currentUser.setAccountType(User.AccountType.ORGANIZATION);
        currentUser.setRoles(roles);

        return currentUser;
    }

    public User leaveOrganization(String orgId, User currentUser) {
        // 1. Validate organization exists
        Organization organization = getOrganizationById(orgId);

        // 2. Get member (user leaves themselves)
        User member = getMemberById(orgId, currentUser.getId().toHexString(), currentUser);

        // 3. Owner cannot leave
        if ("owner".equals(member.getOrganizationRole())) {
            throw new ValidationException("OWNER_CANNOT_LEAVE",
                "Owner cannot leave organization. Transfer ownership first.");
        }

        // 4. Clear organization info
        clearOrganizationInfo(member);

        // 5. Decrement seats used
        decrementSeatsUsed(organization);

        return member;
    }

    /**
     * Clear organization-related fields dari user.
     *
     * @param user User yang akan di-clear organization info nya
     */
    private void clearOrganizationInfo(User user) {
        DocumentUpdater updater = DocumentUpdater.update()
            .set("organizationId", null)
            .set("organizationName", null)
            .set("organizationRole", null)
            .set("invitedBy", null)
            .set("invitedOrganizationId", null)
            .set("invitationStatus", null)
            .set("invitationSentAt", null)
            .set("invitationAcceptedAt", null)
            .set("invitationRole", null)
            .set("inviteCode", null)
            .set("accountType", User.AccountType.INDIVIDUAL);
        
        // Remove ORGANIZATION role jika ada
        List<String> roles = user.getRoles();
        if (roles != null) {
            roles.remove("ORGANIZATION");
            updater.set("roles", roles);
        }

        user.executeUpdate(updater.build());
    }

    /**
     * Decrement seats used pada organization.
     *
     * @param organization Organization entity
     */
    private void decrementSeatsUsed(Organization organization) {
        int currentSeats = Objects.requireNonNullElse(organization.getSeatsUsed(), 0);
        if (currentSeats > 0) {
            DocumentUpdater updater = DocumentUpdater.update()
                .set("seatsUsed", currentSeats - 1);
            organization.executeUpdate(updater.build());
        }
    }

    /**
     * Validate role untuk member organization.
     *
     * @param role Role yang akan divalidasi
     */
    private void validateMemberRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationException("INVALID_ROLE", "Role is required");
        }

        for (String validRole : VALID_MEMBER_ROLES) {
            if (validRole.equals(role)) {
                return;
            }
        }

        throw new ValidationException("INVALID_ROLE",
            "Role must be either 'member' or 'admin'");
    }

    /**
     * Get organization by ID with validation.
     *
     * @param orgId Organization ID
     * @return Organization entity
     * @throws NotFoundException if organization not found
     */
    private Organization getOrganizationById(String orgId) {
        ValidationUtils.validateObjectId(orgId);
        Organization organization = Organization.findById(new ObjectId(orgId));
        if (organization == null) {
            throw new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization with id " + orgId + " not found");
        }
        return organization;
    }

    private void incrementSeatsUsed(Organization organization) {
        int currentSeats = Objects.requireNonNullElse(organization.getSeatsUsed(), 0);
        
        // Validasi: Cek apakah kursi masih tersedia (jika seats tidak null / unlimited)
        if (organization.getSeats() != null && currentSeats >= organization.getSeats()) {
            throw new ValidationException("SEATS_FULL", "Organization has reached its maximum seats limit");
        }

        DocumentUpdater updater = DocumentUpdater.update()
            .set("seatsUsed", currentSeats + 1);
        organization.executeUpdate(updater.build());
    }
}
