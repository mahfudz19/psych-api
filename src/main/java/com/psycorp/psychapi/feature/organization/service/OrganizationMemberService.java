package com.psycorp.psychapi.feature.organization.service;

import java.util.List;
import java.util.Objects;

import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.shared.util.DocumentUpdater;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrganizationMemberService {

    public void removeMember(Organization organization, User member, User currentUser) {
        // 1. Cannot remove owner
        if (User.OrganizationRole.OWNER.equals(member.getOrganizationRole())) {
            throw new ValidationException("CANNOT_REMOVE_OWNER", "Cannot remove organization owner");
        }

        // 2. Admin tidak bisa remove admin lain atau owner
        if (User.OrganizationRole.ADMIN.equals(currentUser.getOrganizationRole()) && User.OrganizationRole.ADMIN.equals(member.getOrganizationRole())) {
            throw new ValidationException("INSUFFICIENT_ROLE", "Admin cannot remove another admin");
        }

        // 3. Clear organization info dari member
        clearOrganizationInfo(member);

        // 4. Decrement seats used
        decrementSeatsUsed(organization);
    }

    public User joinOrganization(Organization organization, User currentUser) {
        // 1. Validasi: Pastikan user belum tergabung dalam organisasi manapun
        if (currentUser.getOrganizationId() != null) {
            throw new ValidationException("ALREADY_IN_ORGANIZATION", "User is already a member of an organization. Please leave your current organization first.");
        }

        // 2. Siapkan role ORGANIZATION untuk user
        List<User.Role> roles = currentUser.getRoles();
        if (roles == null) roles = new java.util.ArrayList<>();
        if (!roles.contains(User.Role.ORGANIZATION)) roles.add(User.Role.ORGANIZATION);

        // 3. Update data User menggunakan DocumentUpdater
        DocumentUpdater updater = DocumentUpdater.update()
            .set("organizationId", organization.getId())
            .set("organizationName", organization.getName())
            .set("organizationRole", "member")
            .set("accountType", User.AccountType.ORGANIZATION)
            .set("roles", roles);

        currentUser.executeUpdate(updater.build());

        // 4. Increment seats used pada Organization
        incrementSeatsUsed(organization);

        // 5. Update state object di memory agar return valuenya sesuai (untuk response API)
        currentUser.setOrganizationId(organization.getId());
        currentUser.setOrganizationName(organization.getName());
        currentUser.setOrganizationRole(User.OrganizationRole.MEMBER);
        currentUser.setAccountType(User.AccountType.ORGANIZATION);
        currentUser.setRoles(roles);

        return currentUser;
    }

    public User leaveOrganization(Organization organization, User member) {
        // 1. Owner cannot leave
        if (User.OrganizationRole.OWNER.equals(member.getOrganizationRole())) {
            throw new ValidationException("OWNER_CANNOT_LEAVE",
                "Owner cannot leave organization. Transfer ownership first.");
        }

        // 2. Clear organization info
        clearOrganizationInfo(member);

        // 3. Decrement seats used
        decrementSeatsUsed(organization);

        return member;
    }

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
        List<User.Role> roles = user.getRoles();
        if (roles != null) {
            roles.remove(User.Role.ORGANIZATION);
            updater.set("roles", roles);
        }

        user.executeUpdate(updater.build());
    }

    private void decrementSeatsUsed(Organization organization) {
        int currentSeats = Objects.requireNonNullElse(organization.getSeatsUsed(), 0);
        if (currentSeats > 0) {
            DocumentUpdater updater = DocumentUpdater.update()
                .set("seatsUsed", currentSeats - 1);
            organization.executeUpdate(updater.build());
        }
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
