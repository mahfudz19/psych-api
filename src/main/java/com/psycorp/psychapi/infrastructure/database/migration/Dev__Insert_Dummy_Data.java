package com.psycorp.psychapi.infrastructure.database.migration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.ConfigProvider;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import io.quarkus.arc.profile.IfBuildProfile;

@IfBuildProfile("dev")
@ChangeUnit(id = "Dev__Insert_Dummy_Data", order = "999", author = "mahfudz")
public class Dev__Insert_Dummy_Data {

    private static final String DEFAULT_PASSWORD = "password123";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        String activeProfile = ConfigProvider.getConfig().getValue("quarkus.profile", String.class);
        if (!"dev".equals(activeProfile) && !"test".equals(activeProfile)) {
            return;
        }
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        MongoCollection<Document> organizations = mongoDatabase.getCollection("organizations");
        MongoCollection<Document> subscriptionPlans = mongoDatabase.getCollection("subscription_plans");
        MongoCollection<Document> subscriptions = mongoDatabase.getCollection("subscriptions");
        
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
            "+6281234567890",
            "Individual free user, belum berlangganan",
            null, null, null, 0.0
        );
        allUsers.add(user1);
        
        // 2. Individual Basic User (referred by user1)
        ObjectId user2Id = new ObjectId();
        Document user2 = createUserDocument(
            user2Id,
            "individual.basic@example.com",
            "Individual Basic User",
            "INDIVIDUAL",
            List.of("USER"),
            "+6281234567891",
            "Individual basic user, berlangganan bulanan",
            user1Id, null, null, 0.0
        );
        allUsers.add(user2);
        
        // 3. Individual Premium User (referred by user2)
        ObjectId user3Id = new ObjectId();
        Document user3 = createUserDocument(
            user3Id,
            "individual.premium@example.com",
            "Individual Premium User",
            "INDIVIDUAL",
            List.of("USER"),
            "+6281234567892",
            "Individual premium user, berlangganan tahunan",
            user2Id, null, null, 0.0
        );
        allUsers.add(user3);
        
        // 4. Owner Trial User (ORGANIZATION)
        ObjectId user4Id = new ObjectId();
        Document user4 = createUserDocument(
            user4Id,
            "owner.trial@example.com",
            "Owner Trial User",
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "+6281234567893",
            "Founder & CEO PT Startup Trial",
            null, null, null, 0.0
        );
        user4.put("organizationRole", "owner");
        allUsers.add(user4);
        
        // 5. Owner Starter User (ORGANIZATION)
        ObjectId user5Id = new ObjectId();
        Document user5 = createUserDocument(
            user5Id,
            "owner.starter@example.com",
            "Owner Starter User",
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "+6281234567894",
            "Owner CV Usaha Starter",
            null, null, null, 0.0
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
            "+6281234567895",
            "CEO PT Perusahaan Pro",
            null, null, null, 50000.0
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
            "+6281234567896",
            "President Director PT Korporasi Enterprise",
            null, null, null, 0.0
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
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "+6281234567897",
            "HR Manager di PT Perusahaan Pro",
            user6Id, user6Id, null, 25000.0
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
            "ORGANIZATION",
            List.of("USER", "ORGANIZATION"),
            "+6281234567898",
            "Software Engineer di PT Perusahaan Pro",
            user8Id, user8Id, null, 0.0
        );
        user9.put("inviteCode", "INV-PRO-MEMBER");
        user9.put("invitationStatus", "accepted");
        user9.put("invitationRole", "member");
        user9.put("invitationSentAt", Instant.now());
        user9.put("invitationAcceptedAt", Instant.now());
        user9.put("organizationRole", "member");
        allUsers.add(user9);
        
        users.insertMany(allUsers);
        
        // === STEP 2: Update referralIds dan referredAt ===
        users.updateOne(new Document("_id", user1Id), new Document("$set", new Document("referralIds", List.of(user2Id))));
        users.updateOne(new Document("_id", user2Id), new Document("$set", new Document("referredAt", Instant.now())));
        
        users.updateOne(new Document("_id", user2Id), new Document("$set", new Document("referralIds", List.of(user3Id))));
        users.updateOne(new Document("_id", user3Id), new Document("$set", new Document("referredAt", Instant.now())));
        
        users.updateOne(new Document("_id", user6Id), new Document("$set", new Document("referralIds", List.of(user8Id))));
        users.updateOne(new Document("_id", user8Id), new Document("$set", new Document("referredAt", Instant.now())));
        
        users.updateOne(new Document("_id", user8Id), new Document("$set", new Document("referralIds", List.of(user9Id))));
        users.updateOne(new Document("_id", user9Id), new Document("$set", new Document("referredAt", Instant.now())));
        
        users.updateOne(new Document("_id", user1Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        users.updateOne(new Document("_id", user2Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        users.updateOne(new Document("_id", user6Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        users.updateOne(new Document("_id", user8Id), new Document("$set", new Document("totalReferrals", 1).append("successfulReferrals", 1)));
        
        // === STEP 3: Insert Organizations ===
        List<Document> allOrgs = new ArrayList<>();
        
        ObjectId org1Id = new ObjectId();
        Document org1 = createOrganizationDocument(
            org1Id, "PT Startup Trial", "Startup dalam masa trial 14 hari",
            "https://startup-trial.example.com", "https://example.com/logos/startup-trial.png",
            "Jl. Startup No. 1, Jakarta", "+622112345678", "contact@startup-trial.example.com",
            true, user4Id, 1
        );
        org1.put("trialStartsAt", Instant.now());
        org1.put("trialEndsAt", Instant.now().plusSeconds(14 * 24 * 60 * 60));
        allOrgs.add(org1);
        
        ObjectId org2Id = new ObjectId();
        Document org2 = createOrganizationDocument(
            org2Id, "CV Usaha Starter", "Organisasi dengan plan starter, max 15 member",
            "https://usaha-starter.example.com", "https://example.com/logos/usaha-starter.png",
            "Jl. Starter No. 5, Bandung", "+622298765432", "contact@usaha-starter.example.com",
            true, user5Id, 1
        );
        allOrgs.add(org2);
        
        ObjectId org3Id = new ObjectId();
        Document org3 = createOrganizationDocument(
            org3Id, "PT Perusahaan Pro", "Perusahaan profesional dengan 50 seats",
            "https://perusahaan-pro.example.com", "https://example.com/logos/perusahaan-pro.png",
            "Jl. Pro No. 50, Surabaya", "+623155566677", "contact@perusahaan-pro.example.com",
            true, user6Id, 3
        );
        allOrgs.add(org3);
        
        ObjectId org4Id = new ObjectId();
        Document org4 = createOrganizationDocument(
            org4Id, "PT Korporasi Enterprise", "Korporasi besar dengan unlimited seats dan SSO",
            "https://korporasi-enterprise.example.com", "https://example.com/logos/korporasi-enterprise.png",
            "Jl. Enterprise No. 999, Jakarta Selatan", "+622188899900", "contact@korporasi-enterprise.example.com",
            true, user7Id, 1
        );
        org4.put("approvedBy", "admin_001");
        org4.put("approvedAt", Instant.now());
        allOrgs.add(org4);
        
        organizations.insertMany(allOrgs);
        
        // === STEP 4: Update users dengan organizationId dan organizationName ===
        users.updateOne(new Document("_id", user4Id), new Document("$set", new Document("organizationId", org1Id).append("organizationName", "PT Startup Trial")));
        users.updateOne(new Document("_id", user5Id), new Document("$set", new Document("organizationId", org2Id).append("organizationName", "CV Usaha Starter")));
        users.updateOne(new Document("_id", user6Id), new Document("$set", new Document("organizationId", org3Id).append("organizationName", "PT Perusahaan Pro")));
        users.updateOne(new Document("_id", user7Id), new Document("$set", new Document("organizationId", org4Id).append("organizationName", "PT Korporasi Enterprise")));
        users.updateOne(new Document("_id", user8Id), new Document("$set", new Document("organizationId", org3Id).append("organizationName", "PT Perusahaan Pro").append("invitedOrganizationId", org3Id)));
        users.updateOne(new Document("_id", user9Id), new Document("$set", new Document("organizationId", org3Id).append("organizationName", "PT Perusahaan Pro").append("invitedOrganizationId", org3Id)));

        // === STEP 5: Insert Subscription Plans (Katalog Paket) ===
        List<Document> allPlans = new ArrayList<>();
        
        // Individual Plans
        ObjectId planIndBasic1M = new ObjectId();
        allPlans.add(createPlanDocument(planIndBasic1M, "Individual Basic (Monthly)", "IND_BASIC_1M", 49000.0, 30, "USER", null, false));
        
        ObjectId planIndBasic1Y = new ObjectId();
        allPlans.add(createPlanDocument(planIndBasic1Y, "Individual Basic (Yearly)", "IND_BASIC_1Y", 490000.0, 365, "USER", null, true));
        
        ObjectId planIndPrem1M = new ObjectId();
        allPlans.add(createPlanDocument(planIndPrem1M, "Individual Premium (Monthly)", "IND_PREM_1M", 149000.0, 30, "USER", null, false));
        
        ObjectId planIndPrem1Y = new ObjectId();
        allPlans.add(createPlanDocument(planIndPrem1Y, "Individual Premium (Yearly)", "IND_PREM_1Y", 1490000.0, 365, "USER", null, false));

        // Organization Plans
        ObjectId planOrgStart1M = new ObjectId();
        allPlans.add(createPlanDocument(planOrgStart1M, "Org Starter (Monthly)", "ORG_START_1M", 499000.0, 30, "ORGANIZATION", 15, false));
        
        ObjectId planOrgStart1Y = new ObjectId();
        allPlans.add(createPlanDocument(planOrgStart1Y, "Org Starter (Yearly)", "ORG_START_1Y", 4990000.0, 365, "ORGANIZATION", 15, false));
        
        ObjectId planOrgPro1M = new ObjectId();
        allPlans.add(createPlanDocument(planOrgPro1M, "Org Pro (Monthly)", "ORG_PRO_1M", 1299000.0, 30, "ORGANIZATION", 50, true));
        
        ObjectId planOrgPro1Y = new ObjectId();
        allPlans.add(createPlanDocument(planOrgPro1Y, "Org Pro (Yearly)", "ORG_PRO_1Y", 12990000.0, 365, "ORGANIZATION", 50, false));
        
        ObjectId planOrgEnt1M = new ObjectId();
        allPlans.add(createPlanDocument(planOrgEnt1M, "Org Enterprise (Monthly)", "ORG_ENT_1M", 4999000.0, 30, "ORGANIZATION", 9999, false));
        
        ObjectId planOrgEnt1Y = new ObjectId();
        allPlans.add(createPlanDocument(planOrgEnt1Y, "Org Enterprise (Yearly)", "ORG_ENT_1Y", 49990000.0, 365, "ORGANIZATION", 9999, false));

        subscriptionPlans.insertMany(allPlans);

        // === STEP 6: Insert Active Subscriptions (Transaksi) ===
        List<Document> activeSubscriptions = new ArrayList<>();

        // User 2 (Basic Individual) berlangganan 30 hari
        activeSubscriptions.add(createSubscriptionDocument(new ObjectId(), planIndBasic1M, "USER", user2Id, Instant.now(), Instant.now().plusSeconds(30 * 24 * 60 * 60), "ACTIVE", "TEST_TRX_001"));
        
        // User 3 (Premium Individual) berlangganan 365 hari
        activeSubscriptions.add(createSubscriptionDocument(new ObjectId(), planIndPrem1Y, "USER", user3Id, Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60), "ACTIVE", "TEST_TRX_002"));

        // Org 2 (Starter Organization) berlangganan 30 hari
        activeSubscriptions.add(createSubscriptionDocument(new ObjectId(), planOrgStart1M, "ORGANIZATION", org2Id, Instant.now(), Instant.now().plusSeconds(30 * 24 * 60 * 60), "ACTIVE", "TEST_TRX_003"));

        // Org 3 (Pro Organization) berlangganan 365 hari
        activeSubscriptions.add(createSubscriptionDocument(new ObjectId(), planOrgPro1Y, "ORGANIZATION", org3Id, Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60), "ACTIVE", "TEST_TRX_004"));

        // Org 4 (Enterprise Organization) berlangganan 30 hari
        activeSubscriptions.add(createSubscriptionDocument(new ObjectId(), planOrgEnt1M, "ORGANIZATION", org4Id, Instant.now(), Instant.now().plusSeconds(30 * 24 * 60 * 60), "ACTIVE", "TEST_TRX_005"));

        subscriptions.insertMany(activeSubscriptions);
    }

    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        String activeProfile = ConfigProvider.getConfig().getValue("quarkus.profile", String.class);
        if (!"dev".equals(activeProfile) && !"test".equals(activeProfile)) {
            return; 
        }
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        MongoCollection<Document> organizations = mongoDatabase.getCollection("organizations");
        MongoCollection<Document> subscriptionPlans = mongoDatabase.getCollection("subscription_plans");
        MongoCollection<Document> subscriptions = mongoDatabase.getCollection("subscriptions");
        
        users.deleteMany(new Document("email", new Document("$in", Arrays.asList(
            "individual.free@example.com",
            "individual.basic@example.com",
            "individual.premium@example.com",
            "owner.trial@example.com",
            "owner.starter@example.com",
            "owner.pro@example.com",
            "owner.enterprise@example.com",
            "admin.member@example.com",
            "regular.member@example.com"
        ))));
        
        organizations.deleteMany(new Document("name", new Document("$in", Arrays.asList(
            "PT Startup Trial",
            "CV Usaha Starter",
            "PT Perusahaan Pro",
            "PT Korporasi Enterprise"
        ))));

        subscriptionPlans.deleteMany(new Document());
        subscriptions.deleteMany(new Document());
    }

    private String getPasswordHash(String password) {
        return PasswordEncoder.hash(password);
    }

    private Document createUserDocument(ObjectId id, String email, String fullName, String accountType, List<String> roles, String phone, String bio, ObjectId referredBy, ObjectId invitedBy, ObjectId invitedOrganizationId, double referralEarnings) {        
        Document doc = new Document("_id", id)
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
            .append("revenueSharePercentage", 0)
            .append("referralCode", generateReferralCode(email))
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

        doc.entrySet().removeIf(entry -> entry.getValue() == null);
        return doc;
    }
    
    private Document createOrganizationDocument(ObjectId id, String name, String description, String website, String logo, String address, String phone, String email, Boolean status, ObjectId ownerId, Integer seatsUsed) {
        Document doc = new Document("_id", id)
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
            .append("seatsUsed", seatsUsed)
            .append("ownerId", ownerId)
            .append("createdAt", Instant.now())
            .append("updatedAt", Instant.now())
            .append("deletedAt", null);

        doc.entrySet().removeIf(entry -> entry.getValue() == null);
        return doc;
    }
    
    private String generateReferralCode(String email) {
        if (email == null || email.isEmpty()) return "REF" + System.currentTimeMillis();
        String alphaOnly = email.replaceAll("[^a-zA-Z]", "");
        String prefix = alphaOnly.substring(0, Math.min(3, alphaOnly.length())).toUpperCase();
        return prefix + Math.abs(email.hashCode() % 90000 + 10000);
    }

    private Document createPlanDocument(ObjectId id, String name, String code, Double price, Integer durationDays, String targetAudience, Integer maxSeats, Boolean recommended) {
        Document doc = new Document("_id", id)
            .append("name", name)
            .append("code", code)
            .append("price", price)
            .append("durationDays", durationDays)
            .append("targetAudience", targetAudience)
            .append("maxSeats", maxSeats)
            .append("recommended", recommended)
            .append("createdAt", Instant.now())
            .append("updatedAt", Instant.now());
        doc.entrySet().removeIf(entry -> entry.getValue() == null);
        return doc;
    }

    private Document createSubscriptionDocument(ObjectId id, ObjectId planId, String subscriberType, ObjectId subscriberId, Instant startDate, Instant endDate, String status, String paymentGatewayId) {
        Document doc = new Document("_id", id)
            .append("planId", planId)
            .append("subscriberType", subscriberType)
            .append("subscriberId", subscriberId)
            .append("startDate", startDate)
            .append("endDate", endDate)
            .append("status", status)
            .append("paymentGatewayId", paymentGatewayId)
            .append("createdAt", Instant.now())
            .append("updatedAt", Instant.now());
        doc.entrySet().removeIf(entry -> entry.getValue() == null);
        return doc;
    }
}