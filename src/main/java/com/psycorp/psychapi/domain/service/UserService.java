package com.psycorp.psychapi.domain.service;

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
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service untuk mengelola operasi CRUD pada User.
 * Menangani validasi, pencarian, filter, dan pagination.
 */
@ApplicationScoped
public class UserService {

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

    public User createUser(String email, String password, String fullName, String phone, String bio, String referredBy) {
        validateUserData(email, password, fullName, null);
        
        // Hash password sebelum menyimpan ke database
        String hashedPassword = PasswordEncoder.hash(password);
        
        User user = User.create(email, hashedPassword, fullName, referredBy);
        
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

    /**
     * Update user dengan field-field yang disediakan.
     * Field dengan nilai null akan dihapus dari database.
     *
     * @param id User ID
     * @param email Email (null = tidak diubah)
     * @param fullName Full name (null = tidak diubah)
     * @param phone Phone (null = tidak diubah)
     * @param bio Bio (null = tidak diubah)
     * @param status Status (null = tidak diubah)
     * @param loginAttempts Login attempts (null = hapus field dari database)
     * @return Updated user
     */
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

    /**
     * Verify password user dengan cara compare plain text password
     * dengan hashed password di database.
     *
     * @param userId User ID
     * @param password Password plain text yang akan diverifikasi
     * @return true jika password match, false jika tidak
     */
    public boolean verifyPassword(String userId, String password) {
        User user = getUserById(userId);
        String hashedPassword = user.getPassword();
        
        return PasswordEncoder.verify(password, hashedPassword);
    }

    /**
     * Update password user dengan password baru.
     * Method ini akan hash password baru sebelum menyimpan ke database.
     *
     * @param userId User ID
     * @param oldPassword Password lama (untuk verifikasi)
     * @param newPassword Password baru yang akan di-set
     * @return Updated user
     *
     * @throws ValidationException jika password lama salah
     */
    public User updatePassword(String userId, String oldPassword, String newPassword) {
        // Verify old password dulu
        if (!verifyPassword(userId, oldPassword)) {
            throw new ValidationException("INVALID_PASSWORD", "Current password is incorrect");
        }
        
        // Validate new password
        validateUserData(null, newPassword, null, null);
        
        // Hash new password
        String hashedPassword = PasswordEncoder.hash(newPassword);
        
        User user = getUserById(userId);
        user.setPassword(hashedPassword);
        user.update();
        
        return getUserById(userId);
    }

    /**
     * Reset password user (hanya untuk admin).
     * Method ini tidak memerlukan verifikasi password lama.
     *
     * @param userId User ID
     * @param newPassword Password baru yang akan di-set
     * @return Updated user
     */
    public User resetPassword(String userId, String newPassword) {
        // Validate new password
        validateUserData(null, newPassword, null, null);
        
        // Hash new password
        String hashedPassword = PasswordEncoder.hash(newPassword);
        
        User user = getUserById(userId);
        user.setPassword(hashedPassword);
        user.update();
        
        return getUserById(userId);
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
