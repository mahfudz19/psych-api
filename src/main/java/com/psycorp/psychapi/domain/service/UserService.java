package com.psycorp.psychapi.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.psycorp.psychapi.common.util.DocumentUpdater;
import com.psycorp.psychapi.common.util.FilterCombiner;
import com.psycorp.psychapi.common.util.FilterParser;
import com.psycorp.psychapi.common.util.ObjectIdValidator;
import com.psycorp.psychapi.common.util.SearchBuilder;
import com.psycorp.psychapi.common.util.SortBuilder;
import com.psycorp.psychapi.domain.model.Organization;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserService {

    // Fields yang bisa di-search untuk User
    private static final String[] SEARCH_FIELDS = {"email", "fullName", "phone", "bio"};
    
    @Inject
    ReferralService referralService;

    public List<User> getAllUsers(String search, String filter, String sortBy, String sortOrder, int page, int limit) {
        // 1. Build search filter pada email, fullName, phone, dan bio
        Bson searchFilter = SearchBuilder.build(search, SEARCH_FIELDS);
        
        // 2. Parse custom filter (contoh: "status:active", "roles:in:USER,ORGANIZATION")
        Bson customFilter = FilterParser.parse(filter);
        
        // 3. Combine semua filters dengan $and
        Bson finalFilter = FilterCombiner.combine(searchFilter, customFilter);
        
        // 4. Build sort
        Bson sort = SortBuilder.build(sortBy, sortOrder);
        
        // 5. Execute query dengan pagination
        PanacheQuery<User> query = User.find(finalFilter, sort);
        query.page(page - 1, limit);
        
        return query.list();
    }

    public long getTotalUsersCount(String search, String filter) {
        // Build search filter
        Bson searchFilter = SearchBuilder.build(search, SEARCH_FIELDS);
        
        // Parse custom filter
        Bson customFilter = FilterParser.parse(filter);
        
        // Combine filters
        Bson finalFilter = FilterCombiner.combine(searchFilter, customFilter);
        
        return User.count(finalFilter);
    }

    public User getUserById(String id) {
        // Validate ObjectId format
        ObjectId objectId = ObjectIdValidator.validate(id);
        
        User user = User.findById(objectId);
        if (user == null) {
            throw new NotFoundException("USER_NOT_FOUND", "User with id " + id + " not found");
        }
        return user;
    }

    public User register(
        String email,
        String password,
        String fullName,
        String referralCode,
        AccountType accountType,
        String inviteCode,
        String invitedBy,
        String invitedOrganizationId,
        String invitationRole
    ) {        
        // 1. Validate user data (email format, password strength, etc)
        validateUserData(email, password, fullName, null);
        
        // 2. Validate email uniqueness (DB check)
        User existingUser = User.find("email", email).firstResult();
        if (existingUser != null) {
            throw new ValidationException("EMAIL_EXISTS",
                "Email '" + email + "' is already registered");
        }
        
        // 3. Validate referralCode using ReferralService (FAIL FAST - throw jika tidak ditemukan)
        User referrer = null;
        if (referralCode != null && !referralCode.isEmpty()) {
            referrer = referralService.validateReferralCode(referralCode, null);
            
            // Prevent self-referral
            referralService.checkSelfReferral(referrer, email);
        }
        
        // 4. Determine inviter: inviteCode OR direct add
        User inviter = null;
        org.bson.types.ObjectId orgId = null;
        String role = null;
        
        if (inviteCode != null && !inviteCode.isEmpty()) {
            // === SCENARIO A: Invite dengan code ===
            inviter = validateInviteCode(inviteCode);
            orgId = inviter.getInvitedOrganizationId();
            role = inviter.getInvitationRole() != null ? inviter.getInvitationRole() : "member";
            
        } else if (invitedBy != null && !invitedBy.isEmpty() && invitedOrganizationId != null && !invitedOrganizationId.isEmpty()) {
            // === SCENARIO B: Direct add ===
            
            // 4a. Validate invitedBy exists (DB check)
            inviter = User.findById(new org.bson.types.ObjectId(invitedBy));
            if (inviter == null) {
                throw new ValidationException("INVALID_INVITER",
                    "User who invited you does not exist");
            }
            
            // 4b. Validate organization exists (DB check)
            Organization org = Organization.findById(new org.bson.types.ObjectId(invitedOrganizationId));
            if (org == null) {
                throw new ValidationException("INVALID_ORGANIZATION",
                    "Organization does not exist");
            }
            orgId = org.id;
            
            // 4c. AUTHORIZATION: Validate inviter has permission to add members
            if (!inviter.getOrganizationId().equals(org.id)) {
                throw new ValidationException("UNAUTHORIZED",
                    "User does not belong to this organization");
            }
            if (!List.of("owner", "admin").contains(inviter.getOrganizationRole())) {
                throw new ValidationException("UNAUTHORIZED",
                    "Only organization owner or admin can add members directly. Your role: " + inviter.getOrganizationRole());
            }
            
            // 4d. Set role (default to "member" if not specified)
            role = invitationRole != null && !invitationRole.isEmpty() ? invitationRole : "member";
        }
        
        // 5. Hash password
        String hashedPassword = PasswordEncoder.hash(password);
        
        // 6. Create User object
        User user = User.create(email, hashedPassword, fullName, referrer, inviter, accountType);
        
        // 7. For direct add, override invitation info
        if (invitedBy != null && !invitedBy.isEmpty() && invitedOrganizationId != null && !invitedOrganizationId.isEmpty()) {
            user.setInvitedBy(new org.bson.types.ObjectId(invitedBy));
            user.setInvitedOrganizationId(orgId);
            user.setInvitationStatus("accepted");
            user.setInvitationRole(role);
            user.setOrganizationId(orgId);
            user.setOrganizationRole(role);
        }
        
        // 8. Persist user BARU ke database (sekali saja, tanpa update)
        user.persist();
        
        // 9. Update stats referrer LAMA
        if (referrer != null) {
            updateReferrerStats(referrer, user);
        }
        
        // 10. Update organization seats LAMA
        if (inviter != null && orgId != null) {
            updateOrganizationSeats(orgId);
        }
        
        return user;
    }

    // Note: validateReferralCode now delegated to ReferralService
    // This method is kept for backward compatibility if needed elsewhere
    @Deprecated
    private User validateReferralCode(String referralCode) {
        return referralService.validateReferralCode(referralCode, null);
    }

    private User validateInviteCode(String inviteCode) {
        User inviter = User.find("inviteCode", inviteCode).firstResult();
        if (inviter == null) {
            throw new ValidationException("INVALID_INVITE_CODE",
                "Invitation code '" + inviteCode + "' is not valid");
        }
        if (inviter.getInvitedOrganizationId() == null) {
            throw new ValidationException("INVALID_INVITE_CODE",
                "Invitation code '" + inviteCode + "' is not associated with any organization");
        }
        return inviter;
    }

    private void updateReferrerStats(User referrer, User newUser) {
        if (referrer.getReferralIds() == null) {
            referrer.setReferralIds(new ArrayList<>(List.of(newUser.id)));
        } else {
            referrer.getReferralIds().add(newUser.id);
        }
        referrer.setTotalReferrals(referrer.getTotalReferrals() + 1);
        referrer.update();
    }

    private void updateOrganizationSeats(org.bson.types.ObjectId orgId) {
        Organization org = Organization.findById(orgId);
        if (org != null) {
            org.setSeatsUsed(org.getSeatsUsed() + 1);
            org.update();
        }
    }

    public User authenticate(String email, String password) {
        // 1. Find user by email
        User user = User.find("email", email).firstResult();
        
        if (user == null) {
            throw new ValidationException("INVALID_CREDENTIALS",
                "Email or password is incorrect");
        }
        
        // 2. Check if account is suspended (hard limit reached)
        if ("suspended".equals(user.getStatus())) {
            throw new ValidationException("ACCOUNT_LOCKED",
                "Your account has been locked due to too many failed login attempts. " +
                "Please reset your password or contact support.");
        }
        
        // 3. Check if account is inactive/deleted
        if (!"active".equals(user.getStatus())) {
            throw new ValidationException("ACCOUNT_INACTIVE",
                "Your account is " + user.getStatus() + ". Please contact support.");
        }
        
        // 4. Verify password
        String hashedPassword = user.getPassword();
        if (!PasswordEncoder.verify(password, hashedPassword)) {
            // Increment login attempts
            Integer attemptsObj = user.getLoginAttempts();
            int attempts = (attemptsObj != null ? attemptsObj : 0) + 1;
            user.setLoginAttempts(attempts);
            
            // 5. Check if exceeded hard limit (16 attempts)
            if (attempts >= 16) {
                user.setStatus("suspended");
                user.update();
                throw new ValidationException("ACCOUNT_LOCKED",
                    "Your account has been locked due to too many failed login attempts. " +
                    "Please reset your password or contact support.");
            }
            
            // 6. Calculate progressive delay based on tier
            int tier = (attempts - 1) / 3;
            long delaySeconds = switch(tier) {
                case 0 -> 0;      // Attempt 1-3
                case 1 -> 5;      // Attempt 4-6
                case 2 -> 30;     // Attempt 7-9
                case 3 -> 120;    // Attempt 10-12
                case 4 -> 300;    // Attempt 13-15
                default -> 300;   // Cap at 5 minutes
            };
            
            user.update();
            
            // 7. Apply delay BEFORE throwing exception
            if (delaySeconds > 0) {
                try {
                    Thread.sleep(delaySeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            int remainingAttempts = 16 - attempts;
            String message = String.format(
                "Email or password is incorrect. %d attempts remaining before account lockout.",
                remainingAttempts
            );
            throw new ValidationException("INVALID_CREDENTIALS", message);
        }
        
        // 8. Success - reset attempts and update last login
        user.setLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        user.update();
        
        return user;
    }

    @Deprecated
    public User login(String email, String password) {
        return authenticate(email, password);
    }

    public void logout(String userId) {
        User user = getUserById(userId);
        user.setUpdatedAt(Instant.now());
        user.update();
    }

    public User createUser(String email, String password, String fullName, String phone, String bio, String referredBy) {
        validateUserData(email, password, fullName, null);
        
        // Hash password sebelum menyimpan ke database
        String hashedPassword = PasswordEncoder.hash(password);
        
        // Find referrer jika ada referredBy
        User referrer = null;
        if (referredBy != null && !referredBy.isEmpty()) {
            referrer = User.findById(new org.bson.types.ObjectId(referredBy));
        }
        
        User user = User.create(email, hashedPassword, fullName, referrer, null, AccountType.INDIVIDUAL);
        
        // Set optional fields
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone);
        }
        if (bio != null && !bio.isBlank()) {
            user.setBio(bio);
        }
        
        user.persist();
        return user;
    }

    public User updateUser(String id, String email, String fullName, String phone, String bio, String status) {
        User user = getUserById(id);

        DocumentUpdater updater = DocumentUpdater.update()
            .set("email", email)
            .set("fullName", fullName)
            .set("phone", phone)
            .set("bio", bio)
            .set("status", status);

        if (updater.hasChanges()) {
            user.executeUpdate(updater.build());
        }
        
        return getUserById(user.id.toHexString());
    }

    public boolean deleteUser(String id) {
        User user = getUserById(id);
        user.delete();
        return true;
    }

    public User softDeleteUser(String id) {
        User user = getUserById(id);
        user.softDelete();
        user.update();
        return user;
    }

    private void validateUserData(String email, String password, String fullName, String status) {
        List<String> errors = new ArrayList<>();
        
        // Email validation
        if (email == null || email.isBlank()) {
            errors.add("Email is required");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("Invalid email format");
        } else if (email.length() > 255) {
            errors.add("Email must not exceed 255 characters");
        }

        // Email validation (hanya untuk create)
        if (email != null) {
            User existingUser = User.find("email", email).firstResult();
            if (existingUser != null) {
                errors.add("Email already exists");
            }
        }
        
        // Password validation (hanya untuk create)
        if (password != null) {
            if (password.length() < 8) {
                errors.add("Password must be at least 8 characters");
            } else if (password.length() > 100) {
                errors.add("Password must not exceed 100 characters");
            }
        }
        
        // Full name validation
        if (fullName != null) {
            if (fullName.length() < 2) {
                errors.add("Full name must be at least 2 characters");
            } else if (fullName.length() > 100) {
                errors.add("Full name must not exceed 100 characters");
            }
        }
        
        // Status validation
        if (status != null && !status.matches("^(active|inactive|suspended|deleted)$")) {
            errors.add("Status must be one of: active, inactive, suspended, deleted");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", String.join(", ", errors));
        }
    }

}
