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
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.infrastructure.security.JwtService;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserService {

    @Inject
    JwtService jwtService;

    // Fields yang bisa di-search untuk User
    private static final String[] SEARCH_FIELDS = {"email", "fullName", "phone", "bio"};

    public List<User> getAllUsers(String search, String filter, String sortBy, String sortOrder, int page, int limit) {
        // 1. Build search filter pada email, fullName, phone, dan bio
        Bson searchFilter = SearchBuilder.build(search, SEARCH_FIELDS);
        
        // 2. Parse custom filter (contoh: "status:active", "roles:in:USER,ADMIN")
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

    public User register(String email, String password, String fullName, String referredBy, AccountType accountType) {
        if (referredBy != null && !referredBy.isEmpty()) {
            // Todo: Validate referredBy
            throw new ValidationException(
                "FEATURE_NOT_AVAILABLE",
                "Referral system is not available yet. Please register without referredBy."
            );
        }

        // Validate user data
        validateUserData(email, password, fullName, null, referredBy);
        
        // Hash password sebelum menyimpan ke database
        String hashedPassword = PasswordEncoder.hash(password);
        
        User user = User.create(email, hashedPassword, fullName, referredBy, accountType);
        
        // Persist user ke database
        user.persist();

        return user;
    }

    public User login(String email, String password) {
        // Cari user berdasarkan email
        User user = User.find("email", email).firstResult();
        
        if (user == null) {
            throw new ValidationException("INVALID_CREDENTIALS", "Email or password is incorrect");
        }
        
        // Cek status user
        if (!"active".equals(user.getStatus())) {
            throw new ValidationException("ACCOUNT_INACTIVE", "Your account is " + user.getStatus());
        }
        
        // Verify password
        String hashedPassword = user.getPassword();
        if (!PasswordEncoder.verify(password, hashedPassword)) {
            // Increment login attempts
            Integer attempts = user.getLoginAttempts() != null ? user.getLoginAttempts() : 0;
            user.setLoginAttempts(attempts + 1);
            user.update();
            
            throw new ValidationException("INVALID_CREDENTIALS", "Email or password is incorrect");
        }
        
        // Reset login attempts dan update last login
        user.setLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        user.update();
        
        return user;
    }

    public void logout(String userId, Boolean allDevices) {
        // Dalam production, invalidate token di database/redis
        // Untuk sekarang, cukup update last logout time
        User user = getUserById(userId);
        user.setUpdatedAt(Instant.now());
        user.update();
    }

    public boolean forgotPassword(String email) {
        // Cari user berdasarkan email
        User user = User.find("email", email).firstResult();
        
        // Selalu return true untuk security (tidak mengungkap apakah email terdaftar)
        if (user == null) {
            return true;
        }
        
        // Generate reset token (dalam production, simpan di database dengan expiry)
        String resetToken = java.util.UUID.randomUUID().toString();
        
        // TODO: Kirim email dengan reset token
        // emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        
        return true;
    }

    public User resetPasswordWithToken(String token, String newPassword) {
        // TODO: Dalam production, validasi token dari database
        // Untuk sekarang, langsung reset password
        validateUserData(null, newPassword, null, null, null);
        
        // Hash new password
        String hashedPassword = PasswordEncoder.hash(newPassword);
        
        // TODO: Cari user berdasarkan token yang valid
        // Untuk sekarang, throw exception karena token belum diimplementasikan
        throw new ValidationException("NOT_IMPLEMENTED", "Reset password with token belum diimplementasikan");
    }

    public User verifyEmail(String token) {
        // TODO: Dalam production, validasi token dari database
        // Untuk sekarang, throw exception karena token belum diimplementasikan
        throw new ValidationException("NOT_IMPLEMENTED", "Verify email dengan token belum diimplementasikan");
    }

    public void resendVerificationEmail(String email, String userId) {
        // Cari user berdasarkan email
        User user = User.find("email", email).firstResult();
        
        if (user == null) {
            // Tidak melakukan apa-apa untuk security
            return;
        }
        
        // Generate verification token
        String verificationToken = java.util.UUID.randomUUID().toString();
        
        // TODO: Kirim email verifikasi
        // emailService.sendVerificationEmail(user.getEmail(), verificationToken);
    }

    public User createUser(String email, String password, String fullName, String phone, String bio, String referredBy) {
        validateUserData(email, password, fullName, null, null);
        
        // Hash password sebelum menyimpan ke database
        String hashedPassword = PasswordEncoder.hash(password);
        
        User user = User.create(email, hashedPassword, fullName, referredBy, AccountType.INDIVIDUAL);
        
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

    public User deleteUserField(String id, String fieldName) {
        User user = getUserById(id);
        
        if (!fieldName.equals("phone") && !fieldName.equals("bio")) {
            throw new ValidationException("INVALID_FIELD", "Field '" + fieldName + "' cannot be deleted. Allowed fields: phone, bio");
        }
        
        user.update();
        
        return user;
    }

    public boolean verifyPassword(String userId, String password) {
        User user = getUserById(userId);
        String hashedPassword = user.getPassword();
        
        return PasswordEncoder.verify(password, hashedPassword);
    }

    public User updatePassword(String userId, String oldPassword, String newPassword) {
        // Verify old password dulu
        if (!verifyPassword(userId, oldPassword)) {
            throw new ValidationException("INVALID_PASSWORD", "Current password is incorrect");
        }
        
        // Validate new password
        validateUserData(null, newPassword, null, null, null);
        
        // Hash new password
        String hashedPassword = PasswordEncoder.hash(newPassword);
        
        User user = getUserById(userId);
        user.setPassword(hashedPassword);
        user.update();
        
        return getUserById(userId);
    }

    public User resetPassword(String userId, String newPassword) {
        // Validate new password
        validateUserData(null, newPassword, null, null, null);
        
        // Hash new password
        String hashedPassword = PasswordEncoder.hash(newPassword);
        
        User user = getUserById(userId);
        user.setPassword(hashedPassword);
        user.update();
        
        return getUserById(userId);
    }

    private void validateUserData(String email, String password, String fullName, String status, String referredBy) {
        List<String> errors = new ArrayList<>();
        
        // Email validation
        if (email == null || email.isBlank()) {
            errors.add("Email is required");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("Invalid email format");
        } else if (email.length() > 255) {
            errors.add("Email must not exceed 255 characters");
        }

        if (referredBy != null && !referredBy.isEmpty()) {
            if (!referredBy.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                errors.add("Invalid referredBy email format");
            } else if (referredBy.length() > 255) {
                errors.add("ReferredBy email must not exceed 255 characters");
            } 
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
