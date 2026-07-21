package com.psycorp.psychapi.infrastructure.security;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class SuperAdminAuthorizationFilter implements ContainerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        
        // Hanya cek untuk path admin
        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
            return;
        }

        // Get SecurityContext yang sudah di-set oleh JwtAuthenticationFilter
        SecurityContext securityContext = requestContext.getSecurityContext();
        
        // Jika tidak ada SecurityContext, berarti user belum authenticated
        if (securityContext == null) {
            abortWithUnauthorized(requestContext, "Authentication required");
            return;
        }

        // Cek apakah user adalah superadmin
        boolean isSuperAdmin = securityContext.isUserInRole("SUPERADMIN");
        
        if (!isSuperAdmin) {
            abortWithForbidden(requestContext, "Access denied: Superadmin privileges required");
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"success\": false, \"message\": \"" + message + "\", \"code\": \"UNAUTHORIZED\"}")
                .build()
        );
    }

    private void abortWithForbidden(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
            Response.status(Response.Status.FORBIDDEN)
                .entity("{\"success\": false, \"message\": \"" + message + "\", \"code\": \"FORBIDDEN\"}")
                .build()
        );
    }
}
