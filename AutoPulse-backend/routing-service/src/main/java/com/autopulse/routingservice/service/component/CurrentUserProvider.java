package com.autopulse.routingservice.service.component;

import com.autopulse.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            String userId = jwt.getClaimAsString("sub"); // sau "userId", depinde de tokenul tău

            if (userId == null || userId.isBlank()) {
                throw new UnauthorizedException("User id claim is missing from token");
            }

            try {
                return UUID.fromString(userId);
            } catch (IllegalArgumentException ex) {
                throw new UnauthorizedException("Invalid user id claim in token");
            }
        }

        throw new UnauthorizedException("Unsupported authentication principal");
    }
}