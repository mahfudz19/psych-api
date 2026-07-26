package com.psycorp.psychapi.infrastructure.seed;

import java.util.Arrays;
import java.util.List;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration class for seeder settings.
 * 
 * Provides centralized configuration for:
 * - Enable/disable seeding
 * - Auto-clear existing data
 * - Allowed environments for seeding
 */
@ConfigMapping(prefix = "seeder")
public interface SeederConfig {

    /**
     * Check if seeder is enabled.
     * 
     * @return true if seeding is enabled, false otherwise
     */
    @WithName("enabled")
    @WithDefault("true")
    boolean enabled();

    /**
     * Check if auto-clear is enabled.
     * When enabled, existing data will be cleared before seeding.
     * 
     * @return true if auto-clear is enabled, false otherwise
     */
    @WithName("auto-clear")
    @WithDefault("false")
    boolean autoClear();

    /**
     * Get list of allowed environments for seeding.
     * Seeder will only run in these environments.
     * 
     * @return comma-separated list of allowed environments
     */
    @WithName("environments")
    @WithDefault("dev,test")
    String environments();

    /**
     * Check if current profile is allowed for seeding.
     * 
     * @param currentProfile the current Quarkus profile
     * @return true if current profile is in allowed list
     */
    default boolean isAllowedProfile(String currentProfile) {
        if (currentProfile == null || currentProfile.isBlank()) {
            return false;
        }
        List<String> allowedProfiles = Arrays.asList(environments().split(","));
        return allowedProfiles.stream()
            .map(String::trim)
            .anyMatch(profile -> profile.equalsIgnoreCase(currentProfile));
    }
}
