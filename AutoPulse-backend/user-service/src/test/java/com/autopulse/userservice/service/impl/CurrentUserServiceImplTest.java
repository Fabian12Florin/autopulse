package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.DispatcherProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.testutil.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DispatcherProfileRepository dispatcherProfileRepository = mock(DispatcherProfileRepository.class);
    private final CourierProfileRepository courierProfileRepository = mock(CourierProfileRepository.class);
    private final CurrentUserServiceImpl service = new CurrentUserServiceImpl(
            userRepository,
            dispatcherProfileRepository,
            courierProfileRepository
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesJwtSubjectUsernameRolesAndProfiles() {
        authenticate(TestDataFactory.KEYCLOAK_ID.toString(), Map.of("preferred_username", "driver"),
                "ROLE_ADMIN", "ROLE_COURIER");
        User user = TestDataFactory.user();
        DispatcherProfile dispatcher = TestDataFactory.dispatcherProfile();
        CourierProfile courier = TestDataFactory.courierProfile();
        when(userRepository.findByKeycloakUserId(TestDataFactory.KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(dispatcherProfileRepository.findByUserKeycloakUserId(TestDataFactory.KEYCLOAK_ID)).thenReturn(Optional.of(dispatcher));
        when(courierProfileRepository.findByUserKeycloakUserId(TestDataFactory.KEYCLOAK_ID)).thenReturn(Optional.of(courier));

        assertThat(service.requireKeycloakUserId()).isEqualTo(TestDataFactory.KEYCLOAK_ID);
        assertThat(service.getCurrentUsername()).isEqualTo("driver");
        assertThat(service.hasRole("ADMIN")).isTrue();
        assertThat(service.hasRole("ROLE_COURIER")).isTrue();
        assertThat(service.hasAnyRole(null, "DISPATCHER", "COURIER")).isTrue();
        assertThat(service.requireLocalUser()).isSameAs(user);
        assertThat(service.requireCurrentDispatcherProfile()).isSameAs(dispatcher);
        assertThat(service.requireCurrentCourierProfile()).isSameAs(courier);
    }

    @Test
    void fallsBackToSubjectAsUsernameAndReturnsFalseWithoutAuthentication() {
        authenticate(TestDataFactory.KEYCLOAK_ID.toString(), Map.of(), "ROLE_ADMIN");
        assertThat(service.getCurrentUsername()).isEqualTo(TestDataFactory.KEYCLOAK_ID.toString());

        SecurityContextHolder.clearContext();
        assertThat(service.hasRole("ADMIN")).isFalse();
    }

    @Test
    void throwsWhenAuthenticationIsMissingOrSubjectIsInvalidOrLocalDataMissing() {
        assertThatThrownBy(service::requireKeycloakUserId).isInstanceOf(NotFoundException.class);

        authenticate("not-a-uuid", Map.of(), "ROLE_ADMIN");
        assertThatThrownBy(service::requireKeycloakUserId).isInstanceOf(NotFoundException.class);

        authenticate(TestDataFactory.KEYCLOAK_ID.toString(), Map.of(), "ROLE_ADMIN");
        assertThatThrownBy(service::requireLocalUser).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(service::requireCurrentDispatcherProfile).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(service::requireCurrentCourierProfile).isInstanceOf(NotFoundException.class);
    }

    private void authenticate(String subject, Map<String, Object> extraClaims, String... authorities) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        extraClaims.forEach(builder::claim);
        Jwt jwt = builder.build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
