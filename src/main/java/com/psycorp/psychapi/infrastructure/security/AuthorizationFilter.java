package com.psycorp.psychapi.infrastructure.security;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.psycorp.psychapi.shared.response.ApiErrorResponse;

import io.quarkus.security.Authenticated;
import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) 
public class AuthorizationFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo; // Memungkinkan kita membaca anotasi dari Controller

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method method = resourceInfo.getResourceMethod();
        Class<?> declaringClass = resourceInfo.getResourceClass();

        // 1. Cek @PermitAll (Lewati pengamanan)
        if (method.isAnnotationPresent(PermitAll.class) || 
           (!method.isAnnotationPresent(RolesAllowed.class) && 
            !method.isAnnotationPresent(Authenticated.class) && 
            declaringClass.isAnnotationPresent(PermitAll.class))) {
            return;
        }

        // 2. Cek @DenyAll (Tolak mutlak)
        if (method.isAnnotationPresent(DenyAll.class) || declaringClass.isAnnotationPresent(DenyAll.class)) {
            abortWithForbidden(requestContext, "Endpoint is blocked by DenyAll");
            return;
        }

        boolean requiresAuthentication = false;
        Set<String> allowedRoles = new HashSet<>();

        // 3. Kumpulkan Anotasi dari Level Method dan Class
        RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            rolesAllowed = declaringClass.getAnnotation(RolesAllowed.class);
        }

        if (rolesAllowed != null) {
            requiresAuthentication = true;
            allowedRoles.addAll(Arrays.asList(rolesAllowed.value()));
        }

        Authenticated authenticated = method.getAnnotation(Authenticated.class);
        if (authenticated == null) {
            authenticated = declaringClass.getAnnotation(Authenticated.class);
        }

        if (authenticated != null) {
            requiresAuthentication = true;
        }

        // 4. Jika tidak ada anotasi keamanan, biarkan API Public lewat
        if (!requiresAuthentication) {
            return;
        }

        // 5. Verifikasi: Apakah User sudah login? (Punya Principal dari AuthenticationFilter)
        if (requestContext.getSecurityContext().getUserPrincipal() == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiErrorResponse.of("UNAUTHORIZED", "Unauthorized", "Authentication required"))  // ← Pakai ApiErrorResponse
                .type(MediaType.APPLICATION_JSON)
                .build());
            return;
        }


        // 6. Verifikasi: Cek Role
        if (!allowedRoles.isEmpty()) {
            boolean hasRole = false;
            
            // Loop melalui daftar role yang diizinkan (Logika OR: Salah satu cukup)
            for (String role : allowedRoles) {
                // PANGGIL isUserInRole() YANG ANDA BUAT DI AuthenticationFilter!
                if (requestContext.getSecurityContext().isUserInRole(role)) {
                    hasRole = true;
                    break;
                }
            }

            // Jika tidak punya satu pun role yang diizinkan, tendang dengan 403 Forbidden
            if (!hasRole) {
                abortWithForbidden(requestContext, "Access denied. Insufficient roles.");
            }
        }
    }

    private void abortWithForbidden(ContainerRequestContext requestContext, String message) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
            "FORBIDDEN",
            "Forbidden",
            message
        );
        
        requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
            .entity(errorResponse)
            .type(MediaType.APPLICATION_JSON)
            .build());
    }
}