package com.psycorp.psychapi.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.psycorp.psychapi.api.dto.OrganizationMemberRequests.InviteMemberRequest;
import com.psycorp.psychapi.api.dto.OrganizationMemberRequests.MembersListRequest;
import com.psycorp.psychapi.api.dto.OrganizationMemberRequests.UpdateMemberRoleRequest;
import com.psycorp.psychapi.common.util.FilterCombiner;
import com.psycorp.psychapi.common.util.FilterParser;
import com.psycorp.psychapi.common.util.ObjectIdValidator;
import com.psycorp.psychapi.common.util.SearchBuilder;
import com.psycorp.psychapi.common.util.SortBuilder;
import com.psycorp.psychapi.domain.model.Organization;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service untuk mengelola members dalam organization.
 * Menyediakan fitur list, search, filter, sort, pagination, invite, update role, remove, dan leave.
 */
@ApplicationScoped
public class OrganizationMemberService {

    // Fields yang bisa di-search untuk member
    private static final String[] SEARCH_FIELDS = {"fullName", "email"};

    // Valid organization member roles
    private static final String[] VALID_MEMBER_ROLES = {"member", "admin"};

    @Inject
    OrganizationService organizationService;

    @Inject
    UserService userService;

    /**
     * Mendapatkan daftar members organization dengan pagination, search, sort, dan filter.
     * Hanya owner dan admin yang bisa melihat semua members.
     *
     * @param orgId Organization ID
     * @param userId User ID yang melakukan request
     * @param request Query parameters untuk pagination, search, sort, filter
     * @return List of User yang merupakan member organization
     */
    public List<User> getMembersByOrganizationId(String orgId, String userId, MembersListRequest request) {
        // 1. Validate organization exists dan user memiliki akses sebagai owner/admin
        Organization organization = organizationService.getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, userId, "owner", "admin");

        // 2. Build base filter: organizationId = orgId
        Bson baseFilter = org.bson.Document.parse(
            "{\"organizationId\": {\"$oid\": \"" + orgId + "\"}}"
        );

        // 3. Build search filter pada fullName dan email
        Bson searchFilter = SearchBuilder.build(request.search(), SEARCH_FIELDS);

        // 4. Parse custom filter
        Bson customFilter = FilterParser.parse(request.filter());

        // 5. Combine all filters
        Bson finalFilter = FilterCombiner.combine(baseFilter, searchFilter, customFilter);

        // 6. Build sort
        Bson sort = SortBuilder.build(request.sortBy(), request.sortOrder());

        // 7. Execute query dengan pagination
        PanacheQuery<User> query = User.find(finalFilter, sort);
        query.page(request.page() - 1, request.limit());

        return query.list();
    }

    /**
     * Mendapatkan total count members organization berdasarkan search dan filter.
     *
     * @param orgId Organization ID
     * @param search Search keyword
     * @param filter Custom filter string
     * @return Total count members
     */
    public long getMembersCount(String orgId, String search, String filter) {
        ObjectIdValidator.validate(orgId);

        Bson baseFilter = org.bson.Document.parse(
            "{\"organizationId\": {\"$oid\": \"" + orgId + "\"}}"
        );

        Bson searchFilter = SearchBuilder.build(search, SEARCH_FIELDS);
        Bson customFilter = FilterParser.parse(filter);

        Bson finalFilter = FilterCombiner.combine(baseFilter, searchFilter, customFilter);

        return User.count(finalFilter);
    }

    /**
     * Mendapatkan detail member berdasarkan userId dan organizationId.
     * User yang melakukan request harus merupakan member organization yang sama.
     *
     * @param orgId Organization ID
     * @param memberId User ID member yang dicari
     * @param requesterId User ID yang melakukan request
     * @return User member
     */
    public User getMemberById(String orgId, String memberId, String requesterId) {
        ObjectId organizationId = ObjectIdValidator.validate(orgId);
        ObjectId targetMemberId = ObjectIdValidator.validate(memberId);

        // Validate requester adalah member organization ini
        User requester = userService.getUserById(requesterId);
        if (requester.getOrganizationId() == null || !requester.getOrganizationId().equals(organizationId)) {
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

    /**
     * Invite member baru ke organization.
     * Jika email sudah terdaftar dan belum punya organization, set invitation fields.
     * Jika email belum terdaftar, create placeholder user dengan invitation status pending.
     *
     * @param orgId Organization ID
     * @param inviterId User ID yang menginvite
     * @param request Invite request
     * @return User yang diinvite
     */
    public User inviteMember(String orgId, String inviterId, InviteMemberRequest request) {
        // 1. Validate organization exists dan inviter punya akses
        Organization organization = organizationService.getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, inviterId, "owner", "admin");

        // 2. Validate role
        validateMemberRole(request.role());

        // 3. Check apakah email sudah menjadi member organization ini
        User existingMember = User.find("email", request.email().trim().toLowerCase()).firstResult();
        if (existingMember != null && existingMember.getOrganizationId() != null
                && existingMember.getOrganizationId().equals(organization.id)) {
            throw new ValidationException("EMAIL_ALREADY_MEMBER",
                "Email is already a member of this organization");
        }

        // 4. Check seats availability (skip untuk trial dengan seats = -1)
        if (organization.getSeats() != null && organization.getSeats() > 0) {
            int seatsUsed = organization.getSeatsUsed() != null ? organization.getSeatsUsed() : 0;
            
            if (seatsUsed >= organization.getSeats()) {
                throw new ValidationException("SEATS_LIMIT_REACHED",
                    "Organization has reached maximum member seats limit");
            }
        }

        // 5. Generate invitation code
        String inviteCode = generateInviteCode();

        if (existingMember != null) {
            // Email sudah terdaftar tapi belum di organization ini
            // Update user dengan invitation info
            existingMember.setInvitedBy(new ObjectId(inviterId));
            existingMember.setInvitedOrganizationId(organization.id);
            existingMember.setInvitationStatus("pending");
            existingMember.setInvitationSentAt(Instant.now());
            existingMember.setInvitationRole(request.role());
            existingMember.setInviteCode(inviteCode);
            existingMember.setUpdatedAt(Instant.now());
            existingMember.update();

            return existingMember;
        }

        // 6. Email belum terdaftar, buat placeholder user
        User invitedUser = new User();
        invitedUser.setEmail(request.email().trim().toLowerCase());
        invitedUser.setFullName(request.email().split("@")[0]); // Default name dari email prefix
        invitedUser.setProvider("invitation");
        invitedUser.setRoles(List.of("USER"));
        invitedUser.setStatus("active");
        invitedUser.setAccountType(User.AccountType.INDIVIDUAL);
        invitedUser.setInvitedBy(new ObjectId(inviterId));
        invitedUser.setInvitedOrganizationId(organization.id);
        invitedUser.setInvitationStatus("pending");
        invitedUser.setInvitationSentAt(Instant.now());
        invitedUser.setInvitationRole(request.role());
        invitedUser.setInviteCode(inviteCode);
        invitedUser.setCreatedAt(Instant.now());
        invitedUser.setUpdatedAt(Instant.now());
        invitedUser.persist();

        // 7. Update seats used
        incrementSeatsUsed(organization);

        return invitedUser;
    }

    /**
     * Update role member organization.
     * Hanya owner yang bisa update role.
     *
     * @param orgId Organization ID
     * @param ownerId User ID owner yang melakukan update
     * @param memberId User ID member yang diupdate
     * @param request Update role request
     * @return User member yang sudah diupdate
     */
    public User updateMemberRole(String orgId, String ownerId, String memberId, UpdateMemberRoleRequest request) {
        // 1. Validate organization exists dan user adalah owner
        Organization organization = organizationService.getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, ownerId, "owner");

        // 2. Validate new role
        validateMemberRole(request.role());

        // 3. Get member
        User member = getMemberById(orgId, memberId, ownerId);

        // 4. Cannot change owner role
        if ("owner".equals(member.getOrganizationRole())) {
            throw new ValidationException("CANNOT_CHANGE_OWNER",
                "Cannot change the role of organization owner");
        }

        // 5. Update role
        member.setOrganizationRole(request.role());
        member.setUpdatedAt(Instant.now());
        member.update();

        return member;
    }

    /**
     * Remove member dari organization.
     * Owner dan admin bisa remove member. Owner tidak bisa di-remove.
     *
     * @param orgId Organization ID
     * @param removerId User ID yang melakukan remove
     * @param memberId User ID member yang di-remove
     * @param confirmation Confirmation text
     * @return true jika berhasil
     */
    public boolean removeMember(String orgId, String removerId, String memberId, String confirmation) {
        // 1. Validate confirmation
        if (!"REMOVE_MEMBER".equals(confirmation)) {
            throw new ValidationException("CONFIRMATION_MISMATCH",
                "Confirmation text must be 'REMOVE_MEMBER'");
        }

        // 2. Validate organization exists dan remover punya akses
        Organization organization = organizationService.getOrganizationById(orgId);
        organizationService.validateOrganizationAccess(organization, removerId, "owner", "admin");

        // 3. Get member
        User member = getMemberById(orgId, memberId, removerId);

        // 4. Cannot remove owner
        if ("owner".equals(member.getOrganizationRole())) {
            throw new ValidationException("CANNOT_REMOVE_OWNER",
                "Cannot remove organization owner");
        }

        // 5. Admin tidak bisa remove admin lain atau owner
        User remover = userService.getUserById(removerId);
        if ("admin".equals(remover.getOrganizationRole()) && "admin".equals(member.getOrganizationRole())) {
            throw new ValidationException("INSUFFICIENT_ROLE",
                "Admin cannot remove another admin");
        }

        // 6. Clear organization info dari member
        clearOrganizationInfo(member);

        // 7. Decrement seats used
        decrementSeatsUsed(organization);

        return true;
    }

    /**
     * Member meninggalkan organization.
     * Owner tidak bisa leave, harus transfer ownership dulu.
     *
     * @param orgId Organization ID
     * @param userId User ID yang ingin leave
     * @return User yang sudah leave organization
     */
    public User leaveOrganization(String orgId, String userId) {
        // 1. Validate organization exists
        Organization organization = organizationService.getOrganizationById(orgId);

        // 2. Get member
        User member = getMemberById(orgId, userId, userId);

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
        user.setOrganizationId(null);
        user.setOrganizationName(null);
        user.setOrganizationRole(null);
        user.setInvitedBy(null);
        user.setInvitedOrganizationId(null);
        user.setInvitationStatus(null);
        user.setInvitationSentAt(null);
        user.setInvitationAcceptedAt(null);
        user.setInvitationRole(null);
        user.setInviteCode(null);

        // Remove ORGANIZATION role jika ada
        List<String> roles = user.getRoles();
        if (roles != null) {
            roles.remove("ORGANIZATION");
            user.setRoles(roles);
        }

        // Reset account type ke INDIVIDUAL
        user.setAccountType(User.AccountType.INDIVIDUAL);
        user.setUpdatedAt(Instant.now());
        user.update();
    }

    /**
     * Increment seats used pada organization.
     *
     * @param organization Organization entity
     */
    private void incrementSeatsUsed(Organization organization) {
        int currentSeats = organization.getSeatsUsed() != null ? organization.getSeatsUsed() : 0;
        organization.setSeatsUsed(currentSeats + 1);
        organization.setUpdatedAt(Instant.now());
        organization.update();
    }

    /**
     * Decrement seats used pada organization.
     *
     * @param organization Organization entity
     */
    private void decrementSeatsUsed(Organization organization) {
        int currentSeats = organization.getSeatsUsed() != null ? organization.getSeatsUsed() : 0;
        if (currentSeats > 0) {
            organization.setSeatsUsed(currentSeats - 1);
            organization.setUpdatedAt(Instant.now());
            organization.update();
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
     * Generate unique invitation code.
     *
     * @return Unique invitation code
     */
    private String generateInviteCode() {
        return "INV" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
