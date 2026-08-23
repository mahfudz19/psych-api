package com.psycorp.psychapi.feature.organization.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.organization.api.dto.request.CreateOrganizationRequest;
import com.psycorp.psychapi.feature.organization.api.dto.request.UpdateOrganizationRequest;
import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.feature.user.service.UserService;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.shared.util.DocumentUpdater;
import com.psycorp.psychapi.shared.util.MongoFilter;
import com.psycorp.psychapi.shared.util.ValidationUtils;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrganizationService {

    // Fields yang bisa di-search untuk Organization
    // private static final String[] SEARCH_FIELDS = {"name", "description", "email", "address"};

    // Trial period dalam hari
    private static final int TRIAL_DAYS = 14;

    @Inject
    UserService userService;

    public Organization createOrganization(User user, CreateOrganizationRequest request) {
        boolean isSuperAdmin = isSuperAdmin(user);
        // 1. Validate user doesn't already own an organization (optional: allow multiple)
        // Uncomment jika ingin membatasi 1 organization per user
        // if (hasOrganization(userId)) {
        //     throw new ValidationException("ORG_ALREADY_EXISTS",
        //         "User already owns an organization");
        // }

        // 2. Validate request data        
        validateCreateRequest(request);

        // 3. Create organization entity
        Organization organization = new Organization();
        organization.setName(request.name().trim());
        organization.setDescription(request.description());
        organization.setWebsite(request.website());
        organization.setPhone(request.phone());
        organization.setEmail(request.email());
        organization.setAddress(request.address());
        if (!isSuperAdmin) organization.setOwnerId(user.id);
        organization.setPlan("free_trial");
        organization.setStatus(true);
        organization.setTrialStartsAt(Instant.now());
        organization.setTrialEndsAt(Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS));
        organization.setSeats(-1); // Unlimited untuk trial
        organization.setSeatsUsed(1); // Owner adalah member pertama
        organization.setCreatedAt(Instant.now());
        organization.setUpdatedAt(Instant.now());

        // 4. Persist organization
        organization.persist();

        if (!isSuperAdmin) {
            // 5. Update user dengan organization info
            user.setOrganizationId(organization.id);
            user.setOrganizationName(organization.getName());
            user.setOrganizationRole("owner");
            user.setAccountType(User.AccountType.ORGANIZATION);
    
            // 6. Update roles jika belum punya ORGANIZATION role
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
        }

        return organization;
    }

    public User getOrganizationOwner(Organization organization) {
        return userService.getUserById(organization.getOwnerId().toHexString());
    }

    public Organization getOrganizationById(String orgId, User user) {
        Organization organization = getOrganizationByIdInternal(orgId);
        validateOrganizationAccess(organization, user, "owner", "admin", "member");

        return organization;
    }

    public List<Organization> getOrganizations(User user, int page, int limit, String sortBy, String sortOrder) {
        boolean isSuperAdmin = user.getRoles() != null && user.getRoles().contains("SUPERADMIN");
        
        Bson finalFilter;
        if (isSuperAdmin) {
            finalFilter = new Document();
        } else {
            finalFilter = Filters.eq("ownerId", user.id);
        }

        Bson sort = MongoFilter.sort(sortBy, sortOrder);
        PanacheQuery<Organization> query = Organization.find(finalFilter, sort);
        query.page(page - 1, limit);
        
        return query.list();
    }

    public long getOrganizationsCount(User user) {
        boolean isSuperAdmin = user.getRoles() != null && user.getRoles().contains("SUPERADMIN");
        
        if (isSuperAdmin) {
            return Organization.count();
        } else {
            return Organization.count("ownerId", user.id);
        }
    }

    public Organization updateOrganization(String orgId, User user, UpdateOrganizationRequest request) {
        // 1. Get organization
        Organization organization = getOrganizationById(orgId, user);

        // 2. Validate user has permission
        validateOrganizationAccess(organization, user, "owner", "admin");

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

        return organization;
    }

    public boolean deleteOrganization(String orgId, User user, String confirmation) {
        // 1. Validate confirmation
        if (!"DELETE_MY_ORGANIZATION".equals(confirmation)) {
            throw new ValidationException("CONFIRMATION_MISMATCH",
                "Confirmation text must be 'DELETE_MY_ORGANIZATION'");
        }

        // 2. Get organization
        Organization organization = getOrganizationByIdInternal(orgId);

        // 3. Validate user is owner
        validateOrganizationAccess(organization, user, "owner");

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
        ObjectId objectId = ValidationUtils.validateObjectId(userId);
        return Organization.count("ownerId", objectId) > 0;
    }

    public void validateOrganizationAccess(Organization organization, User user, String... allowedRoles) {
        if (user.getRoles() != null && user.getRoles().contains("SUPERADMIN")) {
            return; // Akses granted tanpa validasi lebih lanjut
        }

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
            throw new ValidationException(
                "UNAUTHORIZED",
                "User does not have permission to perform this action. Required role: " + String.join(" or ", allowedRoles)
            );
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

    private boolean isSuperAdmin(User user) {
        return user.getRoles() != null && user.getRoles().contains("SUPERADMIN");
    }

    private Organization getOrganizationByIdInternal(String orgId) {
        ObjectId objectId = ValidationUtils.validateObjectId(orgId);
        Organization organization = Organization.findById(objectId);
        if (organization == null) {
            throw new NotFoundException("ORG_NOT_FOUND", "Organization with id " + orgId + " not found");
        }
        return organization;
    }

}
