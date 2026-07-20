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

/**
 * JWT Authentication Filter.
 * Filter ini akan mengekstrak dan memvalidasi JWT token dari Authorization header
 * untuk endpoint yang memerlukan authentication.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final String REALM = "JWT";
    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    JwtService jwtService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Get Authorization header
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Check if token exists
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No token provided - let the endpoint handle authorization
            // Some endpoints may be public (login, register, forgot-password)
            return;
        }

        // Extract token
        String token = authHeader.substring(BEARER_PREFIX.length());

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
                // Check if user has the specified role
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
}
