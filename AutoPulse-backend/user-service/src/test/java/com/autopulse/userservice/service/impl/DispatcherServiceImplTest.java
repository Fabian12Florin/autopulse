package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.repository.DispatcherProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.service.CredentialNotificationService;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatcherServiceImplTest {

    private final DispatcherProfileRepository dispatcherProfileRepository = mock(DispatcherProfileRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final KeycloakAdminService keycloakAdminService = mock(KeycloakAdminService.class);
    private final CredentialNotificationService credentialNotificationService = mock(CredentialNotificationService.class);
    private final KeycloakAdminProperties keycloakAdminProperties = TestDataFactory.keycloakProperties();
    private final DispatcherServiceImpl service = new DispatcherServiceImpl(
            dispatcherProfileRepository,
            userRepository,
            keycloakAdminService,
            credentialNotificationService,
            keycloakAdminProperties
    );

    private DispatcherProfile profile;

    @BeforeEach
    void setUp() {
        profile = TestDataFactory.dispatcherProfile();
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dispatcherProfileRepository.saveAndFlush(any(DispatcherProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPersistsDispatcherAndSendsInitialPassword() {
        when(keycloakAdminService.createUser(anyString(), anyString(), anyString(), anyString(), eq(true), eq("DISPATCHER")))
                .thenReturn(TestDataFactory.KEYCLOAK_ID);

        DispatcherResponse response = service.create(TestDataFactory.createDispatcherRequest());

        assertThat(response.regionCode()).isEqualTo("RO-B");
        assertThat(response.user().email()).isEqualTo("dispatcher@example.com");
        verify(credentialNotificationService).sendInitialPassword(any(User.class), eq("DISPATCHER"), anyString());
    }

    @Test
    void createRejectsDuplicateEmailAndCleansUpCreatedKeycloakUserOnFailure() {
        when(userRepository.existsByEmailIgnoreCase("dispatcher@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(TestDataFactory.createDispatcherRequest()))
                .isInstanceOf(ConflictException.class);

        when(userRepository.existsByEmailIgnoreCase("dispatcher@example.com")).thenReturn(false);
        when(keycloakAdminService.createUser(anyString(), anyString(), anyString(), anyString(), eq(true), eq("DISPATCHER")))
                .thenReturn(TestDataFactory.KEYCLOAK_ID);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.create(TestDataFactory.createDispatcherRequest()))
                .isInstanceOf(IllegalStateException.class);
        verify(keycloakAdminService).deleteUser(TestDataFactory.KEYCLOAK_ID);
    }

    @Test
    void createSurfacesCleanupFailureAsDataIntegrityViolation() {
        when(keycloakAdminService.createUser(anyString(), anyString(), anyString(), anyString(), eq(true), eq("DISPATCHER")))
                .thenReturn(TestDataFactory.KEYCLOAK_ID);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new IllegalStateException("db down"));
        doThrow(new IllegalStateException("cleanup down")).when(keycloakAdminService).deleteUser(TestDataFactory.KEYCLOAK_ID);

        assertThatThrownBy(() -> service.create(TestDataFactory.createDispatcherRequest()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getSearchAndUpdateDispatcher() {
        when(dispatcherProfileRepository.findById(TestDataFactory.DISPATCHER_PROFILE_ID)).thenReturn(Optional.of(profile));
        when(dispatcherProfileRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 5))))
                .thenReturn(new PageImpl<>(List.of(profile)));

        assertThat(service.getById(TestDataFactory.DISPATCHER_PROFILE_ID).regionCode()).isEqualTo("RO-B");
        assertThat(service.search("ro-b", true, PageRequest.of(0, 5))).hasSize(1);

        DispatcherResponse response = service.update(TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.updateDispatcherRequest(false));
        assertThat(response.regionCode()).isEqualTo("RO-C");
        assertThat(response.user().email()).isEqualTo("updated@example.com");
        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "updated@example.com", "Grace", "Hopper", false);
    }

    @Test
    void updateRejectsMissingProfileAndDuplicateEmail() {
        assertThatThrownBy(() -> service.getById(TestDataFactory.DISPATCHER_PROFILE_ID))
                .isInstanceOf(NotFoundException.class);

        when(dispatcherProfileRepository.findById(TestDataFactory.DISPATCHER_PROFILE_ID)).thenReturn(Optional.of(profile));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@example.com", TestDataFactory.USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.updateDispatcherRequest(true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCompensatesKeycloakWhenLocalSaveFails() {
        when(dispatcherProfileRepository.findById(TestDataFactory.DISPATCHER_PROFILE_ID)).thenReturn(Optional.of(profile));
        when(dispatcherProfileRepository.saveAndFlush(any(DispatcherProfile.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.update(TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.updateDispatcherRequest(true)))
                .isInstanceOf(IllegalStateException.class);

        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "updated@example.com", "Grace", "Hopper", true);
        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "driver@example.com", "Jane", "Driver", true);
    }
}
