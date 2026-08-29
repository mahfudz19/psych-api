package com.psycorp.psychapi.infrastructure.database.migration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class MigrationLifecycle {
    private static final Logger LOG = Logger.getLogger(MigrationLifecycle.class);

    @ConfigProperty(name = "app.migration.exit-after", defaultValue = "false")
    boolean exitAfter;

    public void onStart(@Observes StartupEvent ev) {
        if (exitAfter) {
            LOG.info("✅ Database Migration & Seeding selesai. Mematikan instance secara otomatis...");
            Quarkus.asyncExit(0);
        }
    }
}