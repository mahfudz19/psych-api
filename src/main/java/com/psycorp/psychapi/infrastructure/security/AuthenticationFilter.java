package com.psycorp.psychapi.infrastructure.security;

import java.security.Principal;
import java.util.List;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.auth.service.JwtService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @Inject
    JwtService jwtService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String token = extractToken(requestContext.getHeaderString("Authorization"));
        
        // Biarkan public API (tanpa token) lewat
        if (token == null) {
            return;
        }

        try {
            // 1. Validasi Token
            ObjectId userId = jwtService.validateAccessToken(token);

            // 2. Query Database (Hanya 1x per request)
            User user = User.findById(userId);
            if (user == null) {
                throw new Exception("User not found");
            }

            // 3. Simpan data user ke Context agar bisa dipakai di Endpoint
            requestContext.setProperty("validatedUser", user);

            // 4. Bangun Security Context dengan Logika Dinamis
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> userId.toHexString();
                }

                @Override
                public boolean isUserInRole(String role) {
                    if (role == null) return false;

                    // // --- LOGIKA KUSTOM example (ABAC) ---
                    if (role.equals("ORG_OWNER")) {
                        return User.OrganizationRole.owner.equals(user.getOrganizationRole());
                    }
                    if (role.equals("ORG_ADMIN")) {
                        return List.of(User.OrganizationRole.owner, User.OrganizationRole.admin).contains(user.getOrganizationRole());
                    }

                    // --- LOGIKA DEFAULT (RBAC) ---
                    List<User.Role> userRoles = user.getRoles();
                    return userRoles != null && userRoles.stream().anyMatch(r -> r.name().equals(role));
                }

                @Override
                public boolean isSecure() {
                    return requestContext.getUriInfo().getRequestUri().getScheme().equals("https");
                }

                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });

        } catch (Exception e) {
            ApiErrorResponse errorResponse = ApiErrorResponse.of(
                "UNAUTHORIZED",
                "Invalid or expired token"
            );
            
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponse)
                        .type(MediaType.APPLICATION_JSON)
                        .build()
            );
        }
    }
    
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}