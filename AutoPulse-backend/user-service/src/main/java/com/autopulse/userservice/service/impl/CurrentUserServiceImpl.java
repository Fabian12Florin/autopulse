package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.DispatcherProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;
    private final DispatcherProfileRepository dispatcherProfileRepository;
    private final CourierProfileRepository courierProfileRepository;

    public CurrentUserServiceImpl(UserRepository userRepository,
                                  DispatcherProfileRepository dispatcherProfileRepository,
                                  CourierProfileRepository courierProfileRepository) {
        this.userRepository = userRepository;
        this.dispatcherProfileRepository = dispatcherProfileRepository;
        this.courierProfileRepository = courierProfileRepository;
    }

    @Override
    public UUID requireKeycloakUserId() {
        Jwt jwt = requireJwt();
        String subject = jwt.getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Authenticated principal does not expose a valid Keycloak user id");
        }
    }

    @Override
    public String getCurrentUsername() {
        Jwt jwt = requireJwt();
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        return preferredUsername != null ? preferredUsername : jwt.getSubject();
    }

    @Override
    public boolean hasRole(String role) {
        String authority = normalizeRole(role);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles)
                .filter(Objects::nonNull)
                .anyMatch(this::hasRole);
    }

    @Override
    public User requireLocalUser() {
        UUID keycloakUserId = requireKeycloakUserId();
        return userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new NotFoundException("Authenticated user is not provisioned in user-service"));
    }

    @Override
    public DispatcherProfile requireCurrentDispatcherProfile() {
        UUID keycloakUserId = requireKeycloakUserId();
        return dispatcherProfileRepository.findByUserKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new NotFoundException("Authenticated dispatcher profile was not found"));
    }

    @Override
    public CourierProfile requireCurrentCourierProfile() {
        UUID keycloakUserId = requireKeycloakUserId();
        return courierProfileRepository.findByUserKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new NotFoundException("Authenticated courier profile was not found"));
    }

    private Jwt requireJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        throw new NotFoundException("No authenticated JWT principal found");
    }

    private String normalizeRole(String role) {
        String value = role.trim();
        return value.startsWith("ROLE_") ? value : "ROLE_" + value;
    }
}
