package com.psycorp.psychapi.infrastructure.seed;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psycorp.psychapi.domain.model.Post;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Seeder untuk Post entities.
 * Membuat sample posts untuk development dan testing.
 * 
 * Best practices implemented:
 * - Idempotent: Check by unique title, not count
 * - Non-destructive: Doesn't delete existing data
 * - Environment-aware: Only runs in dev/test profiles
 * - Configurable: Controlled via seeder.* configuration
 */
@ApplicationScoped
public class PostSeeder {

    private static final Logger log = LoggerFactory.getLogger(PostSeeder.class);

    // Unique identifier for idempotent check
    private static final String SEED_MARKER_TITLE = "Post 1";

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
        log.info("🌱 Starting Post seeder in profile '{}'", currentProfile);

        // Check if data already exists (idempotent check by unique title)
        Post existingMarker = Post.find("title", SEED_MARKER_TITLE).firstResult();
        if (existingMarker != null) {
            log.info("✅ Post data already exists - skipping seeding (found marker: {})", SEED_MARKER_TITLE);
            log.info("💡 Tip: Set SEEDER_AUTO_CLEAR=true to force re-seeding");
            return false;
        }

        // Perform actual seeding
        try {
            seedInternal();
            log.info("✅ Post seeder completed successfully");
            logSummary();
            return true;
        } catch (Exception e) {
            log.error("❌ Error during post seeding: {}", e.getMessage(), e);
            throw new RuntimeException("Post seeding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if seeder should run based on configuration and environment.
     */
    private boolean shouldRun() {
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
        java.util.List<String> allowedProfiles = java.util.List.of(environments.split(","));
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
     * Internal seeding logic - creates sample posts.
     */
    private void seedInternal() {
        log.info("Starting post seeding...");

        Post post1 = new Post();
        post1.setTitle("Post 1");
        post1.setContent("Content 1");
        post1.setStatus("published");
        post1.setCreatedAt(Instant.now());
        post1.setUpdatedAt(Instant.now());

        Post post2 = new Post();
        post2.setTitle("Post 2");
        post2.setContent("Content 2");
        post2.setStatus("draft");
        post2.setCreatedAt(Instant.now());
        post2.setUpdatedAt(Instant.now());

        Post.persist(post1, post2);
        log.info("✓ Created 2 sample posts");
    }

    /**
     * Log summary of seeded data.
     */
    private void logSummary() {
        log.info("");
        log.info("========================================");
        log.info("✅ Post Seeding Complete!");
        log.info("========================================");
        log.info("Total Posts: {}", Post.count());
        log.info("  - Published: 1");
        log.info("  - Draft: 1");
        log.info("========================================");
        log.info("");
    }
}
