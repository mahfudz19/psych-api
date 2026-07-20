package com.psycorp.psychapi.domain.service;

import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.infrastructure.security.JwtService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service layer untuk orchestrate authentication flow.
 * 
 * Service ini memisahkan business logic authentication dari Resource layer,
 * sehingga AuthResource hanya bertugas handle HTTP request/response.
 */
@ApplicationScoped
public class AuthService {

    @Inject
    UserService userService;

    @Inject
    JwtService jwtService;

    /**
     * Hasil dari authentication yang berhasil.
     * 
     * @param user User yang berhasil di-authenticate
     * @param token JWT token yang dihasilkan
     */
    public record AuthenticationResult(User user, String token) {}

    /**
     * Authenticate user dengan email dan password.
     * 
     * @param email Email user
     * @param password Password user (plain text)
     * @return AuthenticationResult berisi user dan JWT token
     * @throws com.psycorp.psychapi.infrastructure.exception.ValidationException jika credentials invalid
     */
    public AuthenticationResult login(String email, String password) {
        // 1. Authenticate user (validasi credentials di UserService)
        User user = userService.authenticate(email, password);
        
        // 2. Generate JWT token
        String token = jwtService.generateToken(user);
        
        // 3. Return result
        return new AuthenticationResult(user, token);
    }

    /**
     * Register user baru.
     * 
     * @param email Email user
     * @param password Password user (plain text)
     * @param fullName Nama lengkap user
     * @param referralCode Kode referral (optional)
     * @param accountType Tipe akun (INDIVIDUAL atau ORGANIZATION)
     * @param inviteCode Kode undangan organization (optional)
     * @param invitedBy User ID yang mengundang (optional)
     * @param invitedOrganizationId Organization ID (optional)
     * @param invitationRole Role di organization (optional)
     * @return AuthenticationResult berisi user dan JWT token
     */
    public AuthenticationResult register(
        String email,
        String password,
        String fullName,
        String referralCode,
        com.psycorp.psychapi.domain.model.User.AccountType accountType,
        String inviteCode,
        String invitedBy,
        String invitedOrganizationId,
        String invitationRole
    ) {
        // 1. Register user baru
        User user = userService.register(
            email,
            password,
            fullName,
            referralCode,
            accountType,
            inviteCode,
            invitedBy,
            invitedOrganizationId,
            invitationRole
        );
        
        // 2. Generate JWT token
        String token = jwtService.generateToken(user);
        
        // 3. Return result
        return new AuthenticationResult(user, token);
    }

    /**
     * Logout user.
     * 
     * @param userId User ID yang sedang logout
     */
    public void logout(String userId) {
        userService.logout(userId);
    }

    /**
     * Extract user information dari JWT token.
     * Method ini hanya menggunakan claims dari token, tidak query database.
     *
     * @param token JWT token
     * @return User object dengan informasi dari token (hanya field yang ada di claims)
     * @throws com.psycorp.psychapi.infrastructure.exception.ValidationException jika token invalid
     */
    public User getUserFromToken(String token) {
        return jwtService.extractUserFromToken(token);
    }

    /**
     * Extract user information dari SecurityContext (yang sudah di-set oleh JwtAuthenticationFilter).
     * Method ini mencari token dari header request dan extract user info.
     *
     * @param securityContext SecurityContext dari request
     * @return User object dengan informasi dari token
     */
    public User getCurrentUserFromToken(jakarta.ws.rs.core.SecurityContext securityContext) {
        // Extract token dari header (filter sudah validasi, jadi kita bisa langsung parse)
        String userId = securityContext.getUserPrincipal().getName();
        
        // Query database untuk mendapatkan user lengkap
        // Karena dari token kita hanya dapat id, email, roles
        return userService.getUserById(userId);
    }
}
