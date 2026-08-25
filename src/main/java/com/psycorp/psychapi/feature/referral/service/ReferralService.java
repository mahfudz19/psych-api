package com.psycorp.psychapi.feature.referral.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service untuk mengelola referral code system.
 * Menangani generate, validate, regenerate codes dengan embedded archive.
 * 
 * @author Architect
 */
@ApplicationScoped
public class ReferralService {
    
    
    // Characters yang digunakan untuk referral code (uppercase alphanumeric)
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    @Inject
    RateLimitService rateLimitService;
    
    /**
     * Generate referral code unik untuk user baru.
     * 
     * @param user User yang akan di-generate referral code-nya
     * @return Referral code yang di-generate
     */
    public String generateReferralCode(User user) {
        String userId = user.id != null ? user.id.toHexString() : "new";
        
        // Rate limit check untuk generation
        rateLimitService.checkCodeGenerationLimit(userId);
        
        String code;
        int maxAttempts = 10;
        int attempts = 0;
        
        do {
            code = generateRandomCode();
            attempts++;
            
            // Check uniqueness (current codes)
            User existing = User.find("referralCode", code).firstResult();
            if (existing == null) {
                // Also check archived codes
                Bson archivedQuery = Filters.eq("referralCodeHistory.code", code);
                User.find(archivedQuery).firstResult();
            }
            
        } while (attempts < maxAttempts && User.find("referralCode", code).firstResult() != null);
        
        if (attempts >= maxAttempts) {
            throw new ValidationException("REFERRAL_CODE_GENERATION_FAILED", 
                "Unable to generate unique referral code. Please try again.");
        }
        
        user.setReferralCode(code);
        
        return code;
    }
    
    /**
     * Validate referral code dan return referrer user.
     * Checks both current codes dan archived codes untuk backward compatibility.
     * 
     * @param referralCode Referral code untuk validate
     * @param ipAddress IP address dari client (untuk logging)
     * @return User referrer jika code valid
     * @throws ValidationException jika code invalid atau tidak ditemukan
     */
    public User validateReferralCode(String referralCode, String ipAddress) {
        // Handle null or unknown IP address (fallback for development or missing IP)
        if (ipAddress == null || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = "localhost";
        }
        
        // Rate limit check untuk validation
        rateLimitService.checkCodeValidationLimit(ipAddress);
        
        // Mask code untuk logging
        String maskedCode = maskCode(referralCode);
                
        if (referralCode == null || referralCode.trim().isEmpty()) {
            throw new ValidationException("INVALID_REFERRAL_CODE", 
                "Referral code is required");
        }
        
        // Step 1: Find by current referralCode
        User referrer = User.find("referralCode", referralCode.trim()).firstResult();
        if (referrer != null) {
            return referrer;
        }
        
        // Step 2: Find in referralCodeHistory (backward compatibility)
        Bson archivedQuery = Filters.eq("referralCodeHistory.code", referralCode.trim());
        referrer = User.find(archivedQuery).firstResult();
        if (referrer != null) {
            return referrer;
        }
        
        // Step 3: Not found
        throw new ValidationException("INVALID_REFERRAL_CODE", 
            "Referral code '" + maskedCode + "' is not valid");
    }
    
    /**
     * Regenerate referral code untuk user existing.
     * Code lama akan di-archive ke referralCodeHistory.
     * 
     * @param user User yang akan regenerate code
     * @param reason Alasan regenerasi (e.g., "user_request", "security", "regenerated")
     * @return New referral code
     */
    public String regenerateReferralCode(User user, String reason) {
        String userId = user.id.toHexString();
        
        // Rate limit check untuk regeneration
        rateLimitService.checkCodeRegenerationLimit(userId);
        
        String oldCode = user.getReferralCode();
        String newCode;
        
        // Generate new unique code
        int maxAttempts = 10;
        int attempts = 0;
        
        do {
            newCode = generateRandomCode();
            attempts++;
            
            // Check uniqueness
            User existing = User.find("referralCode", newCode).firstResult();
            if (existing == null) {
                Bson archivedQuery = Filters.eq("referralCodeHistory.code", newCode);
                User.find(archivedQuery).firstResult();
            }
            
        } while (attempts < maxAttempts && User.find("referralCode", newCode).firstResult() != null);
        
        if (attempts >= maxAttempts) {
            throw new ValidationException("REFERRAL_CODE_GENERATION_FAILED", 
                "Unable to generate unique referral code. Please try again.");
        }
        
        // Archive old code
        if (oldCode != null && !oldCode.isEmpty()) {
            archiveCode(user, oldCode, newCode, reason);
        }
        
        // Set new code
        user.setReferralCode(newCode);
        user.setUpdatedAt(Instant.now());
        user.update();
        
        return newCode;
    }
    
    /**
     * Archive referral code lama ke referralCodeHistory array.
     * 
     * @param user User yang code-nya akan di-archive
     * @param oldCode Code lama yang di-archive
     * @param newCode Code baru yang menggantikan
     * @param reason Alasan archiving
     */
    private void archiveCode(User user, String oldCode, String newCode, String reason) {
        // Create archive entry as Map for MongoDB push
        Map<String, Object> archiveEntry = new HashMap<>();
        archiveEntry.put("code", oldCode);
        archiveEntry.put("archivedAt", Instant.now());
        archiveEntry.put("reason", reason);
        archiveEntry.put("replacedBy", newCode);
        
        // Add to referralCodeHistory array using MongoDB update
        Bson update = Updates.push("referralCodeHistory", archiveEntry);
        User.mongoCollection().updateOne(
            Filters.eq("_id", user.id),
            update
        );
    }
    
    /**
     * Get referral history untuk user.
     * Mengembalikan current active code dan archived codes.
     *
     * @param user User yang history-nya akan diambil
     * @return Map dengan key "current" (ReferralCodeHistoryEntry) dan "archived" (List of ReferralCodeHistoryEntry)
     */
    public Map<String, Object> getReferralHistory(User user) {
        Map<String, Object> result = new HashMap<>();
        
        // Current code
        User.ReferralCodeHistoryEntry currentEntry = null;
        if (user.getReferralCode() != null) {
            currentEntry = new User.ReferralCodeHistoryEntry();
            currentEntry.setCode(user.getReferralCode());
            currentEntry.setArchivedAt(null);
            currentEntry.setReason(null);
            currentEntry.setReplacedBy(null);
        }
        result.put("current", currentEntry);
        
        // Archived codes
        List<User.ReferralCodeHistoryEntry> archivedCodes = user.getReferralCodeHistory();
        if (archivedCodes == null) {
            archivedCodes = new ArrayList<>();
        }
        result.put("archived", archivedCodes);
        
        return result;
    }
    
    /**
     * Check apakah user menggunakan referral code sendiri.
     * 
     * @param referrer User referrer
     * @param newUserEmail Email user baru yang akan register
     * @throws ValidationException jika self-referral detected
     */
    public void checkSelfReferral(User referrer, String newUserEmail) {
        if (referrer.getEmail().equals(newUserEmail)) {           
            throw new ValidationException("INVALID_REFERRAL", 
                "Cannot use your own referral code");
        }
    }
    
    /**
     * Generate random referral code.
     * 
     * @return Random alphanumeric code
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }
    
    /**
     * Mask code untuk logging (security).
     * 
     * @param code Code untuk mask
     * @return Masked code
     */
    private String maskCode(String code) {
        if (code == null || code.isEmpty()) {
            return "***";
        }
        if (code.length() <= 3) {
            return "***";
        }
        return code.substring(0, 3) + "***";
    }
}
