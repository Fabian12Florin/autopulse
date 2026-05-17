package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.service.CredentialNotificationService;
import com.autopulse.userservice.service.CurrentUserService;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.PasswordResetResponse;
import com.autopulse.userservice.web.dto.TokenResponse;
import com.autopulse.userservice.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CourierProfileRepository courierProfileRepository = mock(CourierProfileRepository.class);
    private final KeycloakAdminService keycloakAdminService = mock(KeycloakAdminService.class);
    private final CredentialNotificationService credentialNotificationService = mock(CredentialNotificationService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final UserServiceImpl service = new UserServiceImpl(
            userRepository,
            courierProfileRepository,
            keycloakAdminService,
            credentialNotificationService,
            currentUserService
    );

    private User user;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user();
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getsSearchesAndReportsMissingUsers() {
        when(userRepository.findById(TestDataFactory.USER_ID)).thenReturn(Optional.of(user));
        when(currentUserService.requireLocalUser()).thenReturn(user);
        when(userRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(user)));

        assertThat(service.getById(TestDataFactory.USER_ID).email()).isEqualTo(user.getEmail());
        assertThat(service.getCurrentUser().email()).isEqualTo(user.getEmail());
        assertThat(service.search("jane", null, null, null, true, PageRequest.of(0, 10))).hasSize(1);
        assertThatThrownBy(() -> service.getById(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateSynchronizesKeycloakAndLocalUser() {
        when(userRepository.findById(TestDataFactory.USER_ID)).thenReturn(Optional.of(user));

        UserResponse response = service.update(TestDataFactory.USER_ID, TestDataFactory.updateUserRequest(false));

        assertThat(response.email()).isEqualTo("updated@example.com");
        assertThat(response.active()).isFalse();
        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "updated@example.com", "Grace", "Hopper", false);
    }

    @Test
    void updateRejectsDuplicateEmailAndOnRouteDeactivation() {
        when(userRepository.findById(TestDataFactory.USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@example.com", TestDataFactory.USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(TestDataFactory.USER_ID, TestDataFactory.updateUserRequest(true)))
                .isInstanceOf(ConflictException.class);

        CourierProfile onRoute = TestDataFactory.courierProfile(AvailabilityStatus.ON_ROUTE, "RO-B");
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@example.com", TestDataFactory.USER_ID)).thenReturn(false);
        when(courierProfileRepository.findByUserId(TestDataFactory.USER_ID)).thenReturn(Optional.of(onRoute));

        assertThatThrownBy(() -> service.update(TestDataFactory.USER_ID, TestDataFactory.updateUserRequest(false)))
                .isInstanceOf(BadRequestException.class);
        verify(keycloakAdminService, never()).updateUser(eq(TestDataFactory.KEYCLOAK_ID), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void updateCompensatesKeycloakWhenLocalSaveFails() {
        when(userRepository.findById(TestDataFactory.USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> service.update(TestDataFactory.USER_ID, TestDataFactory.updateUserRequest(true)))
                .isInstanceOf(IllegalStateException.class);

        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "updated@example.com", "Grace", "Hopper", true);
        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "driver@example.com", "Jane", "Driver", true);
    }

    @Test
    void updateCurrentUserKeepsExistingActiveStateAndHandlesConflicts() {
        when(currentUserService.requireLocalUser()).thenReturn(user);

        assertThat(service.updateCurrentUser(TestDataFactory.updateMyUserRequest()).email()).isEqualTo("me@example.com");
        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "me@example.com", "Current", "User", true);

        when(userRepository.existsByEmailIgnoreCaseAndIdNot("me@example.com", TestDataFactory.USER_ID)).thenReturn(true);
        assertThatThrownBy(() -> service.updateCurrentUser(TestDataFactory.updateMyUserRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void activateAndDeactivateSynchronizeEnabledStateAndRestoreOnFailure() {
        when(userRepository.findById(TestDataFactory.USER_ID)).thenReturn(Optional.of(user));

        assertThat(service.deactivate(TestDataFactory.USER_ID).active()).isFalse();
        assertThat(service.activate(TestDataFactory.USER_ID).active()).isTrue();

        doThrow(new IllegalStateException("keycloak down"))
                .doNothing()
                .when(keycloakAdminService).setEnabled(TestDataFactory.KEYCLOAK_ID, false);
        assertThatThrownBy(() -> service.deactivate(TestDataFactory.USER_ID)).isInstanceOf(IllegalStateException.class);
        verify(keycloakAdminService, times(2)).setEnabled(TestDataFactory.KEYCLOAK_ID, true);
    }

    @Test
    void resetPasswordGeneratesStrongPasswordAndQueuesNotification() {
        when(userRepository.findById(TestDataFactory.USER_ID)).thenReturn(Optional.of(user));

        PasswordResetResponse response = service.resetPassword(TestDataFactory.USER_ID);

        assertThat(response.email()).isEqualTo(user.getEmail());
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(keycloakAdminService).resetPassword(eq(TestDataFactory.KEYCLOAK_ID), passwordCaptor.capture(), eq(false));
        assertThat(passwordCaptor.getValue())
                .hasSize(18)
                .containsPattern("[A-Z]")
                .containsPattern("[a-z]")
                .containsPattern("\\d")
                .containsPattern("[@#$%&*!?]");
        verify(credentialNotificationService).sendResetPassword(user, passwordCaptor.getValue());
    }

    @Test
    void publicPasswordResetIsGenericForActiveInactiveAndUnknownUsers() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        PasswordResetAcceptedResponse active = service.requestPasswordReset(TestDataFactory.passwordResetRequest());
        assertThat(active.message()).contains("If the email is registered");
        verify(credentialNotificationService).sendResetPassword(eq(user), anyString());

        User inactive = TestDataFactory.user(UUID.randomUUID(), UUID.randomUUID(), "inactive@example.com", false);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(inactive));
        assertThat(service.requestPasswordReset(TestDataFactory.passwordResetRequest()).message()).contains("If the email is registered");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        assertThat(service.requestPasswordReset(TestDataFactory.passwordResetRequest()).message()).contains("If the email is registered");
    }

    @Test
    void changeMyPasswordVerifiesCurrentPasswordRevokesTokenAndUpdatesPassword() {
        when(currentUserService.requireLocalUser()).thenReturn(user);
        TokenResponse token = TestDataFactory.tokenResponse("verification-refresh");
        when(keycloakAdminService.login(user.getEmail(), "OldPass123")).thenReturn(token);

        assertThat(service.changeMyPassword(TestDataFactory.changePasswordRequest()).email()).isEqualTo(user.getEmail());

        verify(keycloakAdminService).logout("verification-refresh");
        verify(keycloakAdminService).resetPassword(TestDataFactory.KEYCLOAK_ID, "NewPass123", false);
    }

    @Test
    void changePasswordIgnoresMissingOrFailingVerificationTokenRevocation() {
        when(currentUserService.requireLocalUser()).thenReturn(user);
        when(keycloakAdminService.login(user.getEmail(), "OldPass123")).thenReturn(TestDataFactory.tokenResponse(" "));
        service.changeMyPassword(TestDataFactory.changePasswordRequest());

        when(keycloakAdminService.login(user.getEmail(), "OldPass123")).thenReturn(TestDataFactory.tokenResponse("refresh"));
        doThrow(new IllegalStateException("logout failed")).when(keycloakAdminService).logout("refresh");
        service.changeMyPassword(TestDataFactory.changePasswordRequest());
    }
}
