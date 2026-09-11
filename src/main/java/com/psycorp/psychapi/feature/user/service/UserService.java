package com.psycorp.psychapi.feature.user.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.referral.service.ReferralService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.feature.user.model.User.AccountType;
import com.psycorp.psychapi.feature.user.model.User.Status;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserService implements PanacheMongoRepository<User> {
    
    @Inject
    ReferralService referralService;

    public User register(String email, String password, String fullName, String referralCode, AccountType accountType, String inviteCode, String invitedBy, String invitedOrganizationId, String invitationRole, String hashedVerificationToken, Instant verificationExpiresAt, String ip) {
        // 1. Validate user data (email format, password strength, etc)
        validateUserData(email, fullName);
        
        // 2. Validate email uniqueness (DB check)
        User existingUser = User.find("email", email).firstResult();
        if (existingUser != null) {
            throw new ValidationException("EMAIL_EXISTS", "Email '" + email + "' is already registered");
        }
        
        // 3. Validate referralCode using ReferralService (FAIL FAST - throw jika tidak ditemukan)
        User referrer = null;
        if (referralCode != null && !referralCode.isEmpty()) {
            referrer = referralService.validateReferralCode(referralCode, ip);
            
            // Prevent self-referral
            referralService.checkSelfReferral(referrer, email);
        }
        
        // 4. Determine inviter: inviteCode OR direct add
        User inviter = null;
        org.bson.types.ObjectId orgId = null;
        User.OrganizationRole role = null;
        
        if (inviteCode != null && !inviteCode.isEmpty()) {
            // === SCENARIO A: Invite dengan code ===
            inviter = validateInviteCode(inviteCode);
            orgId = inviter.getOrganizationId();
            role =  User.OrganizationRole.member;
            
        } else if (invitedBy != null && !invitedBy.isEmpty() && invitedOrganizationId != null && !invitedOrganizationId.isEmpty()) {
            // === SCENARIO B: Direct add ===
            
            // 4a. Validate invitedBy exists (DB check)
            inviter = User.findById(new org.bson.types.ObjectId(invitedBy));
            if (inviter == null) {
                throw new ValidationException("INVALID_INVITER", "User who invited you does not exist");
            }
            
            // 4b. Validate organization exists (DB check)
            Organization org = Organization.findById(new org.bson.types.ObjectId(invitedOrganizationId));
            if (org == null) {
                throw new ValidationException("INVALID_ORGANIZATION", "Organization does not exist");
            }
            orgId = org.id;
            
            // 4c. AUTHORIZATION: Validate inviter has permission to add members
            if (!inviter.getOrganizationId().equals(org.id)) {
                throw new ValidationException("UNAUTHORIZED", "User does not belong to this organization");
            }
            if (!List.of(User.OrganizationRole.owner, User.OrganizationRole.admin).contains(inviter.getOrganizationRole())) {
                throw new ValidationException("UNAUTHORIZED", "Only organization owner or admin can add members directly. Your role: " + inviter.getOrganizationRole());
            }
            role =  User.OrganizationRole.member;
        }
        
        // 5. Create User object
        User user = User.create(email, password, fullName, referrer, inviter, accountType, hashedVerificationToken, verificationExpiresAt);
        
        // 6. For direct add, override invitation info
        if (inviteCode != null && !inviteCode.isEmpty() && inviter != null) {
            user.setInvitedBy(inviter.getId());
            user.setOrganizationRole(role);
            user.setOrganizationId(orgId);
            
        } else if (invitedBy != null && !invitedBy.isEmpty() && invitedOrganizationId != null && !invitedOrganizationId.isEmpty()) {
            user.setInvitedBy(new org.bson.types.ObjectId(invitedBy));
            user.setOrganizationRole(role);
            user.setOrganizationId(orgId);
        }
        
        // 7. Persist user BARU ke database (sekali saja, tanpa update)
        user.persist();
        
        // 8. Update stats referrer LAMA
        if (referrer != null) {
            updateReferrerStats(referrer, user);
        }
        
        // 9. Update organization seats LAMA
        if (inviter != null && orgId != null) {
            updateOrganizationSeats(orgId);
        }
        
        return user;
    }

    public User login(String email, String password) {
        // 1. Find user by email
        User user = User.find("email", email).firstResult();
        
        if (user == null) {
            throw new ValidationException("INVALID_CREDENTIALS", "Email or password is incorrect");
        }
        
        // 2. Check if account is suspended (hard limit reached)
        if (User.Status.SUSPENDED.equals(user.getStatus())) {
            throw new ValidationException("ACCOUNT_LOCKED", "Your account has been locked due to too many failed login attempts.  Please reset your password or contact support.");
        }
        
        // 3. Check if account is inactive/deleted
        if (!User.Status.ACTIVE.equals(user.getStatus())) {
            throw new ValidationException("ACCOUNT_INACTIVE", "Your account is " + user.getStatus() + ". Please contact support.");
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
                user.setStatus(Status.SUSPENDED);
                user.update();
                throw new ValidationException("ACCOUNT_LOCKED", "Your account has been locked due to too many failed login attempts. Please reset your password or contact support.");
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

    private User validateInviteCode(String inviteCode) {
        User inviter = User.find("inviteCode", inviteCode).firstResult();
        if (inviter == null) {
            throw new ValidationException("INVALID_INVITE_CODE", "Invitation code '" + inviteCode + "' is not valid");
        }
        if (inviter.getOrganizationId() == null) {
            throw new ValidationException("INVALID_INVITE_CODE", "Invitation code '" + inviteCode + "' is not associated with any organization");
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

    private void validateUserData(String email, String fullName) {
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
        
        // Full name validation
        if (fullName != null) {
            if (fullName.length() < 2) {
                errors.add("Full name must be at least 2 characters");
            } else if (fullName.length() > 100) {
                errors.add("Full name must not exceed 100 characters");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", String.join(", ", errors));
        }
    }
}
