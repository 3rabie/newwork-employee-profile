package com.newwork.employee.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility for retrieving the authenticated user from the security context.
 */
public final class AuthenticatedUserAccessor {

    private AuthenticatedUserAccessor() {
    }

    public static UUID currentUserId() {
        return currentUser().getUserId();
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found in security context");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }
}
