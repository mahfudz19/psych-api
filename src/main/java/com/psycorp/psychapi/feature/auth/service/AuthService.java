package com.psycorp.psychapi.feature.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.auth.api.dto.request.UpdateProfileRequest;
import com.psycorp.psychapi.feature.auth.api.dto.response.LoginResponse;
import com.psycorp.psychapi.feature.auth.api.dto.response.SessionResponse;
import com.psycorp.psychapi.feature.auth.api.dto.response.UserInfoResponse;
import com.psycorp.psychapi.feature.auth.model.DeviceInfo;
import com.psycorp.psychapi.feature.auth.model.RefreshToken;
import com.psycorp.psychapi.feature.auth.model.RefreshToken.RevokeReason;
import com.psycorp.psychapi.feature.auth.model.RefreshToken.TokenStatus;
import com.psycorp.psychapi.feature.storage.service.ProfilePictureChangedEvent;
import com.psycorp.psychapi.feature.storage.service.StorageService;
import com.psycorp.psychapi.feature.user.api.dto.response.UserResponse;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.feature.user.model.User.AccountType;
import com.psycorp.psychapi.feature.user.service.UserService;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.infrastructure.security.PasswordEncoder;
import com.psycorp.psychapi.shared.util.DocumentUpdater;
import com.psycorp.psychapi.shared.util.MongoFilter;
import com.psycorp.psychapi.shared.util.ValidationUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.InternalServerErrorException;

@ApplicationScoped
public class AuthService {

    @Inject
    JwtService jwtService;

    @Inject
    UserService userService;
    
    @Inject
    EmailService emailService;

    @Inject
    StorageService storageService;

    @Inject
    Event<ProfilePictureChangedEvent> profilePictureEvent;

    @ConfigProperty(name = "google.client.id")
    String googleClientId;
    /**
     * Register user baru dan generate tokens.
     */
    @Transactional
    public UserInfoResponse register(
        String email,
        String password,
        String fullName,
        String referralCode,
        AccountType accountType,
        String inviteCode,
        String invitedBy,
        String invitedOrganizationId,
        String invitationRole,
        DeviceInfo deviceInfo
    ) {
        // 1. Generate Plain Token untuk dikirim via Email
        String plainVerificationToken = UUID.randomUUID().toString();
        
        // 2. Hash token untuk disimpan di Database (keamanan)
        String hashedVerificationToken = hashToken(plainVerificationToken);
        
        // 3. Set waktu kedaluwarsa link (misal: 15 menit dari sekarang)
        Instant verificationExpiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        User user = userService.register(
            email,
            password,
            fullName,
            referralCode,
            accountType,
            inviteCode,
            invitedBy,
            invitedOrganizationId,
            invitationRole,
            hashedVerificationToken,
            verificationExpiresAt
        );

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), plainVerificationToken);

        return UserInfoResponse.from(user); 
    }

    /**
     * Memproses permintaan kirim ulang email verifikasi.
     */
    @Transactional
    public UserInfoResponse resendVerificationEmail(String email) {
        // 1. Cari user berdasarkan email
        User user = User.find("email", email).firstResult();
        if (user == null) {
            // Gunakan pesan generic agar attacker tidak tahu email ini terdaftar atau tidak
            throw new ValidationException("INVALID_REQUEST", "Jika email terdaftar, tautan verifikasi akan dikirim.");
        }

        // 2. Cegah pengiriman jika akun sudah aktif
        if (User.Status.ACTIVE.equals(user.getStatus())) {
            throw new ValidationException("ALREADY_VERIFIED", "Akun ini sudah aktif. Silakan langsung login.");
        }

        // 3. Generate token baru
        String plainNewToken = java.util.UUID.randomUUID().toString();
        String hashedNewToken = hashToken(plainNewToken);
        Instant newVerificationExpiresAt = Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES);

        // 4. Update data user (Token baru & perpanjang TTL 24 jam)
        user.renewVerification(hashedNewToken, newVerificationExpiresAt);

        // 5. Kirim email di background
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), plainNewToken);

        return UserInfoResponse.from(user);
    }

    /**
     * Verifikasi email menggunakan plain token dari URL.
     * Jika sukses, user diaktifkan dan langsung diberikan JWT Session (Login Otomatis).
     */
    @Transactional
    public LoginResponse verifyEmail(String email, String plainToken, DeviceInfo deviceInfo) {
        // 1. Cari user berdasarkan email
        User user = User.find("email", email).firstResult();
        if (user == null) {
            throw new ValidationException("INVALID_REQUEST", "Data pengguna tidak ditemukan.");
        }

        // 2. Cegah verifikasi ulang jika sudah aktif
        if (User.Status.ACTIVE.equals(user.getStatus())) {
            throw new ValidationException("ALREADY_VERIFIED", "Akun ini sudah diverifikasi sebelumnya.");
        }

        // 3. Pastikan token belum expired
        if (user.getVerificationExpiresAt() == null || Instant.now().isAfter(user.getVerificationExpiresAt())) {
            throw new ValidationException("TOKEN_EXPIRED", "Tautan verifikasi sudah kedaluwarsa. Silakan minta tautan baru.");
        }

        // 4. Cocokkan Hash Token
        String hashedInputToken = hashToken(plainToken);
        if (!hashedInputToken.equals(user.getVerificationToken())) {
            throw new ValidationException("INVALID_TOKEN", "Tautan verifikasi tidak valid.");
        }

        // 5. Aktivasi akun dan bersihkan data verifikasi
        user.activateAccount(); // Memanggil metode yang kita buat di User.java

        // 6. Generate Session (Otomatis Login setelah verifikasi sukses)
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refreshToken = jwtService.generateRefreshToken();
        Instant refreshTokenExpiry = jwtService.getRefreshTokenExpiry();

        saveRefreshToken(user.getId(), refreshToken, refreshTokenExpiry, deviceInfo);

        UserResponse userResponse = UserResponse.fromEntity(user);
        return LoginResponse.of(userResponse, accessToken, refreshToken, jwtService.getAccessTokenExpiry());
    }

    /**
     * Authenticate user dengan email dan password.
     */
    @Transactional
    public LoginResponse login(String email, String password, DeviceInfo deviceInfo) {
        // Authenticate user (menggunakan UserService)
        User user = userService.authenticate(email, password);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.id, user.getEmail(), user.getRoles());
        String refreshToken = jwtService.generateRefreshToken();
        Instant refreshTokenExpiry = jwtService.getRefreshTokenExpiry();

        // Save refresh token
        saveRefreshToken(user.id, refreshToken, refreshTokenExpiry, deviceInfo);

        // Build response
        UserResponse userResponse = UserResponse.fromEntity(user);

        return LoginResponse.of(userResponse, accessToken, refreshToken, jwtService.getAccessTokenExpiry());
    }

    /**
     * Refresh access token dengan refresh token.
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken, DeviceInfo deviceInfo) {
        // Hash refresh token untuk lookup
        String tokenHash = hashToken(refreshToken);

        // Find refresh token di database
        RefreshToken tokenEntity = RefreshToken.findByTokenHash(tokenHash);

        if (tokenEntity == null) {
            throw new ValidationException("INVALID_TOKEN", "Refresh token tidak ditemukan");
        }

        // Validate token status
        if (!tokenEntity.isActive()) {
            if (tokenEntity.isExpired()) {
                throw new ValidationException("TOKEN_EXPIRED", "Refresh token sudah expired");
            }
            if (tokenEntity.isRevoked()) {
                throw new ValidationException("TOKEN_REVOKED", "Refresh token sudah dicabut: " + tokenEntity.getRevokeReason());
            }
            if (tokenEntity.isRotated()) {
                // Token sudah di-rotate, cek apakah ini reuse attempt
                throw new ValidationException("TOKEN_ROTATED", "Refresh token sudah di-rotate. Gunakan token terbaru.");
            }
        }

        // Check for token reuse (security alert)
        if (tokenEntity.getUsedAt() != null && tokenEntity.getRotatedAt() != null 
            && tokenEntity.getUsedAt().isAfter(tokenEntity.getRotatedAt())) {
            // Token reuse detected! Revoke all sessions untuk user ini.
            RefreshToken.revokeAllByUserId(tokenEntity.getUserId(), RefreshToken.RevokeReason.fromValue("TOKEN_REUSE_DETECTED"));
            throw new ValidationException("SECURITY_ALERT", 
                "Refresh token reuse detected. Semua session telah dicabut untuk keamanan.");
        }

        // Get user
        ObjectId userId = tokenEntity.getUserId();
        User user = User.findById(userId);
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User tidak ditemukan");
        }

        // Mark old token sebagai used
        tokenEntity.markAsUsed();

        // Generate NEW tokens
        String newAccessToken = jwtService.generateAccessToken(userId, user.getEmail(), user.getRoles());
        String newRefreshToken = jwtService.generateRefreshToken();
        Instant newRefreshTokenExpiry = jwtService.getRefreshTokenExpiry();

        // Mark old token sebagai rotated
        ObjectId newTokenId = saveRefreshToken(userId, newRefreshToken, newRefreshTokenExpiry, deviceInfo);
        tokenEntity.rotate(newTokenId);

        // Build response (hanya token, tanpa user info)
        return LoginResponse.ofRefresh(newAccessToken, newRefreshToken, jwtService.getAccessTokenExpiry());
    }

    public void logout(ObjectId userId, String targetSessionId, String currentRefreshToken) {
        RefreshToken targetToken;

        if (targetSessionId != null && !targetSessionId.isBlank()) {
            ObjectId sessionObjectId = ValidationUtils.validateObjectId(targetSessionId);
            
            if (currentRefreshToken != null && !currentRefreshToken.isBlank()) {
                RefreshToken currentToken = RefreshToken.findByTokenHash(hashToken(currentRefreshToken));
                if (currentToken != null && sessionObjectId.equals(currentToken.getId())) {
                    throw new ValidationException("CANNOT_REVOKE_CURRENT_SESSION", 
                        "Gunakan tombol Logout untuk mengakhiri sesi yang sedang aktif saat ini.");
                }
            }
            
            targetToken = RefreshToken.findById(sessionObjectId);
        } else {
            // Mode 2: Current session dari cookie
            if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
                throw new ValidationException("INVALID_REQUEST", "Tidak ada sesi yang aktif. Silakan login ulang.");
            }
            String tokenHash = hashToken(currentRefreshToken);
            targetToken = RefreshToken.findByTokenHash(tokenHash);
        }

        // 1. Validasi keberadaan & kepemilikan token
        if (targetToken == null || !userId.equals(targetToken.getUserId())) {
            throw new ValidationException("SESSION_NOT_FOUND", "Sesi tidak ditemukan atau bukan milik Anda.");
        }

        // 2. Validasi status aktif
        if (!targetToken.isActive()) {
            if (targetToken.isRotated()) {
                RefreshToken.revokeAllByUserId(userId, RevokeReason.fromValue("TOKEN_REUSE_DETECTED"));
                throw new ValidationException("SECURITY_ALERT", "Token telah di-rotate sebelumnya. Semua sesi dicabut demi keamanan.");
            }
            throw new ValidationException("SESSION_ALREADY_INACTIVE", "Sesi ini sudah tidak aktif atau telah di-logout sebelumnya.");
        }

        // 3. Revoke token
        Bson filter = MongoFilter.and(
            Filters.eq("_id", targetToken.getId()),
            Filters.eq("userId", userId),
            Filters.in("status", TokenStatus.active.getValue())
        );

        Bson update = DocumentUpdater.update()
            .set("status", TokenStatus.revoked.getValue())
            .set("revokeReason", RevokeReason.LOGOUT.getValue())
            .set("updatedAt", Instant.now())
            .build();

        RefreshToken.mongoCollection().updateOne(filter, update);
    }
    
    /**
     * Save refresh token ke database.
     */
    private ObjectId saveRefreshToken(ObjectId userId, String refreshToken, Instant expiry, DeviceInfo deviceInfo) {
        String tokenHash = hashToken(refreshToken);
        String tokenPrefix = refreshToken.substring(0, Math.min(8, refreshToken.length()));

        RefreshToken token = new RefreshToken();
        token.setTokenHash(tokenHash);
        token.setTokenPrefix(tokenPrefix);
        token.setUserId(userId);
        token.setDeviceId(generateDeviceId());
        token.setDeviceInfo(deviceInfo);
        token.setStatus(RefreshToken.TokenStatus.active.getValue());
        token.setExpiresAt(expiry);
        token.setCreatedAt(Instant.now());
        token.setUpdatedAt(Instant.now());

        token.persist();
        return token.id;
    }

    /**
     * Hash refresh token dengan SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ValidationException("INVALID_TOKEN", "Error hashing token");
        }
    }

    /**
     * Generate device ID sederhana.
     */
    private String generateDeviceId() {
        return "dev_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get all sessions untuk user dengan pagination.
     */
    public List<SessionResponse> getSessions(ObjectId userId, Bson filter, Bson sort, int page, int limit) {
        Bson baseFilter = MongoFilter.and(Filters.eq("userId", userId), filter);

        List<RefreshToken> tokens = RefreshToken.find(baseFilter, sort)
            .page(page - 1, limit)
            .list();

        return tokens.stream()
            .map(token -> SessionResponse.fromEntity(token, null))
            .toList();
    }

    /**
     * Get total count sessions untuk user.
     */
    public long getSessionsCount(ObjectId userId, Bson filter) {
        Bson baseFilter = MongoFilter.and(Filters.eq("userId", userId), filter);
        return RefreshToken.count(baseFilter);
    }

    /**
     * Meminta tautan reset password (Lupa Password).
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = User.find("email", email).firstResult();
        if (user == null) {
            return; 
        }
        if (User.Status.DELETED.equals(user.getStatus()) || User.Status.SUSPENDED.equals(user.getStatus())) {
            return;
        }
        String plainToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(plainToken);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        user.applyPasswordResetToken(hashedToken, expiresAt);

        emailService.sendResetPasswordEmail(user.getEmail(), user.getFullName(), plainToken);
    }

    /**
     * Mengeksekusi penggantian password menggunakan token.
     */
    @Transactional
    public void resetPassword(String plainToken, String newPassword) {
        String hashedToken = hashToken(plainToken);

        User user = User.find("resetPasswordToken", hashedToken).firstResult();
        if (user == null) {
            throw new ValidationException("INVALID_TOKEN", "Tautan pengaturan ulang kata sandi tidak valid atau salah.");
        }

        if (user.getResetPasswordExpiresAt() == null || Instant.now().isAfter(user.getResetPasswordExpiresAt())) {
            throw new ValidationException("TOKEN_EXPIRED", "Tautan pengaturan ulang kata sandi sudah kedaluwarsa. Silakan minta tautan baru.");
        }

        String newHashedPassword = PasswordEncoder.hash(newPassword);
        user.resetPassword(newHashedPassword);

        RefreshToken.revokeAllByUserId(user.getId(), RefreshToken.RevokeReason.fromValue("PASSWORD_CHANGED"));
    }

    /**
     * Authenticate user menggunakan Google ID Token HANYA untuk akun yang sudah ada.
     */
    @Transactional
    public LoginResponse googleLoginOnly(String idTokenString, DeviceInfo deviceInfo) {
        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = new GsonFactory();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new ValidationException("INVALID_TOKEN", "Token Google tidak valid atau sudah kedaluwarsa.");
            }

            String email = idToken.getPayload().getEmail();
            String googleId = idToken.getPayload().getSubject();

            User user = User.find("email", email).firstResult();
            
            // TOLAK JIKA BELUM TERDAFTAR
            if (user == null) {
                throw new ValidationException("USER_NOT_FOUND", "Akun Anda belum terdaftar. Silakan mendaftar terlebih dahulu.");
            }

            // CEK STATUS BANNED/DELETED
            if (User.Status.SUSPENDED.equals(user.getStatus()) || User.Status.DELETED.equals(user.getStatus())) {
                throw new ValidationException("ACCOUNT_INACTIVE", "Akun Anda ditangguhkan atau dihapus.");
            }

            boolean needUpdate = false;
            
            // SINKRONISASI JIKA SEBELUMNYA DAFTAR MANUAL
            if (user.getProviderId() == null || !user.getProvider().equals("google")) {
                user.setProvider("google");
                user.setProviderId(googleId);
                needUpdate = true;
            }

            // AKTIVASI OTOMATIS JIKA SEBELUMNYA MALAS KLIK EMAIL
            if (User.Status.PENDING.equals(user.getStatus())) {
                user.setStatus(User.Status.ACTIVE);
                user.setVerificationToken(null);
                user.setVerificationExpiresAt(null);
                user.setExpiredAt(null);
                needUpdate = true;
            }

            if (needUpdate) {
                user.setUpdatedAt(Instant.now());
                user.update(); 
            }

            // GENERATE SESSION
            String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
            String refreshToken = jwtService.generateRefreshToken();
            Instant refreshTokenExpiry = jwtService.getRefreshTokenExpiry();

            saveRefreshToken(user.getId(), refreshToken, refreshTokenExpiry, deviceInfo);

            UserResponse userResponse = UserResponse.fromEntity(user);
            return LoginResponse.of(userResponse, accessToken, refreshToken, jwtService.getAccessTokenExpiry());

        } catch (java.security.GeneralSecurityException | java.io.IOException e) {
            throw new ValidationException("AUTH_FAILED", "Gagal memvalidasi token Google: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_TOKEN_FORMAT", "Format token tidak valid. Pastikan Anda mengirimkan ID Token (JWT) Google.");
        } catch (ValidationException ve) {
            throw ve;
        } catch (RuntimeException e) {
            throw new ValidationException("AUTH_FAILED", "Gagal mengautentikasi akun Google: " + e.toString());
        }
    }

    /**
     * Registrasi user baru menggunakan Google ID Token beserta logika organisasi/referral.
     */
    @Transactional
    public LoginResponse googleRegister(
        String idTokenString, 
        AccountType accountType,
        String referralCode,
        String inviteCode,
        String invitedBy,
        String invitedOrganizationId,
        String invitationRole,
        DeviceInfo deviceInfo
    ) {
        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = new GsonFactory();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new ValidationException("INVALID_TOKEN", "Token Google tidak valid atau sudah kedaluwarsa.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");

            User existingUser = User.find("email", email).firstResult();
            
            // TOLAK JIKA SUDAH TERDAFTAR
            if (existingUser != null) {
                throw new ValidationException("EMAIL_EXISTS", "Email sudah terdaftar. Silakan langsung login menggunakan Google.");
            }

            // 1. PINJAM LOGIKA REGISTRASI UTAMA UNTUK VALIDASI REFERRAL/ORGANIZATION
            // Kita menggunakan UUID acak sebagai dummy password karena pendaftaran manual mewajibkannya.
            String dummyPassword = UUID.randomUUID().toString() + "Ggl1!"; 
            
            User user = userService.register(
                email,
                dummyPassword,
                name,
                referralCode,
                accountType,
                inviteCode,
                invitedBy,
                invitedOrganizationId,
                invitationRole,
                null, // Tidak butuh token verifikasi email
                null
            );

            // 2. TIMPA PENGATURAN BAWAAN MENJADI GOOGLE SSO
            user.setProvider("google");
            user.setProviderId(googleId);
            
            // Langsung aktifkan tanpa verifikasi email
            user.setStatus(User.Status.ACTIVE);
            user.setExpiredAt(null);
            
            // Kosongkan password agar murni menjadi akun SSO
            user.setPassword(null);
            user.setUpdatedAt(Instant.now());
            user.update(); // Simpan perubahan

            // 3. GENERATE SESSION (Berbeda dengan register manual, Google Register langsung login)
            String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
            String refreshToken = jwtService.generateRefreshToken();
            Instant refreshTokenExpiry = jwtService.getRefreshTokenExpiry();

            saveRefreshToken(user.getId(), refreshToken, refreshTokenExpiry, deviceInfo);

            UserResponse userResponse = UserResponse.fromEntity(user);
            return LoginResponse.of(userResponse, accessToken, refreshToken, jwtService.getAccessTokenExpiry());

        } catch (java.security.GeneralSecurityException | java.io.IOException e) {
            throw new ValidationException("AUTH_FAILED", "Gagal memvalidasi token Google: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_TOKEN_FORMAT", "Format token tidak valid. Pastikan Anda mengirimkan ID Token (JWT) Google.");
        } catch (ValidationException ve) {
            throw ve;
        } catch (RuntimeException e) {
            throw new ValidationException("AUTH_FAILED", "Gagal melakukan registrasi Google: " + e.toString());
        }
    }

    public void updateUserProfile(User user, UpdateProfileRequest request) {
        String oldPic = user.getProfilePicture();
        boolean isNewUpload = request.profilePicture() != null && (request.profilePicture().startsWith("temp/") || request.profilePicture().contains("/temp/"));

        // 1. Commit file baru dari temp/ (sinkron — butuh URL untuk DB)
        String newPic = oldPic;
        if (isNewUpload) {
            newPic = storageService.commitPublicFile(request.profilePicture());
        } else if (request.profilePicture() != null) {
            newPic = request.profilePicture();
        }

        // 2. Simpan ke DB
        try {
            user.updateProfile(
                request.fullName(),
                request.phone(),
                request.bio(),
                request.dateOfBirth(),
                request.gender(),
                newPic
            );
        } catch (Exception e) {
            if (isNewUpload) storageService.deletePublicFile(newPic);
            throw new InternalServerErrorException("Gagal menyimpan profil", e);
        }

        // 3. Hapus foto lama di background (fire and forget)
        if (isNewUpload && oldPic != null && !oldPic.isBlank()) {
            profilePictureEvent.fireAsync(new ProfilePictureChangedEvent(oldPic));
        }
    }
}
