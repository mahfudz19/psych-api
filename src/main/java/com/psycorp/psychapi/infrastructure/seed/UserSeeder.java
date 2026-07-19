package com.psycorp.psychapi.infrastructure.seed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.psycorp.psychapi.domain.model.Organization;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.model.User.AccountType;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Seeder untuk User dan Organization.
 * Membuat data sample berdasarkan kombinasi user yang mungkin terjadi:
 * 
 * 1. Individual Free User
 * 2. Individual Premium User
 * 3. Individual Enterprise User
 * 4. Organization Owner (Free Trial)
 * 5. Organization Owner (Free Plan)
 * 6. Organization Owner (Pro Plan)
 * 7. Organization Owner (Enterprise)
 * 8. Organization Admin
 * 9. Organization Member
 */
@ApplicationScoped
public class UserSeeder {

    public void init(@Observes StartupEvent event) {
        long userCount = User.count();
        long orgCount = Organization.count();
        
        if (userCount > 0 || orgCount > 0) {
            System.out.println("Seed already exists. Skipping user & organization seeding.");
            return;
        }

        System.out.println("Starting user & organization seeding...");

        // ==========================================
        // INDIVIDUAL USERS (No Organization)
        // ==========================================

        // 1. Individual Free User
        User individualFree = User.create(
            "individual.free@example.com",
            PasswordEncoder.hash("password123"),
            "Individual Free User",
            null,
            AccountType.INDIVIDUAL
        );
        individualFree.setPhone("+6281234567890");
        individualFree.setBio("Individual free user, belum berlangganan");
        individualFree.setRoles(List.of("USER"));
        individualFree.setSubscriptionTier("free");
        individualFree.persist();
        System.out.println("✓ Created: Individual Free User");

        // 2. Individual Premium User
        User individualPremium = User.create(
            "individual.premium@example.com",
            PasswordEncoder.hash("password123"),
            "Individual Premium User",
            null,
            AccountType.INDIVIDUAL
        );
        individualPremium.setPhone("+6281234567891");
        individualPremium.setBio("Individual premium user, berlangganan pribadi");
        individualPremium.setRoles(List.of("USER"));
        individualPremium.setSubscriptionTier("premium");
        individualPremium.setSubscriptionExpiry(Instant.now().plus(30, ChronoUnit.DAYS));
        individualPremium.persist();
        System.out.println("✓ Created: Individual Premium User");

        // 3. Individual Enterprise User
        User individualEnterprise = User.create(
            "individual.enterprise@example.com",
            PasswordEncoder.hash("password123"),
            "Individual Enterprise User",
            null,
            AccountType.INDIVIDUAL
        );
        individualEnterprise.setPhone("+6281234567892");
        individualEnterprise.setBio("Individual enterprise user dengan fitur lengkap");
        individualEnterprise.setRoles(List.of("USER"));
        individualEnterprise.setSubscriptionTier("enterprise");
        individualEnterprise.setSubscriptionExpiry(Instant.now().plus(365, ChronoUnit.DAYS));
        individualEnterprise.persist();
        System.out.println("✓ Created: Individual Enterprise User");

        // ==========================================
        // ORGANIZATION OWNERS
        // ==========================================

        // 4. Organization Owner (Free Trial) - Trial 14 hari
        Organization orgTrial = new Organization();
        orgTrial.setName("PT Startup Trial");
        orgTrial.setDescription("Startup dalam masa trial 14 hari");
        orgTrial.setWebsite("https://startup-trial.example.com");
        orgTrial.setLogo("https://example.com/logos/startup-trial.png");
        orgTrial.setAddress("Jl. Startup No. 1, Jakarta");
        orgTrial.setPhone("+622112345678");
        orgTrial.setEmail("contact@startup-trial.example.com");
        orgTrial.setStatus(true);
        orgTrial.setPlan("free_trial");
        orgTrial.setTrialStartsAt(Instant.now());
        orgTrial.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        orgTrial.setSeats(999); // Unlimited selama trial
        orgTrial.setSeatsUsed(0);
        orgTrial.setCreatedAt(Instant.now());
        orgTrial.setUpdatedAt(Instant.now());
        orgTrial.persist();

        User orgOwnerTrial = User.create(
            "owner.trial@example.com",
            PasswordEncoder.hash("password123"),
            "Owner Trial User",
            null,
            AccountType.ORGANIZATION
        );
        orgOwnerTrial.setPhone("+6281234567893");
        orgOwnerTrial.setBio("Founder & CEO PT Startup Trial");
        orgOwnerTrial.setRoles(List.of("USER", "ORGANIZATION"));
        orgOwnerTrial.setOrganizationId(orgTrial.id);
        orgOwnerTrial.setOrganizationRole("owner");
        orgOwnerTrial.setOrganizationName("PT Startup Trial");
        orgOwnerTrial.setSubscriptionTier("free");
        orgTrial.setOwnerId(orgOwnerTrial.id);
        orgTrial.setSeatsUsed(1);
        orgTrial.update();
        orgOwnerTrial.persist();
        System.out.println("✓ Created: Organization Owner (Free Trial) + Organization");

        // 5. Organization Owner (Free Plan) - Setelah trial berakhir
        Organization orgFree = new Organization();
        orgFree.setName("CV Usaha Gratis");
        orgFree.setDescription("Organisasi dengan plan gratis, max 5 member");
        orgFree.setWebsite("https://usaha-gratis.example.com");
        orgFree.setLogo("https://example.com/logos/usaha-gratis.png");
        orgFree.setAddress("Jl. Gratis No. 5, Bandung");
        orgFree.setPhone("+622298765432");
        orgFree.setEmail("contact@usaha-gratis.example.com");
        orgFree.setStatus(true);
        orgFree.setPlan("free");
        orgFree.setSeats(5);
        orgFree.setSeatsUsed(1);
        orgFree.setCreatedAt(Instant.now());
        orgFree.setUpdatedAt(Instant.now());
        orgFree.persist();

        User orgOwnerFree = User.create(
            "owner.free@example.com",
            PasswordEncoder.hash("password123"),
            "Owner Free User",
            null,
            AccountType.ORGANIZATION
        );
        orgOwnerFree.setPhone("+6281234567894");
        orgOwnerFree.setBio("Owner CV Usaha Gratis");
        orgOwnerFree.setRoles(List.of("USER", "ORGANIZATION"));
        orgOwnerFree.setOrganizationId(orgFree.id);
        orgOwnerFree.setOrganizationRole("owner");
        orgOwnerFree.setOrganizationName("CV Usaha Gratis");
        orgOwnerFree.setSubscriptionTier("free");
        orgFree.setOwnerId(orgOwnerFree.id);
        orgFree.update();
        orgOwnerFree.persist();
        System.out.println("✓ Created: Organization Owner (Free Plan) + Organization");

        // 6. Organization Owner (Pro Plan) - Berlangganan
        Organization orgPro = new Organization();
        orgPro.setName("PT Perusahaan Pro");
        orgPro.setDescription("Perusahaan profesional dengan 50 seats");
        orgPro.setWebsite("https://perusahaan-pro.example.com");
        orgPro.setLogo("https://example.com/logos/perusahaan-pro.png");
        orgPro.setAddress("Jl. Pro No. 50, Surabaya");
        orgPro.setPhone("+623155566677");
        orgPro.setEmail("contact@perusahaan-pro.example.com");
        orgPro.setStatus(true);
        orgPro.setPlan("pro");
        orgPro.setSeats(50);
        orgPro.setSeatsUsed(1);
        orgPro.setSubscriptionExpiry(Instant.now().plus(30, ChronoUnit.DAYS));
        orgPro.setCreatedAt(Instant.now());
        orgPro.setUpdatedAt(Instant.now());
        orgPro.persist();

        User orgOwnerPro = User.create(
            "owner.pro@example.com",
            PasswordEncoder.hash("password123"),
            "Owner Pro User",
            null,
            AccountType.ORGANIZATION
        );
        orgOwnerPro.setPhone("+6281234567895");
        orgOwnerPro.setBio("CEO PT Perusahaan Pro");
        orgOwnerPro.setRoles(List.of("USER", "ORGANIZATION"));
        orgOwnerPro.setOrganizationId(orgPro.id);
        orgOwnerPro.setOrganizationRole("owner");
        orgOwnerPro.setOrganizationName("PT Perusahaan Pro");
        orgOwnerPro.setSubscriptionTier("premium");
        orgOwnerPro.setRevenueSharePercentage(15); // Pro dapat 15% revenue share
        orgPro.setOwnerId(orgOwnerPro.id);
        orgPro.update();
        orgOwnerPro.persist();
        System.out.println("✓ Created: Organization Owner (Pro Plan) + Organization");

        // 7. Organization Owner (Enterprise) - Custom plan
        Organization orgEnterprise = new Organization();
        orgEnterprise.setName("PT Korporasi Enterprise");
        orgEnterprise.setDescription("Korporasi besar dengan unlimited seats dan SSO");
        orgEnterprise.setWebsite("https://korporasi-enterprise.example.com");
        orgEnterprise.setLogo("https://example.com/logos/korporasi-enterprise.png");
        orgEnterprise.setAddress("Jl. Enterprise No. 999, Jakarta Selatan");
        orgEnterprise.setPhone("+622188899900");
        orgEnterprise.setEmail("contact@korporasi-enterprise.example.com");
        orgEnterprise.setStatus(true);
        orgEnterprise.setPlan("enterprise");
        orgEnterprise.setSeats(9999); // Unlimited
        orgEnterprise.setSeatsUsed(1);
        orgEnterprise.setSubscriptionExpiry(Instant.now().plus(365, ChronoUnit.DAYS));
        orgEnterprise.setApprovedBy("admin_001");
        orgEnterprise.setApprovedAt(Instant.now());
        orgEnterprise.setCreatedAt(Instant.now());
        orgEnterprise.setUpdatedAt(Instant.now());
        orgEnterprise.persist();

        User orgOwnerEnterprise = User.create(
            "owner.enterprise@example.com",
            PasswordEncoder.hash("password123"),
            "Owner Enterprise User",
            null,
            AccountType.ORGANIZATION
        );
        orgOwnerEnterprise.setPhone("+6281234567896");
        orgOwnerEnterprise.setBio("President Director PT Korporasi Enterprise");
        orgOwnerEnterprise.setRoles(List.of("USER", "ORGANIZATION"));
        orgOwnerEnterprise.setOrganizationId(orgEnterprise.id);
        orgOwnerEnterprise.setOrganizationRole("owner");
        orgOwnerEnterprise.setOrganizationName("PT Korporasi Enterprise");
        orgOwnerEnterprise.setSubscriptionTier("enterprise");
        orgOwnerEnterprise.setRevenueSharePercentage(20); // Enterprise dapat 20% revenue share
        orgEnterprise.setOwnerId(orgOwnerEnterprise.id);
        orgEnterprise.update();
        orgOwnerEnterprise.persist();
        System.out.println("✓ Created: Organization Owner (Enterprise) + Organization");

        // ==========================================
        // ORGANIZATION MEMBERS (dengan referral tracking)
        // ==========================================

        // 8. Organization Admin (dijoin oleh owner pro)
        User orgAdmin = User.create(
            "admin.member@example.com",
            PasswordEncoder.hash("password123"),
            "Organization Admin User",
            orgOwnerPro.id.toHexString(), // Direfer oleh owner pro
            AccountType.INDIVIDUAL // Admin adalah individual yang join organization
        );
        orgAdmin.setPhone("+6281234567897");
        orgAdmin.setBio("HR Manager di PT Perusahaan Pro");
        orgAdmin.setRoles(List.of("USER"));
        orgAdmin.setOrganizationId(orgPro.id);
        orgAdmin.setOrganizationRole("admin");
        orgAdmin.setOrganizationName("PT Perusahaan Pro");
        orgAdmin.setSubscriptionTier("free"); // Member ikut subscription org
        orgAdmin.persist();
        
        // Update seats used di organization pro
        orgPro.setSeatsUsed(orgPro.getSeatsUsed() + 1);
        orgPro.update();
        
        // Update referrals list di owner pro
        if (orgOwnerPro.getReferrals() == null) {
            orgOwnerPro.setReferrals(List.of(orgAdmin.id.toHexString()));
        } else {
            orgOwnerPro.getReferrals().add(orgAdmin.id.toHexString());
        }
        orgOwnerPro.update();
        System.out.println("✓ Created: Organization Admin");

        // 9. Organization Member (regular employee)
        User orgMember = User.create(
            "regular.member@example.com",
            PasswordEncoder.hash("password123"),
            "Organization Member User",
            orgAdmin.id.toHexString(), // Direfer oleh admin
            AccountType.INDIVIDUAL // Member adalah individual yang join organization
        );
        orgMember.setPhone("+6281234567898");
        orgMember.setBio("Software Engineer di PT Perusahaan Pro");
        orgMember.setRoles(List.of("USER"));
        orgMember.setOrganizationId(orgPro.id);
        orgMember.setOrganizationRole("member");
        orgMember.setOrganizationName("PT Perusahaan Pro");
        orgMember.setSubscriptionTier("free");
        orgMember.persist();
        
        // Update seats used di organization pro
        orgPro.setSeatsUsed(orgPro.getSeatsUsed() + 1);
        orgPro.update();
        
        // Update referrals list di admin
        if (orgAdmin.getReferrals() == null) {
            orgAdmin.setReferrals(List.of(orgMember.id.toHexString()));
        } else {
            orgAdmin.getReferrals().add(orgMember.id.toHexString());
        }
        orgAdmin.update();
        System.out.println("✓ Created: Organization Member");

        // ==========================================
        // ADMIN USER (Platform Administrator)
        // ==========================================

        User adminUser = User.create(
            "admin@psycorp.com",
            PasswordEncoder.hash("admin123"),
            "Platform Administrator",
            null,
            AccountType.INDIVIDUAL // Admin adalah individual dengan role ADMIN
        );
        adminUser.setPhone("+6281234567899");
        adminUser.setBio("Super Admin PsychCorp Platform");
        adminUser.setRoles(List.of("USER", "ADMIN"));
        adminUser.setSubscriptionTier("enterprise");
        adminUser.setStatus("active");
        adminUser.persist();
        System.out.println("✓ Created: Platform Administrator");

        // ==========================================
        // SUMMARY
        // ==========================================

        System.out.println("\n========================================");
        System.out.println("✅ User & Organization Seeding Complete!");
        System.out.println("========================================");
        System.out.println("Total Users: " + User.count());
        System.out.println("Total Organizations: " + Organization.count());
        System.out.println("\n📊 User Breakdown:");
        System.out.println("  - Individual Free: 1");
        System.out.println("  - Individual Premium: 1");
        System.out.println("  - Individual Enterprise: 1");
        System.out.println("  - Organization Owners: 4 (Trial, Free, Pro, Enterprise)");
        System.out.println("  - Organization Admin: 1");
        System.out.println("  - Organization Member: 1");
        System.out.println("  - Platform Admin: 1");
        System.out.println("\n🏢 Organizations:");
        System.out.println("  - PT Startup Trial (free_trial)");
        System.out.println("  - CV Usaha Gratis (free)");
        System.out.println("  - PT Perusahaan Pro (pro)");
        System.out.println("  - PT Korporasi Enterprise (enterprise)");
        System.out.println("========================================\n");
    }
}
