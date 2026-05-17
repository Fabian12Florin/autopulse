package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.service.CredentialNotificationService;
import com.autopulse.userservice.service.CurrentUserService;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.CourierResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourierServiceImplTest {

    private final CourierProfileRepository courierProfileRepository = mock(CourierProfileRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final KeycloakAdminService keycloakAdminService = mock(KeycloakAdminService.class);
    private final CredentialNotificationService credentialNotificationService = mock(CredentialNotificationService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final KeycloakAdminProperties keycloakAdminProperties = TestDataFactory.keycloakProperties();
    private final CourierServiceImpl service = new CourierServiceImpl(
            courierProfileRepository,
            userRepository,
            keycloakAdminService,
            credentialNotificationService,
            currentUserService,
            keycloakAdminProperties
    );

    private CourierProfile courier;
    private DispatcherProfile dispatcher;

    @BeforeEach
    void setUp() {
        courier = TestDataFactory.courierProfile();
        dispatcher = TestDataFactory.dispatcherProfile();
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courierProfileRepository.saveAndFlush(any(CourierProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createCourierAsAdminPersistsIdentityProfileAndNotification() {
        when(keycloakAdminService.createUser(anyString(), anyString(), anyString(), anyString(), eq(true), eq("COURIER")))
                .thenReturn(TestDataFactory.KEYCLOAK_ID);

        CourierResponse response = service.create(TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, " ro-b "));

        assertThat(response.regionCode()).isEqualTo("RO-B");
        assertThat(response.availabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        verify(credentialNotificationService).sendInitialPassword(any(User.class), eq("COURIER"), anyString());
    }

    @Test
    void createRejectsDuplicateEmailInvalidInitialStatusAndDispatcherRegionMismatch() {
        when(userRepository.existsByEmailIgnoreCase("courier@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, "RO-B")))
                .isInstanceOf(ConflictException.class);

        when(userRepository.existsByEmailIgnoreCase("courier@example.com")).thenReturn(false);
        assertThatThrownBy(() -> service.create(TestDataFactory.createCourierRequest(AvailabilityStatus.ON_ROUTE, "RO-B")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(TestDataFactory.createCourierRequest(AvailabilityStatus.SUSPENDED, "RO-B")))
                .isInstanceOf(BadRequestException.class);

        when(currentUserService.hasRole("DISPATCHER")).thenReturn(true);
        when(currentUserService.requireCurrentDispatcherProfile()).thenReturn(dispatcher);
        assertThatThrownBy(() -> service.create(TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, "RO-C")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getByIdAppliesAdminDispatcherAndCourierReadAccess() {
        when(courierProfileRepository.findById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(Optional.of(courier));
        when(currentUserService.hasRole("ADMIN")).thenReturn(true);
        assertThat(service.getById(TestDataFactory.COURIER_PROFILE_ID).regionCode()).isEqualTo("RO-B");

        when(currentUserService.hasRole("ADMIN")).thenReturn(false);
        when(currentUserService.hasRole("DISPATCHER")).thenReturn(true);
        when(currentUserService.requireCurrentDispatcherProfile()).thenReturn(dispatcher);
        assertThat(service.getById(TestDataFactory.COURIER_PROFILE_ID).regionCode()).isEqualTo("RO-B");

        courier.setRegionCode("RO-C");
        assertThatThrownBy(() -> service.getById(TestDataFactory.COURIER_PROFILE_ID)).isInstanceOf(NotFoundException.class);

        when(currentUserService.hasRole("DISPATCHER")).thenReturn(false);
        when(currentUserService.hasRole("COURIER")).thenReturn(true);
        courier.setRegionCode("RO-B");
        when(currentUserService.requireCurrentCourierProfile()).thenReturn(courier);
        assertThat(service.getById(TestDataFactory.COURIER_PROFILE_ID).courierProfileId()).isEqualTo(TestDataFactory.COURIER_PROFILE_ID);

        CourierProfile other = TestDataFactory.courierProfile();
        org.springframework.test.util.ReflectionTestUtils.setField(other, "id", UUID.randomUUID());
        when(currentUserService.requireCurrentCourierProfile()).thenReturn(other);
        assertThatThrownBy(() -> service.getById(TestDataFactory.COURIER_PROFILE_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchRestrictsDispatcherRegionAndAvailableByDepotRequiresDepot() {
        when(courierProfileRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(courier)));

        assertThat(service.search(null, "ro-b", AvailabilityStatus.AVAILABLE, true, PageRequest.of(0, 10))).hasSize(1);

        when(currentUserService.hasRole("DISPATCHER")).thenReturn(true);
        when(currentUserService.requireCurrentDispatcherProfile()).thenReturn(dispatcher);
        assertThat(service.search(null, null, null, null, PageRequest.of(0, 10))).hasSize(1);
        assertThatThrownBy(() -> service.search(null, "RO-C", null, null, PageRequest.of(0, 10)))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.searchAvailableByDepot(null, PageRequest.of(0, 10)))
                .isInstanceOf(BadRequestException.class);
        assertThat(service.searchAvailableByDepot(TestDataFactory.DEPOT_ID, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void updateCourierAsAdminSynchronizesKeycloakAndProfile() {
        when(courierProfileRepository.findById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(Optional.of(courier));
        when(currentUserService.hasRole("ADMIN")).thenReturn(true);

        CourierResponse response = service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.OFF_DUTY, true, "ro-c")
        );

        assertThat(response.regionCode()).isEqualTo("RO-C");
        assertThat(response.user().email()).isEqualTo("updated-courier@example.com");
        verify(keycloakAdminService).updateUser(TestDataFactory.KEYCLOAK_ID, "updated-courier@example.com", "Alan", "Turing", true);
    }

    @Test
    void updateRejectsInvalidAvailabilityAndDeactivationStates() {
        when(courierProfileRepository.findById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(Optional.of(courier));
        when(currentUserService.hasRole("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.ON_ROUTE, true, "RO-B")
        )).isInstanceOf(BadRequestException.class);

        courier.setAvailabilityStatus(AvailabilityStatus.ON_ROUTE);
        assertThatThrownBy(() -> service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.AVAILABLE, true, "RO-B")
        )).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.AVAILABLE, false, "RO-B")
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void dispatcherCanManageOnlyOwnRegion() {
        when(courierProfileRepository.findById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(Optional.of(courier));
        when(currentUserService.hasRole("DISPATCHER")).thenReturn(true);
        when(currentUserService.requireCurrentDispatcherProfile()).thenReturn(dispatcher);

        assertThat(service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.OFF_DUTY, true, "RO-B")
        ).regionCode()).isEqualTo("RO-B");

        assertThatThrownBy(() -> service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.OFF_DUTY, true, "RO-C")
        )).isInstanceOf(BadRequestException.class);

        courier.setRegionCode("RO-C");
        assertThatThrownBy(() -> service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.OFF_DUTY, true, "RO-B")
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateAvailabilityHandlesCourierSelfServiceAndManagerRules() {
        when(courierProfileRepository.findById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(Optional.of(courier));
        when(currentUserService.hasRole("COURIER")).thenReturn(true);
        when(currentUserService.requireCurrentCourierProfile()).thenReturn(courier);

        assertThat(service.updateAvailability(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.availabilityRequest(AvailabilityStatus.OFF_DUTY)
        ).availabilityStatus()).isEqualTo(AvailabilityStatus.OFF_DUTY);

        assertThatThrownBy(() -> service.updateAvailability(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.availabilityRequest(AvailabilityStatus.SUSPENDED)
        )).isInstanceOf(BadRequestException.class);

        CourierProfile other = TestDataFactory.courierProfile();
        org.springframework.test.util.ReflectionTestUtils.setField(other, "id", UUID.randomUUID());
        when(currentUserService.requireCurrentCourierProfile()).thenReturn(other);
        assertThatThrownBy(() -> service.updateAvailability(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.availabilityRequest(AvailabilityStatus.AVAILABLE)
        )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateMyAvailabilityRejectsSuspendedOrOnRouteCurrentStates() {
        when(currentUserService.requireCurrentCourierProfile()).thenReturn(courier);

        assertThat(service.updateMyAvailability(TestDataFactory.availabilityRequest(AvailabilityStatus.OFF_DUTY)).availabilityStatus())
                .isEqualTo(AvailabilityStatus.OFF_DUTY);

        courier.setAvailabilityStatus(AvailabilityStatus.SUSPENDED);
        assertThatThrownBy(() -> service.updateMyAvailability(TestDataFactory.availabilityRequest(AvailabilityStatus.AVAILABLE)))
                .isInstanceOf(BadRequestException.class);

        courier.setAvailabilityStatus(AvailabilityStatus.ON_ROUTE);
        assertThatThrownBy(() -> service.updateMyAvailability(TestDataFactory.availabilityRequest(AvailabilityStatus.AVAILABLE)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void nonAdminNonDispatcherCannotManageCourierData() {
        when(courierProfileRepository.findById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(Optional.of(courier));

        assertThatThrownBy(() -> service.update(
                TestDataFactory.COURIER_PROFILE_ID,
                TestDataFactory.updateCourierRequest(AvailabilityStatus.AVAILABLE, true, "RO-B")
        )).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getById(TestDataFactory.COURIER_PROFILE_ID)).isInstanceOf(BadRequestException.class);
    }
}
