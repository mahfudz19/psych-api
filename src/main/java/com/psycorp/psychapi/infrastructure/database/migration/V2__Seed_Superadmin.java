package com.psycorp.psychapi.infrastructure.database.migration;

import java.time.Instant;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * Migration V2: Seed akun Superadmin dengan struktur User yang lengkap.
 * Membuat akun superadmin default dengan email admin@psycorp.com.
 */
@ChangeUnit(id = "V2__Seed_Superadmin", order = "002", author = "mahfudz")
public class V2__Seed_Superadmin {

    private static final String SUPERADMIN_EMAIL = "admin@psycorp.com";
    private static final String DEFAULT_PASSWORD = "admin123";

    /**
     * Execution method untuk insert akun superadmin jika belum ada.
     * 
     * @param mongoDatabase Database MongoDB untuk operasi seeding
     */
    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        
        // Cek apakah superadmin sudah ada
        long count = users.countDocuments(new Document("email", SUPERADMIN_EMAIL));
        if (count > 0) {
            // Superadmin sudah ada, skip seeding
            return;
        }
        
        // Generate referral code untuk superadmin
        String referralCode = generateReferralCode(SUPERADMIN_EMAIL, Instant.now());
        
        // Buat dokumen superadmin dengan struktur lengkap sesuai model User
        Document superadmin = new Document()
                .append("email", SUPERADMIN_EMAIL)
                .append("password", hashPassword(DEFAULT_PASSWORD))
                .append("fullName", "Super Admin")
                .append("provider", "local")
                .append("providerId", null)
                .append("profilePicture", null)
                .append("phone", null)
                .append("bio", null)
                .append("dateOfBirth", null)
                .append("gender", null)
                .append("roles", java.util.Arrays.asList("SUPERADMIN", "USER", "ORGANIZATION"))
                .append("organizationId", null)
                .append("organizationRole", null)
                .append("organizationName", null)
                .append("subscriptionTier", "enterprise")
                .append("subscriptionExpiry", null)
                .append("revenueSharePercentage", 0)
                .append("referralCode", referralCode)
                .append("referredBy", null)
                .append("referralIds", new java.util.ArrayList<>())
                .append("totalReferrals", 0)
                .append("successfulReferrals", 0)
                .append("referralEarnings", 0.0)
                .append("referredAt", null)
                .append("inviteCode", null)
                .append("invitedBy", null)
                .append("invitedOrganizationId", null)
                .append("invitationStatus", null)
                .append("invitationSentAt", null)
                .append("invitationAcceptedAt", null)
                .append("invitationRole", null)
                .append("status", "ACTIVE")
                .append("lastLoginAt", null)
                .append("loginAttempts", 0)
                .append("createdAt", Instant.now())
                .append("updatedAt", Instant.now())
                .append("deletedAt", null)
                .append("accountType", "ORGANIZATION");
        
        users.insertOne(superadmin);
    }

    /**
     * Rollback execution untuk menghapus akun superadmin jika migration gagal.
     * 
     * @param mongoDatabase Database MongoDB untuk operasi rollback
     */
    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        users.deleteOne(new Document("email", SUPERADMIN_EMAIL));
    }
    
    /**
     * Hash password menggunakan BCrypt.
     * 
     * @param password Password plain text
     * @return Hashed password
     */
    private String hashPassword(String password) {
        // Menggunakan BCrypt dengan work factor 10 (default yang aman)
        return org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(10));
    }
    
    /**
     * Generate unique referral code dari email dan timestamp.
     * Format: 3 chars prefix + 5 digits timestamp + 3 digits random
     * 
     * @param email User email
     * @param createdAt Creation timestamp
     * @return Unique 11-character referral code
     */
    private String generateReferralCode(String email, Instant createdAt) {
        if (email == null || email.isEmpty()) {
            return "ADM" + createdAt.getEpochSecond() + (int)(Math.random() * 1000);
        }
        
        // Extract first 3 alphabetic characters untuk prefix
        String alphaOnly = email.replaceAll("[^a-zA-Z]", "");
        String prefix = alphaOnly.substring(0, Math.min(3, alphaOnly.length())).toUpperCase();
        
        // Use last 5 digits of timestamp
        String timestamp = String.valueOf(createdAt.getEpochSecond());
        String timeSuffix = timestamp.length() > 5 
                ? timestamp.substring(timestamp.length() - 5) 
                : timestamp;
        
        // Add 3-digit random number untuk uniqueness (000-999)
        String randomSuffix = String.format("%03d", (int)(Math.random() * 1000));
        
        return prefix + timeSuffix + randomSuffix;
    }
}
