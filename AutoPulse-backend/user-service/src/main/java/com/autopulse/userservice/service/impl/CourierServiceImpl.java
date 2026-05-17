package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.mapper.CourierProfileMapper;
import com.autopulse.userservice.mapper.DispatcherProfileMapper;
import com.autopulse.userservice.mapper.UserMapper;
import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.repository.specification.CourierProfileSpecifications;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.CredentialNotificationService;
import com.autopulse.userservice.service.CurrentUserService;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.CreateCourierRequest;
import com.autopulse.userservice.web.dto.UpdateCourierAvailabilityRequest;
import com.autopulse.userservice.web.dto.UpdateCourierRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CourierServiceImpl implements CourierService {

    private final CourierProfileRepository courierProfileRepository;
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final CredentialNotificationService credentialNotificationService;
    private final CurrentUserService currentUserService;
    private final KeycloakAdminProperties keycloakAdminProperties;

    public CourierServiceImpl(CourierProfileRepository courierProfileRepository,
                              UserRepository userRepository,
                              KeycloakAdminService keycloakAdminService,
                              CredentialNotificationService credentialNotificationService,
                              CurrentUserService currentUserService,
                              KeycloakAdminProperties keycloakAdminProperties) {
        this.courierProfileRepository = courierProfileRepository;
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.credentialNotificationService = credentialNotificationService;
        this.currentUserService = currentUserService;
        this.keycloakAdminProperties = keycloakAdminProperties;
    }

    @Override
    @Transactional
    public CourierResponse create(CreateCourierRequest request) {
        String normalizedEmail = UserMapper.normalizeEmail(request.email());
        ensureEmailIsUnique(normalizedEmail, null);
        validateInitialAvailability(request.availabilityStatus());
        enforceDispatcherRegionOnCreate(request.regionCode());

        String generatedPassword = UUID.randomUUID().toString();
        UUID keycloakUserId = null;
        try {
            keycloakUserId = keycloakAdminService.createUser(
                    normalizedEmail,
                    UserMapper.normalizeText(request.firstName()),
                    UserMapper.normalizeText(request.lastName()),
                    generatedPassword,
                    true,
                    keycloakAdminProperties.getRoles().getCourier()
            );

            User user = UserMapper.toNewUser(
                    keycloakUserId,
                    normalizedEmail,
                    request.firstName(),
                    request.lastName(),
                    request.phoneNumber()
            );
            user = userRepository.saveAndFlush(user);

            CourierProfile courierProfile = CourierProfileMapper.toNewEntity(user, request);
            courierProfile = courierProfileRepository.saveAndFlush(courierProfile);

            credentialNotificationService.sendInitialPassword(user, keycloakAdminProperties.getRoles().getCourier(), generatedPassword);
            return CourierProfileMapper.toResponse(courierProfile);
        } catch (RuntimeException ex) {
            cleanupCreatedKeycloakUser(keycloakUserId);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CourierResponse getById(UUID courierProfileId) {
        CourierProfile profile = getCourierProfile(courierProfileId);
        enforceCourierReadAccess(profile);
        return CourierProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierResponse> search(UUID depotId,
                                        String regionCode,
                                        AvailabilityStatus availabilityStatus,
                                        Boolean active,
                                        Pageable pageable) {
        String effectiveRegionCode = normalizeRegionFilterForCaller(regionCode);
        return courierProfileRepository
                .findAll(CourierProfileSpecifications.withFilters(depotId, effectiveRegionCode, availabilityStatus, active), pageable)
                .map(CourierProfileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierResponse> searchAvailableByDepot(UUID depotId, Pageable pageable) {
        if (depotId == null) {
            throw new BadRequestException("Depot id must not be null");
        }

        String regionCode = null;
        if (currentUserService.hasRole(keycloakAdminProperties.getRoles().getDispatcher())) {
            regionCode = currentUserService.requireCurrentDispatcherProfile().getRegionCode();
        }

        return courierProfileRepository
                .findAll(CourierProfileSpecifications.availableForDepot(depotId, regionCode), pageable)
                .map(CourierProfileMapper::toResponse);
    }

    @Override
    @Transactional
    public CourierResponse update(UUID courierProfileId, UpdateCourierRequest request) {
        CourierProfile profile = getCourierProfile(courierProfileId);
        enforceCourierManagementAccess(profile, request.regionCode());
        validateManualAvailabilityChange(profile.getAvailabilityStatus(), request.availabilityStatus());
        if (!request.active() && profile.getAvailabilityStatus() == AvailabilityStatus.ON_ROUTE) {
            throw new BadRequestException("Courier cannot be deactivated while currently on route");
        }

        User user = profile.getUser();
        String normalizedEmail = UserMapper.normalizeEmail(request.email());
        ensureEmailIsUnique(normalizedEmail, user.getId());

        UserSnapshot snapshot = UserSnapshot.from(user);

        try {
            keycloakAdminService.updateUser(
                    user.getKeycloakUserId(),
                    normalizedEmail,
                    UserMapper.normalizeText(request.firstName()),
                    UserMapper.normalizeText(request.lastName()),
                    request.active()
            );

            UserMapper.updateUser(
                    user,
                    normalizedEmail,
                    request.firstName(),
                    request.lastName(),
                    request.phoneNumber(),
                    request.active()
            );
            CourierProfileMapper.updateEntity(profile, request);

            CourierProfile savedProfile = courierProfileRepository.saveAndFlush(profile);
            return CourierProfileMapper.toResponse(savedProfile);
        } catch (RuntimeException ex) {
            compensateKeycloakUpdate(snapshot, user.getKeycloakUserId());
            throw ex;
        }
    }

    @Override
    @Transactional
    public CourierResponse updateAvailability(UUID courierProfileId, UpdateCourierAvailabilityRequest request) {
        CourierProfile profile = getCourierProfile(courierProfileId);

        if (currentUserService.hasRole(keycloakAdminProperties.getRoles().getCourier())) {
            CourierProfile currentCourier = currentUserService.requireCurrentCourierProfile();
            if (!currentCourier.getId().equals(courierProfileId)) {
                throw new NotFoundException("Courier profile was not found");
            }
            validateSelfAvailabilityChange(profile.getAvailabilityStatus(), request.availabilityStatus());
        } else {
            enforceCourierManagementAccess(profile, profile.getRegionCode());
            validateManualAvailabilityChange(profile.getAvailabilityStatus(), request.availabilityStatus());
        }

        CourierProfileMapper.updateAvailability(profile, request);
        CourierProfile savedProfile = courierProfileRepository.saveAndFlush(profile);
        return CourierProfileMapper.toResponse(savedProfile);
    }

    @Override
    @Transactional
    public CourierResponse updateMyAvailability(UpdateCourierAvailabilityRequest request) {
        CourierProfile currentCourier = currentUserService.requireCurrentCourierProfile();
        validateSelfAvailabilityChange(currentCourier.getAvailabilityStatus(), request.availabilityStatus());
        CourierProfileMapper.updateAvailability(currentCourier, request);
        CourierProfile savedProfile = courierProfileRepository.saveAndFlush(currentCourier);
        return CourierProfileMapper.toResponse(savedProfile);
    }

    private CourierProfile getCourierProfile(UUID courierProfileId) {
        return courierProfileRepository.findById(courierProfileId)
                .orElseThrow(() -> new NotFoundException("Courier profile was not found"));
    }

    private void ensureEmailIsUnique(String email, UUID currentUserId) {
        boolean exists = currentUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentUserId);
        if (exists) {
            throw new ConflictException("Email address is already used by another user");
        }
    }

    private void validateInitialAvailability(AvailabilityStatus availabilityStatus) {
        if (availabilityStatus == AvailabilityStatus.ON_ROUTE || availabilityStatus == AvailabilityStatus.SUSPENDED) {
            throw new BadRequestException("Courier cannot be created directly in ON_ROUTE or SUSPENDED state");
        }
    }

    private void validateSelfAvailabilityChange(AvailabilityStatus currentStatus, AvailabilityStatus targetStatus) {
        if (targetStatus == AvailabilityStatus.SUSPENDED || targetStatus == AvailabilityStatus.ON_ROUTE) {
            throw new BadRequestException("Couriers cannot manually switch to SUSPENDED or ON_ROUTE");
        }
        if (currentStatus == AvailabilityStatus.SUSPENDED || currentStatus == AvailabilityStatus.ON_ROUTE) {
            throw new BadRequestException("Availability cannot be changed manually from the current state");
        }
    }

    private void validateManualAvailabilityChange(AvailabilityStatus currentStatus, AvailabilityStatus targetStatus) {
        if (targetStatus == AvailabilityStatus.ON_ROUTE) {
            throw new BadRequestException("ON_ROUTE can only be set by delivery execution events");
        }
        if (currentStatus == AvailabilityStatus.ON_ROUTE) {
            throw new BadRequestException("Courier availability cannot be changed manually while the courier is ON_ROUTE");
        }
    }

    private void enforceDispatcherRegionOnCreate(String requestedRegionCode) {
        if (!currentUserService.hasRole(keycloakAdminProperties.getRoles().getDispatcher())) {
            return;
        }

        DispatcherProfile dispatcherProfile = currentUserService.requireCurrentDispatcherProfile();
        String dispatcherRegion = dispatcherProfile.getRegionCode();
        String requestedRegion = DispatcherProfileMapper.normalizeRegionCode(requestedRegionCode);
        if (!dispatcherRegion.equalsIgnoreCase(requestedRegion)) {
            throw new BadRequestException("Dispatcher can only create couriers inside the assigned region");
        }
    }

    private String normalizeRegionFilterForCaller(String requestedRegionCode) {
        if (!currentUserService.hasRole(keycloakAdminProperties.getRoles().getDispatcher())) {
            return requestedRegionCode;
        }

        String dispatcherRegion = currentUserService.requireCurrentDispatcherProfile().getRegionCode();
        if (requestedRegionCode != null && !requestedRegionCode.isBlank()) {
            String normalizedRequestedRegion = DispatcherProfileMapper.normalizeRegionCode(requestedRegionCode);
            if (!dispatcherRegion.equalsIgnoreCase(normalizedRequestedRegion)) {
                throw new BadRequestException("Dispatcher can only search couriers inside the assigned region");
            }
        }
        return dispatcherRegion;
    }

    private void enforceCourierReadAccess(CourierProfile profile) {
        if (currentUserService.hasRole(keycloakAdminProperties.getRoles().getAdmin())) {
            return;
        }

        if (currentUserService.hasRole(keycloakAdminProperties.getRoles().getDispatcher())) {
            String dispatcherRegion = currentUserService.requireCurrentDispatcherProfile().getRegionCode();
            if (!dispatcherRegion.equalsIgnoreCase(profile.getRegionCode())) {
                throw new NotFoundException("Courier profile was not found");
            }
            return;
        }

        if (currentUserService.hasRole(keycloakAdminProperties.getRoles().getCourier())) {
            CourierProfile currentCourier = currentUserService.requireCurrentCourierProfile();
            if (!currentCourier.getId().equals(profile.getId())) {
                throw new NotFoundException("Courier profile was not found");
            }
            return;
        }

        throw new BadRequestException("Caller is not allowed to access courier data");
    }

    private void enforceCourierManagementAccess(CourierProfile profile, String targetRegionCode) {
        if (currentUserService.hasRole(keycloakAdminProperties.getRoles().getAdmin())) {
            return;
        }

        if (currentUserService.hasRole(keycloakAdminProperties.getRoles().getDispatcher())) {
            String dispatcherRegion = currentUserService.requireCurrentDispatcherProfile().getRegionCode();
            String normalizedTargetRegion = DispatcherProfileMapper.normalizeRegionCode(targetRegionCode);
            if (!dispatcherRegion.equalsIgnoreCase(profile.getRegionCode()) || !dispatcherRegion.equalsIgnoreCase(normalizedTargetRegion)) {
                throw new BadRequestException("Dispatcher can only manage couriers in the assigned region");
            }
            return;
        }

        throw new BadRequestException("Caller is not allowed to manage courier data");
    }

    private void cleanupCreatedKeycloakUser(UUID keycloakUserId) {
        if (keycloakUserId == null) {
            return;
        }
        try {
            keycloakAdminService.deleteUser(keycloakUserId);
        } catch (RuntimeException cleanupEx) {
            throw new DataIntegrityViolationException("Courier creation failed and Keycloak cleanup also failed", cleanupEx);
        }
    }

    private void compensateKeycloakUpdate(UserSnapshot userSnapshot, UUID keycloakUserId) {
        try {
            keycloakAdminService.updateUser(
                    keycloakUserId,
                    userSnapshot.email(),
                    userSnapshot.firstName(),
                    userSnapshot.lastName(),
                    userSnapshot.active()
            );
        } catch (RuntimeException ignored) {

        }
    }

    private record UserSnapshot(String email, String firstName, String lastName, boolean active) {
        private static UserSnapshot from(User user) {
            return new UserSnapshot(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    Boolean.TRUE.equals(user.getActive())
            );
        }
    }
}
