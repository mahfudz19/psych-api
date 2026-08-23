package com.psycorp.psychapi.shared.response;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Configuration class untuk cookie settings.
 * Mengambil nilai dari application.properties atau environment variables.
 */
@ApplicationScoped
public class CookieConfig {

    /**
     * Nama cookie untuk refresh token.
     * Default: "refresh_token"
     */
    @ConfigProperty(name = "app.jwt.cookie.name", defaultValue = "refresh_token")
    String cookieName;

    /**
     * Domain cookie (opsional).
     * Kosong untuk localhost, isi domain untuk production.
     * Default: tidak ada (null)
     */
    @ConfigProperty(name = "app.jwt.cookie.domain")
    Optional<String> cookieDomain;

    /**
     * Quarkus profile yang aktif.
     * "dev" untuk development, "prod" untuk production.
     */
    @ConfigProperty(name = "quarkus.profile", defaultValue = "dev")
    String profile;

    /**
     * Flag untuk secure cookie.
     * Jika true, cookie hanya dikirim via HTTPS.
     */
    @ConfigProperty(name = "app.jwt.cookie.secure", defaultValue = "false")
    boolean cookieSecure;

    /**
     * Cek apakah running di production mode.
     * @return true jika profile adalah "prod" atau "production"
     */
    public boolean isProduction() {
        return "prod".equals(profile) || "production".equals(profile);
    }

    /**
     * Determine secure flag berdasarkan profile dan konfigurasi.
     * Secure = true hanya jika production ATAU app.jwt.cookie.secure=true.
     * @return true jika cookie harus secure
     */
    public boolean isSecure() {
        return isProduction() || cookieSecure;
    }

    /**
     * Get nama cookie yang terkonfigurasi.
     * @return cookie name
     */
    public String getCookieName() {
        return cookieName;
    }

    /**
     * Get domain cookie yang terkonfigurasi.
     * @return cookie domain atau null jika tidak dikonfigurasi
     */
    public String getCookieDomain() {
        return cookieDomain.orElse(null);
    }

    /**
     * Get profile yang aktif.
     * @return quarkus profile
     */
    public String getProfile() {
        return profile;
    }
}
