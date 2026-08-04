package com.psycorp.psychapi.infrastructure.security;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final String REALM = "JWT";
    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    JwtService jwtService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String token = null;

        // Get Authorization header
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        } else {
            String cookieName = jwtService != null && jwtService.cookieName != null
                ? jwtService.cookieName
                : "__session";
            
            String cookieHeader = requestContext.getHeaderString(HttpHeaders.COOKIE);
            
            if (cookieHeader != null && !cookieHeader.isEmpty()) {
                token = extractCookieValue(cookieHeader, cookieName);
            }
        }

        if (token == null) {
            return;
        }

        // Validate token
        if (!jwtService.validateToken(token)) {
            // Invalid token
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"success\": false, \"message\": \"Invalid or expired token\", \"code\": \"UNAUTHORIZED\"}")
                    .build()
            );
            return;
        }

        // Extract claims
        String userId = jwtService.getUserIdFromToken(token);
        JwtService.TokenClaims claims = jwtService.parseClaims(token);

        // Create SecurityContext with authenticated user
        final SecurityContext originalSecurityContext = requestContext.getSecurityContext();
        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            @SuppressWarnings("Convert2Lambda")
            public Principal getUserPrincipal() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return userId;
                    }
                };
            }

            @Override
            public boolean isUserInRole(String role) {
                // Check for SUPERADMIN role using isSuperAdmin claim
                if ("SUPERADMIN".equals(role)) {
                    return Boolean.TRUE.equals(claims.isSuperAdmin());
                }
                
                List<String> roles = claims.roles();
                return roles != null && roles.contains(role);
            }

            @Override
            public boolean isSecure() {
                return originalSecurityContext != null && originalSecurityContext.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return REALM;
            }
        });
    }

    private String extractCookieValue(String cookieHeader, String cookieName) {
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && parts[0].trim().equals(cookieName)) {
                return parts[1].trim();
            }
        }
        return null;
    }
}
