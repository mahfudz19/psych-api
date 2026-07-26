package com.psycorp.psychapi.infrastructure.seed;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

/**
 * Abstract base class for all seeders.
 * 
 * Provides common functionality for:
 * - Environment-based execution control
 * - Configuration-driven seeding
 * - Consistent logging
 * - Idempotent seeding pattern
 * 
 * @param <T> the entity type this seeder handles
 */
public abstract class BaseSeeder<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseSeeder.class);

    @Inject
    SeederConfig seederConfig;

    @ConfigProperty(name = "quarkus.profile")
    String currentProfile;

    /**
     * Get the logger for this seeder.
     * 
     * @return the logger instance
     */
    protected Logger getLog() {
        return log;
    }

    /**
     * Check if seeder should run based on configuration and environment.
     * 
     * @return true if seeder should run, false otherwise
     */
    protected boolean shouldRun() {
        // Check if seeder is enabled
        if (!seederConfig.enabled()) {
            log.info("🚫 Seeder is disabled via configuration");
            return false;
        }

        // Check if current profile is allowed
        if (!seederConfig.isAllowedProfile(currentProfile)) {
            log.info("🚫 Seeder skipped - not allowed in profile '{}'. Allowed profiles: {}", 
                    currentProfile, seederConfig.environments());
            return false;
        }

        return true;
    }

    /**
     * Check if auto-clear is enabled.
     * 
     * @return true if auto-clear is enabled
     */
    protected boolean shouldAutoClear() {
        return seederConfig.autoClear();
    }

    /**
     * Get the current Quarkus profile.
     * 
     * @return the current profile name
     */
    protected String getCurrentProfile() {
        return currentProfile;
    }

    /**
     * Template method for seeding with common pre-checks.
     * Subclasses should implement seedInternal() for actual seeding logic.
     * 
     * @return true if seeding was performed, false if skipped
     */
    public boolean seed() {
        if (!shouldRun()) {
            return false;
        }

        log.info("🌱 Starting {} seeder in profile '{}'", getSeederName(), currentProfile);

        // Handle auto-clear if enabled
        if (shouldAutoClear()) {
            log.info("🗑️  Auto-clear enabled - removing existing data before seeding");
            clearData();
        }

        // Check if data already exists (idempotent check)
        if (dataExists()) {
            if (!shouldAutoClear()) {
                log.info("✅ Data already exists - skipping seeding (use seeder.auto-clear=true to force)");
                return false;
            }
        }

        // Perform actual seeding
        try {
            seedInternal();
            log.info("✅ {} seeder completed successfully", getSeederName());
            return true;
        } catch (Exception e) {
            log.error("❌ Error during seeding: {}", e.getMessage(), e);
            throw new RuntimeException("Seeding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get the name of this seeder for logging purposes.
     * 
     * @return the seeder name
     */
    protected abstract String getSeederName();

    /**
     * Check if data already exists.
     * This should check for a unique identifier, not just count.
     * 
     * @return true if data exists, false otherwise
     */
    protected abstract boolean dataExists();

    /**
     * Clear existing data. Only called when auto-clear is enabled.
     * 
     * @return true if data was cleared, false if nothing to clear
     */
    protected abstract boolean clearData();

    /**
     * Perform the actual seeding logic.
     * This method should be idempotent - safe to run multiple times.
     * 
     * @throws Exception if seeding fails
     */
    protected abstract void seedInternal() throws Exception;
}
