package com.psycorp.psychapi.feature.referral.service;

import java.security.SecureRandom;
import java.time.Instant;

import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service untuk mengelola referral code system.
 * Menangani generate, validate, regenerate codes dengan embedded archive.
 * 
 * @author Architect
 */
@ApplicationScoped
public class ReferralService {
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Inject
    RateLimitService rateLimitService;

    public String generateReferralCode(User user) {
        String userId = user.id != null ? user.id.toHexString() : "new";
        rateLimitService.checkCodeGenerationLimit(userId);

        String code = generateUniqueCode();
        user.setReferralCode(code);
        return code;
    }

    public User validateReferralCode(String referralCode, String ipAddress) {
        if (ipAddress == null || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = "localhost";
        }
        rateLimitService.checkCodeValidationLimit(ipAddress);

        if (referralCode == null || referralCode.trim().isEmpty()) {
            throw new ValidationException("INVALID_REFERRAL_CODE", "Referral code is required");
        }

        User referrer = User.find("referralCode", referralCode.trim()).firstResult();
        if (referrer != null) {
            return referrer;
        }

        throw new ValidationException("INVALID_REFERRAL_CODE", "Referral code '" + maskCode(referralCode) + "' is not valid");
    }

    public String regenerateReferralCode(User user, String reason) {
        rateLimitService.checkCodeRegenerationLimit(user.id.toHexString());

        String newCode = generateUniqueCode();
        user.setReferralCode(newCode);
        user.setUpdatedAt(Instant.now());
        user.update();

        return newCode;
    }

    public void checkSelfReferral(User referrer, String newUserEmail) {
        if (referrer.getEmail().equals(newUserEmail)) {
            throw new ValidationException("INVALID_REFERRAL", "Cannot use your own referral code");
        }
    }

    public String maskCode(String code) {
        if (code == null || code.isEmpty()) {
            return "***";
        }
        if (code.length() <= 3) {
            return "***";
        }
        return code.substring(0, 3) + "***";
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int j = 0; j < CODE_LENGTH; j++) {
                code.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
            }
            if (User.find("referralCode", code.toString()).firstResult() == null) {
                return code.toString();
            }
        }
        throw new ValidationException("REFERRAL_CODE_GENERATION_FAILED", "Unable to generate unique referral code. Please try again.");
    }
}
