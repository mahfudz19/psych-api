package com.psycorp.psychapi.infrastructure.database.migration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Migration Dev: Insert dummy data untuk development dan testing.
 * Hanya aktif pada profile 'dev' atau 'test'.
 *
 * Data yang di-insert:
 * - 9 users dengan berbagai role dan subscription tier
 * - 4 organizations dengan berbagai plan (free_trial, free, pro, enterprise)
 * - Referral relationships antar users (referralIds, referredBy, referredAt)
 * - Referral earnings tracking (referralEarnings)
 * - Organization memberships (organizationId, organizationRole, organizationName)
 * - Invitation tracking (inviteCode, invitedBy, invitedOrganizationId, invitationStatus, dll)
 * - Revenue share percentage untuk organization owners
 * - Trial period untuk organization free_trial (trialStartsAt, trialEndsAt)
 * - Subscription expiry untuk premium/enterprise users
 *
 * Referral Chain:
 * - individualFree → individualPremium → individualEnterprise
 * - ownerPro → adminMember → regularMember
 *
 * Organizations:
 * - PT Startup Trial (free_trial, 14 days trial)
 * - CV Usaha Gratis (free plan, max 5 seats)
 * - PT Perusahaan Pro (pro plan, 50 seats, 3 members)
 * - PT Korporasi Enterprise (enterprise plan, unlimited seats)
 */
@IfBuildProfile("dev")
@ChangeUnit(id = "Dev__Insert_Dummy_Data", order = "999", author = "mahfudz")
public class Dev__Insert_Dummy_Data {

    private static final String DEFAULT_PASSWORD = "password123";

    /**
     * Execution method untuk insert dummy data.
     * 
     * @param mongoDatabase Database MongoDB untuk operasi seeding
     */
    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        MongoCollection<Document> organizations = mongoDatabase.getCollection("organizations");
        
        // Cek idempotency - skip jika sudah ada data
        if (users.countDocuments(new Document("email", "individual.free@example.com")) > 0) {
            return;
        }
        
        // === STEP 1: Insert Users ===
        List<Document> allUsers = new ArrayList<>();
        
        // 1. Individual Free User
        ObjectId user1Id = new ObjectId();
        Document user1 = createUserDocument(
            user1Id,
            "individual.free@example.com",
            "Individual Free User",
            "INDIVIDUAL",
            List.of("USER"),
            "free",
            "+6281234567890",
            "Individual free user, belum berlangganan",
            null, // referredBy
            null, // invitedBy
            null,  // invitedOrganizationId
            0.0   // referralEarnings
        );
        allUsers.add(user1);
        
        // 2. Individual Premium User (referred by user1)
        ObjectId user2Id = new ObjectId();
        Document user2 = createUserDocument(
            user2Id,
            "individual.premium@example.com",
            "Individual Premium User",
            "INDIVIDUAL",
            List.of("USER"),
            "premium",
            "+6281234567891",
            "Individual premium user, berlangganan pribadi",
            user1Id, // referredBy
            null,
            null,
            0.0   // referralEarnings
        );
        user2.put("subscriptionExpiry", Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days
        allUsers.add(user2);
        
        // 3. Individual Enterprise User (referred by user2)
        ObjectId user3Id = new ObjectId();
        Document user3 = createUserDocument(
            user3Id,
            "individual.enterprise@example.com",
            "Individual Enterprise User",
            "INDIVIDUAL",
            List.of("USER"),
            "enterprise",
            "+6281234567892",
            "Individual enterprise user dengan fitur lengkap",
            user2Id, // referredBy
            null,
            null,
            0.0   // referralEarnings
        );
        user3.put("subscriptionExpiry", Instant.now().plusSeconds(365 * 24 * 60 * 60)); // 365 days
        allUsers.add(user3);
        
        // 4. Owner Trial User (ORGANIZATION)
        ObjectId user4Id = new ObjectId();
        Document user4 = createUserDocument(
            user4Id,
            "owner.trial@example.com",
            "Owner Trial User",
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "free",
            "+6281234567893",
            "Founder & CEO PT Startup Trial",
            null,
            null,
            null,
            0.0   // referralEarnings
        );
        user4.put("organizationRole", "owner");
        allUsers.add(user4);
        
        // 5. Owner Free User (ORGANIZATION)
        ObjectId user5Id = new ObjectId();
        Document user5 = createUserDocument(
            user5Id,
            "owner.free@example.com",
            "Owner Free User",
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "free",
            "+6281234567894",
            "Owner CV Usaha Gratis",
            null,
            null,
            null,
            0.0   // referralEarnings
        );
        user5.put("organizationRole", "owner");
        allUsers.add(user5);
        
        // 6. Owner Pro User (ORGANIZATION)
        ObjectId user6Id = new ObjectId();
        Document user6 = createUserDocument(
            user6Id,
            "owner.pro@example.com",
            "Owner Pro User",
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "premium",
            "+6281234567895",
            "CEO PT Perusahaan Pro",
            null,
            null,
            null,
            50000.0   // referralEarnings - Pro owner dapat Rp 50.000 per referral
        );
        user6.put("organizationRole", "owner");
        user6.put("revenueSharePercentage", 15);
        allUsers.add(user6);
        
        // 7. Owner Enterprise User (ORGANIZATION)
        ObjectId user7Id = new ObjectId();
        Document user7 = createUserDocument(
            user7Id,
            "owner.enterprise@example.com",
            "Owner Enterprise User",
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "enterprise",
            "+6281234567896",
            "President Director PT Korporasi Enterprise",
            null,
            null,
            null,
            0.0   // referralEarnings - Enterprise owner tidak dapat referral earnings
        );
        user7.put("organizationRole", "owner");
        user7.put("revenueSharePercentage", 20);
        allUsers.add(user7);
        
        // 8. Admin Member User (referred by user6, invited to org)
        ObjectId user8Id = new ObjectId();
        Document user8 = createUserDocument(
            user8Id,
            "admin.member@example.com",
            "Organization Admin User",
            "INDIVIDUAL",
            List.of("USER"),
            "free",
            "+6281234567897",
            "HR Manager di PT Perusahaan Pro",
            user6Id, // referredBy
            user6Id, // invitedBy
            null,    // invitedOrganizationId (set later)
            25000.0  // referralEarnings - Member dapat Rp 25.000 per referral
        );
        user8.put("inviteCode", "INV-PRO-ADMIN");
        user8.put("invitationStatus", "accepted");
        user8.put("invitationRole", "admin");
        user8.put("invitationSentAt", Instant.now());
        user8.put("invitationAcceptedAt", Instant.now());
        user8.put("organizationRole", "admin");
        allUsers.add(user8);
        
        // 9. Regular Member User (referred by user8, invited to org)
        ObjectId user9Id = new ObjectId();
        Document user9 = createUserDocument(
            user9Id,
            "regular.member@example.com",
            "Organization Member User",
            "INDIVIDUAL",
            List.of("USER"),
            "free",
            "+6281234567898",
            "Software Engineer di PT Perusahaan Pro",
            user8Id, // referredBy
            user8Id, // invitedBy
            null,    // invitedOrganizationId (set later)
            0.0      // referralEarnings - Regular member tidak punya referral
        );
        user9.put("inviteCode", "INV-PRO-MEMBER");
        user9.put("invitationStatus", "accepted");
        user9.put("invitationRole", "member");
        user9.put("invitationSentAt", Instant.now());
        user9.put("invitationAcceptedAt", Instant.now());
        user9.put("organizationRole", "member");
        allUsers.add(user9);
        
        // Insert all users
        users.insertMany(allUsers);
        
        // === STEP 2: Update referralIds dan referredAt ===
        // user1 referred user2
        users.updateOne(
            new Document("_id", user1Id),
            new Document("$set", new Document("referralIds", List.of(user2Id)))
        );
        users.updateOne(
            new Document("_id", user2Id),
            new Document("$set", new Document("referredAt", Instant.now()))
        );
        
        // user2 referred user3
        users.updateOne(
            new Document("_id", user2Id),
            new Document("$set", new Document("referralIds", List.of(user3Id)))
        );
        users.updateOne(
            new Document("_id", user3Id),
            new Document("$set", new Document("referredAt", Instant.now()))
        );
        
        // user6 referred user8
        users.updateOne(
            new Document("_id", user6Id),
            new Document("$set", new Document("referralIds", List.of(user8Id)))
        );
        users.updateOne(
            new Document("_id", user8Id),
            new Document("$set", new Document("referredAt", Instant.now()))
        );
        
        // user8 referred user9
        users.updateOne(
            new Document("_id", user8Id),
            new Document("$set", new Document("referralIds", List.of(user9Id)))
        );
        users.updateOne(
            new Document("_id", user9Id),
            new Document("$set", new Document("referredAt", Instant.now()))
        );
        
        // Update totalReferrals
        users.updateOne(new Document("_id", user1Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        users.updateOne(new Document("_id", user2Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        users.updateOne(new Document("_id", user6Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        users.updateOne(new Document("_id", user8Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        
        // === STEP 3: Insert Organizations ===
        List<Document> allOrgs = new ArrayList<>();
        
        // 1. PT Startup Trial
        ObjectId org1Id = new ObjectId();
        Document org1 = createOrganizationDocument(
            org1Id,
            "PT Startup Trial",
            "Startup dalam masa trial 14 hari",
            "https://startup-trial.example.com",
            "https://example.com/logos/startup-trial.png",
            "Jl. Startup No. 1, Jakarta",
            "+622112345678",
            "contact@startup-trial.example.com",
            true,
            "free_trial",
            user4Id, // ownerId
            999,
            1
        );
        org1.put("trialStartsAt", Instant.now());
        org1.put("trialEndsAt", Instant.now().plusSeconds(14 * 24 * 60 * 60)); // 14 days
        allOrgs.add(org1);
        
        // 2. CV Usaha Gratis
        ObjectId org2Id = new ObjectId();
        Document org2 = createOrganizationDocument(
            org2Id,
            "CV Usaha Gratis",
            "Organisasi dengan plan gratis, max 5 member",
            "https://usaha-gratis.example.com",
            "https://example.com/logos/usaha-gratis.png",
            "Jl. Gratis No. 5, Bandung",
            "+622298765432",
            "contact@usaha-gratis.example.com",
            true,
            "free",
            user5Id, // ownerId
            5,
            1
        );
        allOrgs.add(org2);
        
        // 3. PT Perusahaan Pro
        ObjectId org3Id = new ObjectId();
        Document org3 = createOrganizationDocument(
            org3Id,
            "PT Perusahaan Pro",
            "Perusahaan profesional dengan 50 seats",
            "https://perusahaan-pro.example.com",
            "https://example.com/logos/perusahaan-pro.png",
            "Jl. Pro No. 50, Surabaya",
            "+623155566677",
            "contact@perusahaan-pro.example.com",
            true,
            "pro",
            user6Id, // ownerId
            50,
            3
        );
        org3.put("subscriptionExpiry", Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days
        allOrgs.add(org3);
        
        // 4. PT Korporasi Enterprise
        ObjectId org4Id = new ObjectId();
        Document org4 = createOrganizationDocument(
            org4Id,
            "PT Korporasi Enterprise",
            "Korporasi besar dengan unlimited seats dan SSO",
            "https://korporasi-enterprise.example.com",
            "https://example.com/logos/korporasi-enterprise.png",
            "Jl. Enterprise No. 999, Jakarta Selatan",
            "+622188899900",
            "contact@korporasi-enterprise.example.com",
            true,
            "enterprise",
            user7Id, // ownerId
            9999,
            1
        );
        org4.put("subscriptionExpiry", Instant.now().plusSeconds(365 * 24 * 60 * 60)); // 365 days
        org4.put("approvedBy", "admin_001");
        org4.put("approvedAt", Instant.now());
        allOrgs.add(org4);
        
        // Insert all organizations
        organizations.insertMany(allOrgs);
        
        // === STEP 4: Update users dengan organizationId dan organizationName ===
        // User 4 (owner.trial) -> PT Startup Trial
        users.updateOne(
            new Document("_id", user4Id),
            new Document("$set", new Document("organizationId", org1Id)
                .append("organizationName", "PT Startup Trial"))
        );
        
        // User 5 (owner.free) -> CV Usaha Gratis
        users.updateOne(
            new Document("_id", user5Id),
            new Document("$set", new Document("organizationId", org2Id)
                .append("organizationName", "CV Usaha Gratis"))
        );
        
        // User 6 (owner.pro) -> PT Perusahaan Pro
        users.updateOne(
            new Document("_id", user6Id),
            new Document("$set", new Document("organizationId", org3Id)
                .append("organizationName", "PT Perusahaan Pro"))
        );
        
        // User 7 (owner.enterprise) -> PT Korporasi Enterprise
        users.updateOne(
            new Document("_id", user7Id),
            new Document("$set", new Document("organizationId", org4Id)
                .append("organizationName", "PT Korporasi Enterprise"))
        );
        
        // User 8 (admin.member) -> PT Perusahaan Pro
        users.updateOne(
            new Document("_id", user8Id),
            new Document("$set", new Document("organizationId", org3Id)
                .append("organizationName", "PT Perusahaan Pro")
                .append("invitedOrganizationId", org3Id))
        );
        
        // User 9 (regular.member) -> PT Perusahaan Pro
        users.updateOne(
            new Document("_id", user9Id),
            new Document("$set", new Document("organizationId", org3Id)
                .append("organizationName", "PT Perusahaan Pro")
                .append("invitedOrganizationId", org3Id))
        );
    }

    /**
     * Rollback execution untuk menghapus dummy data.
     * 
     * @param mongoDatabase Database MongoDB untuk operasi rollback
     */
    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        MongoCollection<Document> organizations = mongoDatabase.getCollection("organizations");
        
        // Delete dummy users (exclude superadmin)
        users.deleteMany(new Document("email", new Document("$in", Arrays.asList(
            "individual.free@example.com",
            "individual.premium@example.com",
            "individual.enterprise@example.com",
            "owner.trial@example.com",
            "owner.free@example.com",
            "owner.pro@example.com",
            "owner.enterprise@example.com",
            "admin.member@example.com",
            "regular.member@example.com"
        ))));
        
        // Delete dummy organizations
        organizations.deleteMany(new Document("name", new Document("$in", Arrays.asList(
            "PT Startup Trial",
            "CV Usaha Gratis",
            "PT Perusahaan Pro",
            "PT Korporasi Enterprise"
        ))));
    }

    private String getPasswordHash(String password) {
        return PasswordEncoder.hash(password);
    }
    
    /**
     * Helper method untuk membuat user document.
     *
     * @param id ObjectId user
     * @param email Email user
     * @param fullName Nama lengkap
     * @param accountType Tipe akun (INDIVIDUAL atau ORGANIZATION)
     * @param roles List role user
     * @param subscriptionTier Tier subscription (free, premium, enterprise)
     * @param phone Nomor telepon
     * @param bio Bio/deskripsi user
     * @param referredBy ObjectId user yang merefer
     * @param invitedBy ObjectId user yang invite
     * @param invitedOrganizationId ObjectId organization yang di-invite
     * @param referralEarnings Pendapatan dari referral (default 0.0)
     * @return Document user yang sudah lengkap
     */
    private Document createUserDocument(ObjectId id, String email, String fullName,
                                        String accountType, List<String> roles,
                                        String subscriptionTier, String phone,
                                        String bio, ObjectId referredBy,
                                        ObjectId invitedBy, ObjectId invitedOrganizationId,
                                        double referralEarnings) {
        
        return new Document("_id", id)
            .append("email", email)
            .append("password", getPasswordHash(DEFAULT_PASSWORD))
            .append("fullName", fullName)
            .append("provider", "local")
            .append("providerId", null)
            .append("profilePicture", null)
            .append("phone", phone)
            .append("bio", bio)
            .append("dateOfBirth", null)
            .append("gender", null)
            .append("roles", roles)
            .append("organizationId", null)
            .append("organizationRole", null)
            .append("organizationName", null)
            .append("subscriptionTier", subscriptionTier)
            .append("subscriptionExpiry", null)
            .append("revenueSharePercentage", 0)
            .append("referralCode", generateReferralCode(email, Instant.now()))
            .append("referredBy", referredBy)
            .append("referralIds", new ArrayList<>())
            .append("totalReferrals", 0)
            .append("successfulReferrals", 0)
            .append("referralEarnings", referralEarnings)
            .append("referredAt", null)
            .append("inviteCode", null)
            .append("invitedBy", invitedBy)
            .append("invitedOrganizationId", invitedOrganizationId)
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
            .append("accountType", accountType);
    }
    
    /**
     * Helper method untuk membuat organization document.
     */
    private Document createOrganizationDocument(ObjectId id, String name, String description,
                                                 String website, String logo, String address,
                                                 String phone, String email, Boolean status,
                                                 String plan, ObjectId ownerId, 
                                                 Integer seats, Integer seatsUsed) {
        return new Document("_id", id)
            .append("name", name)
            .append("description", description)
            .append("website", website)
            .append("logo", logo)
            .append("address", address)
            .append("phone", phone)
            .append("email", email)
            .append("status", status)
            .append("approvedBy", null)
            .append("approvedAt", null)
            .append("rejectionReason", null)
            .append("trialStartsAt", null)
            .append("trialEndsAt", null)
            .append("plan", plan)
            .append("subscriptionExpiry", null)
            .append("seats", seats)
            .append("seatsUsed", seatsUsed)
            .append("ownerId", ownerId)
            .append("createdAt", Instant.now())
            .append("updatedAt", Instant.now())
            .append("deletedAt", null);
    }
    
    /**
     * Generate unique referral code dari email dan timestamp.
     */
    private String generateReferralCode(String email, Instant createdAt) {
        if (email == null || email.isEmpty()) {
            return "USR" + createdAt.getEpochSecond() + (int)(Math.random() * 1000);
        }
        
        String alphaOnly = email.replaceAll("[^a-zA-Z]", "");
        String prefix = alphaOnly.substring(0, Math.min(3, alphaOnly.length())).toUpperCase();
        
        String timestamp = String.valueOf(createdAt.getEpochSecond());
        String timeSuffix = timestamp.length() > 5 
                ? timestamp.substring(timestamp.length() - 5) 
                : timestamp;
        
        String randomSuffix = String.format("%03d", (int)(Math.random() * 1000));
        
        return prefix + timeSuffix + randomSuffix;
    }
}
