package com.psycorp.psychapi.shared.response;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.NewCookie;

@ApplicationScoped
public class CookieHelper {

    @Inject
    CookieConfig cookieConfig;

    private static final String COOKIE_PATH = "/";
    private static final boolean HTTP_ONLY = true;

    public NewCookie createRefreshCookie(String token) {
        return createTokenCookie(
            cookieConfig.getCookieName(),
            token,
            604800,  // 7 days
            cookieConfig.isSecure(),
            cookieConfig.getCookieDomain()
        );
    }

    public NewCookie createAccessTokenCookie(String token) {
        return createTokenCookie(
            "token",
            token,
            3600,  // 1 hour
            cookieConfig.isSecure(),
            cookieConfig.getCookieDomain()
        );
    }

    public NewCookie createTokenCookie(String name, String token, int maxAge) {
        return createTokenCookie(name, token, maxAge, cookieConfig.isSecure(), cookieConfig.getCookieDomain());
    }

    public NewCookie createTokenCookie(String name, String token, int maxAge, boolean secure, String domain) {
        NewCookie.Builder builder = new NewCookie.Builder(name)
            .value(token)
            .path(COOKIE_PATH)
            .secure(secure)
            .httpOnly(HTTP_ONLY)
            .maxAge(maxAge)
            .comment("Authentication token");

        if (domain != null && !domain.isEmpty()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    public NewCookie deleteCookie() {
        return deleteCookie(cookieConfig.getCookieName());
    }

    public NewCookie deleteCookie(String name) {
        return new NewCookie.Builder(name)
            .value("")
            .path(COOKIE_PATH)
            .maxAge(0)
            .httpOnly(HTTP_ONLY)
            .build();
    }

    public List<NewCookie> buildAuthCookies(String token, String refreshToken) {
        boolean secure = cookieConfig.isSecure();
        String domain = cookieConfig.getCookieDomain();

        return List.of(
            createTokenCookie("token", token, 3600, secure, domain),
            createTokenCookie(cookieConfig.getCookieName(), refreshToken, 604800, secure, domain)
        );
    }

    public List<NewCookie> buildLogoutCookies() {
        return List.of(deleteCookie());
    }
}
