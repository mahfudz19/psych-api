package com.psycorp.psychapi.infrastructure.seed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 
 * Best practices implemented:
 * - Idempotent: Check by unique email, not count
 * - Non-destructive: Upsert pattern, doesn't delete existing data
 * - Environment-aware: Only runs in dev/test profiles
 * - Configurable: Controlled via seeder.* configuration
 */
@ApplicationScoped
public class UserSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    // Unique identifier for idempotent check
    private static final String SEED_MARKER_EMAIL = "individual.free@example.com";

    public void init(@Observes StartupEvent event) {
        seed();
    }

    /**
     * Main seeding method with idempotent logic.
     *
     * @return true if seeding was performed, false if skipped
     */
    public boolean seed() {
        // Check if seeder should run (environment, configuration)
        if (!shouldRun()) {
            return false;
        }

        String currentProfile = getCurrentProfile();
        log.info("🌱 Starting User & Organization seeder in profile '{}'", currentProfile);

        // Check if data already exists (idempotent check by unique email)
        User existingMarker = User.find("email", SEED_MARKER_EMAIL).firstResult();
        if (existingMarker != null) {
            log.info("✅ User data already exists - skipping seeding (found marker: {})", SEED_MARKER_EMAIL);
            log.info("💡 Tip: Set SEEDER_AUTO_CLEAR=true to force re-seeding");
            return false;
        }

        // Perform actual seeding
        try {
            seedInternal();
            log.info("✅ User & Organization seeder completed successfully");
            logSummary();
            return true;
        } catch (Exception e) {
            log.error("❌ Error during user seeding: {}", e.getMessage(), e);
            throw new RuntimeException("User seeding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if seeder should run based on configuration and environment.
     */
    private boolean shouldRun() {
        // Inject config manually since we're not extending BaseSeeder
        // This is a simplified version - in production, use CDI properly
        String enabled = System.getenv("SEEDER_ENABLED");
        if ("false".equalsIgnoreCase(enabled)) {
            log.info("🚫 Seeder is disabled via SEEDER_ENABLED=false");
            return false;
        }

        String environments = System.getenv("SEEDER_ENVIRONMENTS");
        if (environments == null || environments.isBlank()) {
            environments = "dev,test";
        }

        String currentProfile = getCurrentProfile();
        List<String> allowedProfiles = List.of(environments.split(","));
        boolean isAllowed = allowedProfiles.stream()
            .map(String::trim)
            .anyMatch(p -> p.equalsIgnoreCase(currentProfile));

        if (!isAllowed) {
            log.info("🚫 Seeder skipped - not allowed in profile '{}'. Allowed: {}",
                    currentProfile, environments);
            return false;
        }

        return true;
    }

    /**
     * Get current profile for logging.
     */
    private String getCurrentProfile() {
        String profile = System.getProperty("quarkus.profile");
        if (profile == null || profile.isBlank()) {
            profile = System.getenv("QUARKUS_PROFILE");
        }
        return profile != null && !profile.isBlank() ? profile : "dev";
    }

    /**
     * Internal seeding logic - creates all seed data.
     */
    private void seedInternal() {
        log.info("Starting user & organization seeding...");

        // ==========================================
        // INDIVIDUAL USERS (No Organization)
        // ==========================================

        // 1. Individual Free User (NO REFERRAL - First user)
        User individualFree = User.create(
            "individual.free@example.com",
            PasswordEncoder.hash("password123"),
            "Individual Free User",
            null,  // referrer
            null,  // inviter
            AccountType.INDIVIDUAL
        );
        individualFree.setPhone("+6281234567890");
        individualFree.setBio("Individual free user, belum berlangganan");
        individualFree.setRoles(List.of("USER"));
        individualFree.setSubscriptionTier("free");
        individualFree.persist();
        log.info("✓ Created: Individual Free User (referralCode: {})", individualFree.getReferralCode());

        // 2. Individual Premium User (REFERRAL: individualFree)
        User individualPremium = User.create(
            "individual.premium@example.com",
            PasswordEncoder.hash("password123"),
            "Individual Premium User",
            individualFree,  // referrer
            null,            // inviter
            AccountType.INDIVIDUAL
        );
        individualPremium.setPhone("+6281234567891");
        individualPremium.setBio("Individual premium user, berlangganan pribadi");
        individualPremium.setRoles(List.of("USER"));
        individualPremium.setSubscriptionTier("premium");
        individualPremium.setSubscriptionExpiry(Instant.now().plus(30, ChronoUnit.DAYS));
        individualPremium.persist();  // Persist first to get ID
        
        // Set referral after persist (to get individualPremium.id)
        individualPremium.setReferredBy(individualFree.id);
        individualPremium.setReferredAt(Instant.now());
        individualPremium.update();
        
        // Update individualFree referral stats
        individualFree.setReferralIds(new ArrayList<>(List.of(individualPremium.id)));
        individualFree.setTotalReferrals(1);
        individualFree.update();
        
        log.info("✓ Created: Individual Premium User (referralCode: {}, referredBy: individualFree)", 
                individualPremium.getReferralCode());

        // 3. Individual Enterprise User (REFERRAL: individualPremium)
        User individualEnterprise = User.create(
            "individual.enterprise@example.com",
            PasswordEncoder.hash("password123"),
            "Individual Enterprise User",
            individualPremium,  // referrer
            null,               // inviter
            AccountType.INDIVIDUAL
        );
        individualEnterprise.setPhone("+6281234567892");
        individualEnterprise.setBio("Individual enterprise user dengan fitur lengkap");
        individualEnterprise.setRoles(List.of("USER"));
        individualEnterprise.setSubscriptionTier("enterprise");
        individualEnterprise.setSubscriptionExpiry(Instant.now().plus(365, ChronoUnit.DAYS));
        individualEnterprise.persist();  // Persist first to get ID
        
        // Set referral after persist (to get individualEnterprise.id)
        individualEnterprise.setReferredBy(individualPremium.id);
        individualEnterprise.setReferredAt(Instant.now());
        individualEnterprise.update();
        
        // Update individualPremium referral stats
        individualPremium.setReferralIds(new ArrayList<>(List.of(individualEnterprise.id)));
        individualPremium.setTotalReferrals(1);
        individualPremium.update();
        
        log.info("✓ Created: Individual Enterprise User (referralCode: {}, referredBy: individualPremium)", 
                individualEnterprise.getReferralCode());

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
            null,  // referrer
            null,  // inviter
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
        log.info("✓ Created: Organization Owner (Free Trial) + Organization");

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
            null,  // referrer
            null,  // inviter
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
        log.info("✓ Created: Organization Owner (Free Plan) + Organization");

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
            null,  // referrer
            null,  // inviter
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
        log.info("✓ Created: Organization Owner (Pro Plan) + Organization");

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
            null,  // referrer
            null,  // inviter
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
        log.info("✓ Created: Organization Owner (Enterprise) + Organization");

        // ==========================================
        // ORGANIZATION MEMBERS (dengan referral tracking)
        // ==========================================

        // 8. Organization Admin (REFERRAL: orgOwnerPro, INVITATION: orgPro)
        User orgAdmin = User.create(
            "admin.member@example.com",
            PasswordEncoder.hash("password123"),
            "Organization Admin User",
            orgOwnerPro,  // referrer
            null,         // inviter (will set invitation manually)
            AccountType.INDIVIDUAL
        );
        orgAdmin.setReferredBy(orgOwnerPro.id);
        orgAdmin.setReferredAt(Instant.now());
        orgAdmin.setInviteCode("INV-PRO-ADMIN");  // Invitation code for organization
        orgAdmin.setInvitedBy(orgOwnerPro.id);
        orgAdmin.setInvitedOrganizationId(orgPro.id);
        orgAdmin.setInvitationStatus("accepted");
        orgAdmin.setInvitationSentAt(Instant.now());
        orgAdmin.setInvitationAcceptedAt(Instant.now());
        orgAdmin.setInvitationRole("admin");
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
        
        // Update referralIds list di owner pro
        if (orgOwnerPro.getReferralIds() == null) {
            orgOwnerPro.setReferralIds(new ArrayList<>(List.of(orgAdmin.id)));
        } else {
            orgOwnerPro.getReferralIds().add(orgAdmin.id);
        }
        orgOwnerPro.setTotalReferrals(orgOwnerPro.getTotalReferrals() + 1);
        orgOwnerPro.update();
        log.info("✓ Created: Organization Admin (referralCode: {}, referredBy: orgOwnerPro, inviteCode: {})", 
                orgAdmin.getReferralCode(), orgAdmin.getInviteCode());

        // 9. Organization Member (REFERRAL: orgAdmin, INVITATION: orgPro)
        User orgMember = User.create(
            "regular.member@example.com",
            PasswordEncoder.hash("password123"),
            "Organization Member User",
            orgAdmin,     // referrer
            orgAdmin,     // inviter
            AccountType.INDIVIDUAL
        );
        orgMember.setReferredBy(orgAdmin.id);
        orgMember.setReferredAt(Instant.now());
        orgMember.setInviteCode("INV-PRO-MEMBER");  // Invitation code for organization
        orgMember.setInvitedBy(orgOwnerPro.id);
        orgMember.setInvitedOrganizationId(orgPro.id);
        orgMember.setInvitationStatus("accepted");
        orgMember.setInvitationSentAt(Instant.now());
        orgMember.setInvitationAcceptedAt(Instant.now());
        orgMember.setInvitationRole("member");
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
        
        // Update referralIds list di admin
        if (orgAdmin.getReferralIds() == null) {
            orgAdmin.setReferralIds(new ArrayList<>(List.of(orgMember.id)));
        } else {
            orgAdmin.getReferralIds().add(orgMember.id);
        }
        orgAdmin.setTotalReferrals(orgAdmin.getTotalReferrals() + 1);
        orgAdmin.update();
        log.info("✓ Created: Organization Member (referralCode: {}, referredBy: orgAdmin, inviteCode: {})", 
                orgMember.getReferralCode(), orgMember.getInviteCode());

        // ==========================================
        // SUPERADMIN USER (Platform Administrator)
        // ==========================================
        // Note: This user will have superadmin access via SUPERADMIN_EMAILS environment variable.
        // The email "admin@psycorp.com" is included in SUPERADMIN_EMAILS by default.
        // No need to set ADMIN role - superadmin access is granted via JWT claim isSuperAdmin.

        User adminUser = User.create(
            "admin@psycorp.com",
            PasswordEncoder.hash("admin123"),
            "Platform Administrator",
            null,  // referrer
            null,  // inviter
            AccountType.INDIVIDUAL
        );
        adminUser.setPhone("+6281234567899");
        adminUser.setBio("Super Admin PsychCorp Platform");
        adminUser.setRoles(List.of("USER"));  // Only USER role - superadmin access via env variable
        adminUser.setSubscriptionTier("enterprise");
        adminUser.setStatus("active");
        adminUser.persist();
        log.info("✓ Created: Platform Administrator (referralCode: {}, superadmin via SUPERADMIN_EMAILS)", 
                adminUser.getReferralCode());
    }

    /**
     * Log summary of seeded data.
     */
    private void logSummary() {
        log.info("");
        log.info("========================================");
        log.info("✅ User & Organization Seeding Complete!");
        log.info("========================================");
        log.info("Total Users: {}", User.count());
        log.info("Total Organizations: {}", Organization.count());
        log.info("");
        log.info("📊 User Breakdown:");
        log.info("  - Individual Free: 1");
        log.info("  - Individual Premium: 1");
        log.info("  - Individual Enterprise: 1");
        log.info("  - Organization Owners: 4 (Trial, Free, Pro, Enterprise)");
        log.info("  - Organization Admin: 1");
        log.info("  - Organization Member: 1");
        log.info("  - Platform Admin: 1");
        log.info("");
        log.info("🏢 Organizations:");
        log.info("  - PT Startup Trial (free_trial)");
        log.info("  - CV Usaha Gratis (free)");
        log.info("  - PT Perusahaan Pro (pro)");
        log.info("  - PT Korporasi Enterprise (enterprise)");
        log.info("");
        log.info("🔗 Referral Chain:");
        log.info("  individualFree → individualPremium → individualEnterprise");
        log.info("  orgOwnerPro → orgAdmin → orgMember");
        log.info("========================================");
        log.info("");
    }
}
