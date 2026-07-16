package com.psycorp.psychapi.infrastructure.security;

import org.mindrot.jbcrypt.BCrypt;

import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Utility class untuk password hashing menggunakan BCrypt.
 *
 * BCrypt adalah password hashing function yang aman dengan fitur:
 * - Salt otomatis (setiap hash unik meskipun password sama)
 * - Configurable work factor (rounds) untuk adjust security vs performance
 * - One-way function (tidak bisa di-decrypt)
 */
@ApplicationScoped
public class PasswordEncoder {

    private static final int DEFAULT_ROUNDS = 10;

    /**
     * Hash password menggunakan BCrypt dengan work factor default.
     *
     * @param password Password plain text yang akan di-hash
     * @return Hashed password dengan format BCrypt
     * @throws ValidationException jika password null atau empty
     */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("INVALID_PASSWORD", "Password cannot be null or empty");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(DEFAULT_ROUNDS));
    }

    /**
     * Hash password dengan work factor custom.
     *
     * @param password Password plain text yang akan di-hash
     * @param rounds BCrypt work factor (4-31, recommended: 10-12)
     * @return Hashed password dengan format BCrypt
     * @throws ValidationException jika password null/empty atau rounds invalid
     */
    public static String hash(String password, int rounds) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("INVALID_PASSWORD", "Password cannot be null or empty");
        }
        if (rounds < 4 || rounds > 31) {
            throw new ValidationException("INVALID_BCRYPT_ROUNDS", "Rounds must be between 4 and 31");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(rounds));
    }

    public static boolean verify(String password, String hashedPassword) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Handle invalid hash format
            return false;
        }
    }

    public static boolean isBcryptHash(String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
