package com.psycorp.psychapi.domain.service;

import com.psycorp.psychapi.config.JwtConfig;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.infrastructure.security.JwtService;
import com.psycorp.psychapi.infrastructure.security.SuperAdminService;

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

    @Inject
    SuperAdminService superAdminService;

    @Inject
    JwtConfig jwtConfig;

    /**
     * Hasil dari authentication yang berhasil.
     *
     * @param user User yang berhasil di-authenticate
     * @param token JWT token yang dihasilkan
     * @param expiresIn Waktu kadaluarsa token dalam detik
     */
    public record AuthenticationResult(User user, String token, long expiresIn) {}

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
        
        // 2. Check if user is superadmin
        boolean isSuperAdmin = superAdminService.isSuperAdmin(email);
        
        // 3. Generate JWT token with isSuperAdmin claim
        String token = jwtService.generateToken(user, isSuperAdmin);
        
        // 4. Return result dengan expiresIn
        return new AuthenticationResult(user, token, jwtConfig.expiresIn());
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
        
        // 2. Check if user is superadmin
        boolean isSuperAdmin = superAdminService.isSuperAdmin(email);
        
        // 3. Generate JWT token with isSuperAdmin claim
        String token = jwtService.generateToken(user, isSuperAdmin);
        
        // 4. Return result dengan expiresIn
        return new AuthenticationResult(user, token, jwtConfig.expiresIn());
    }

    public void logout(String userId) {
        userService.logout(userId);
    }

    public User getUserFromToken(String token) {
        return jwtService.extractUserFromToken(token);
    }

    public User getCurrentUserFromToken(jakarta.ws.rs.core.SecurityContext securityContext) {
        String userId = securityContext.getUserPrincipal().getName();
        
        return userService.getUserById(userId);
    }
}
