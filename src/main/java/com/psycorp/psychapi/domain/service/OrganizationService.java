package com.psycorp.psychapi.domain.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.psycorp.psychapi.api.dto.OrganizationRequests.CreateOrganizationRequest;
import com.psycorp.psychapi.api.dto.OrganizationRequests.UpdateOrganizationRequest;
import com.psycorp.psychapi.common.util.DocumentUpdater;
import com.psycorp.psychapi.common.util.ObjectIdValidator;
import com.psycorp.psychapi.common.util.SortBuilder;
import com.psycorp.psychapi.domain.model.Organization;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrganizationService {

    // Fields yang bisa di-search untuk Organization
    private static final String[] SEARCH_FIELDS = {"name", "description", "email", "address"};

    // Trial period dalam hari
    private static final int TRIAL_DAYS = 14;

    @Inject
    UserService userService;

    public Organization createOrganization(String userId, CreateOrganizationRequest request) {
        // 1. Validate user exists
        User user = userService.getUserById(userId);

        // 2. Validate user doesn't already own an organization (optional: allow multiple)
        // Uncomment jika ingin membatasi 1 organization per user
        // if (hasOrganization(userId)) {
        //     throw new ValidationException("ORG_ALREADY_EXISTS",
        //         "User already owns an organization");
        // }

        // 3. Validate request data
        validateCreateRequest(request);

        // 4. Create organization entity
        Organization organization = new Organization();
        organization.setName(request.name().trim());
        organization.setDescription(request.description());
        organization.setWebsite(request.website());
        organization.setPhone(request.phone());
        organization.setEmail(request.email());
        organization.setAddress(request.address());
        organization.setOwnerId(user.id);
        organization.setPlan("free_trial");
        organization.setStatus(true);
        organization.setTrialStartsAt(Instant.now());
        organization.setTrialEndsAt(Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS));
        organization.setSeats(-1); // Unlimited untuk trial
        organization.setSeatsUsed(1); // Owner adalah member pertama
        organization.setCreatedAt(Instant.now());
        organization.setUpdatedAt(Instant.now());

        // 5. Persist organization
        organization.persist();

        // 6. Update user dengan organization info
        user.setOrganizationId(organization.id);
        user.setOrganizationName(organization.getName());
        user.setOrganizationRole("owner");
        user.setAccountType(User.AccountType.ORGANIZATION);

        // 7. Update roles jika belum punya ORGANIZATION role
        List<String> roles = user.getRoles();
        if (roles == null) {
            roles = new ArrayList<>();
        }
        if (!roles.contains("ORGANIZATION")) {
            roles.add("ORGANIZATION");
            user.setRoles(roles);
        }
        user.setInviteCode(generateInviteCode());
        user.setUpdatedAt(Instant.now());
        user.update();

        return organization;
    }

    public User getOrganizationOwner(Organization organization) {
        return userService.getUserById(organization.getOwnerId().toHexString());
    }

    public Organization getOrganizationById(String orgId) {
        ObjectId objectId = ObjectIdValidator.validate(orgId);

        Organization organization = Organization.findById(objectId);
        if (organization == null) {
            throw new NotFoundException("ORG_NOT_FOUND", "Organization with id " + orgId + " not found");
        }
        return organization;
    }

    public List<Organization> getOrganizationsByUserId(String userId, int page, int limit, String sortBy, String sortOrder) {
        // Build filter: ownerId = userId OR organizationId di user (untuk member)
        Bson ownerFilter = org.bson.Document.parse("{\"ownerId\": {\"$oid\": \"" + userId + "\"}}");

        // Untuk saat ini, query berdasarkan ownerId
        Bson finalFilter = ownerFilter;

        // Build sort
        Bson sort = SortBuilder.build(sortBy, sortOrder);

        // Execute query dengan pagination
        PanacheQuery<Organization> query = Organization.find(finalFilter, sort);
        query.page(page - 1, limit);

        return query.list();
    }

    public long getOrganizationsCountByUserId(String userId) {
        Bson ownerFilter = org.bson.Document.parse("{\"ownerId\": {\"$oid\": \"" + userId + "\"}}");
        return Organization.count(ownerFilter);
    }

    public Organization updateOrganization(String orgId, String userId, UpdateOrganizationRequest request) {
        // 1. Get organization
        Organization organization = getOrganizationById(orgId);

        // 2. Validate user has permission
        validateOrganizationAccess(organization, userId, "owner", "admin");

        // 3. Build update document
        DocumentUpdater updater = DocumentUpdater.update()
            .set("name", request.name() != null ? request.name().trim() : null)
            .set("description", request.description())
            .set("website", request.website())
            .set("phone", request.phone())
            .set("email", request.email())
            .set("address", request.address());

        // 4. Update organization name di user jika nama berubah
        if (request.name() != null && !request.name().trim().isEmpty()) {
            User user = userService.getUserById(userId);
            if (user.getOrganizationId() != null && user.getOrganizationId().equals(organization.id)) {
                user.setOrganizationName(request.name().trim());
                user.setUpdatedAt(Instant.now());
                user.update();
            }
        }

        // 5. Execute update
        if (updater.hasChanges()) {
            organization.executeUpdate(updater.build());
        }

        return getOrganizationById(orgId);
    }

    public boolean deleteOrganization(String orgId, String userId, String confirmation) {
        // 1. Validate confirmation
        if (!"DELETE_MY_ORGANIZATION".equals(confirmation)) {
            throw new ValidationException("CONFIRMATION_MISMATCH",
                "Confirmation text must be 'DELETE_MY_ORGANIZATION'");
        }

        // 2. Get organization
        Organization organization = getOrganizationById(orgId);

        // 3. Validate user is owner
        validateOrganizationAccess(organization, userId, "owner");

        // 4. Check if organization has other members (optional)
        // Jika ada member lain, owner harus transfer ownership dulu
        if (organization.getSeatsUsed() != null && organization.getSeatsUsed() > 1) {
            throw new ValidationException("ORG_OWNER_CANNOT_DELETE",
                "Organization has other members. Transfer ownership before deleting.");
        }

        // 5. Soft delete organization
        organization.setStatus(false);
        organization.setDeletedAt(Instant.now());
        organization.setUpdatedAt(Instant.now());
        organization.update();

        // 6. Update user organization info
        User user = userService.getUserById(userId);
        if (user.getOrganizationId() != null && user.getOrganizationId().equals(organization.id)) {
            user.setOrganizationId(null);
            user.setOrganizationName(null);
            user.setOrganizationRole(null);
            user.setAccountType(User.AccountType.INDIVIDUAL);

            // Remove ORGANIZATION role
            List<String> roles = user.getRoles();
            if (roles != null) {
                roles.remove("ORGANIZATION");
                user.setRoles(roles);
            }

            user.setUpdatedAt(Instant.now());
            user.update();
        }

        return true;
    }

    public boolean hasOrganization(String userId) {
        ObjectId objectId = ObjectIdValidator.validate(userId);
        return Organization.count("ownerId", objectId) > 0;
    }

    public void validateOrganizationAccess(Organization organization, String userId, String... allowedRoles) {
        User user = userService.getUserById(userId);

        // Check if user belongs to this organization
        if (user.getOrganizationId() == null || !user.getOrganizationId().equals(organization.id)) {
            // Check if user is owner
            if (!organization.getOwnerId().equals(user.id)) {
                throw new ValidationException("UNAUTHORIZED",
                    "User does not have access to this organization");
            }
        }

        // Check role
        String userRole = user.getOrganizationRole();
        boolean hasAllowedRole = false;
        for (String role : allowedRoles) {
            if (role.equals(userRole)) {
                hasAllowedRole = true;
                break;
            }
        }

        // Owner selalu punya akses penuh
        if (!hasAllowedRole && !"owner".equals(userRole)) {
            throw new ValidationException("UNAUTHORIZED",
                "User does not have permission to perform this action. Required role: " + String.join(" or ", allowedRoles));
        }
    }

    private void validateCreateRequest(CreateOrganizationRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.name() == null || request.name().isBlank()) {
            errors.add("Organization name is required");
        } else if (request.name().trim().length() < 2) {
            errors.add("Organization name must be at least 2 characters");
        }

        if (request.email() != null && !request.email().isBlank()) {
            if (!request.email().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                errors.add("Invalid email format");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", String.join(", ", errors));
        }
    }

    private String generateInviteCode() {
        return "INV" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
