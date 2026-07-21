package com.psycorp.psychapi.infrastructure.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SuperAdminService {

    @Inject
    @ConfigProperty(name = "superadmin.emails", defaultValue = "admin@psychapi.com")
    String superAdminEmails;

    public boolean isSuperAdmin(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        List<String> emails = getSuperAdminEmails();
        if (emails.isEmpty()) {
            return false;
        }

        return emails.stream()
            .anyMatch(superEmail -> superEmail.equalsIgnoreCase(email.trim()));
    }

    public List<String> getSuperAdminEmails() {
        if (superAdminEmails == null || superAdminEmails.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(superAdminEmails.split(","))
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .toList();
    }

    public int getSuperAdminCount() {
        return getSuperAdminEmails().size();
    }
}
