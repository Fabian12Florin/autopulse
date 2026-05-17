package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.mapper.DispatcherProfileMapper;
import com.autopulse.userservice.mapper.UserMapper;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.repository.DispatcherProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.repository.specification.DispatcherProfileSpecifications;
import com.autopulse.userservice.service.CredentialNotificationService;
import com.autopulse.userservice.service.DispatcherService;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.web.dto.CreateDispatcherRequest;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.UpdateDispatcherRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DispatcherServiceImpl implements DispatcherService {

    private final DispatcherProfileRepository dispatcherProfileRepository;
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final CredentialNotificationService credentialNotificationService;
    private final KeycloakAdminProperties keycloakAdminProperties;

    public DispatcherServiceImpl(DispatcherProfileRepository dispatcherProfileRepository,
                                 UserRepository userRepository,
                                 KeycloakAdminService keycloakAdminService,
                                 CredentialNotificationService credentialNotificationService,
                                 KeycloakAdminProperties keycloakAdminProperties) {
        this.dispatcherProfileRepository = dispatcherProfileRepository;
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.credentialNotificationService = credentialNotificationService;
        this.keycloakAdminProperties = keycloakAdminProperties;
    }

    @Override
    @Transactional
    public DispatcherResponse create(CreateDispatcherRequest request) {
        String normalizedEmail = UserMapper.normalizeEmail(request.email());
        ensureEmailIsUnique(normalizedEmail, null);

        String generatedPassword = UUID.randomUUID().toString();
        UUID keycloakUserId = null;
        try {
            keycloakUserId = keycloakAdminService.createUser(
                    normalizedEmail,
                    UserMapper.normalizeText(request.firstName()),
                    UserMapper.normalizeText(request.lastName()),
                    generatedPassword,
                    true,
                    keycloakAdminProperties.getRoles().getDispatcher()
            );

            User user = UserMapper.toNewUser(
                    keycloakUserId,
                    normalizedEmail,
                    request.firstName(),
                    request.lastName(),
                    request.phoneNumber()
            );
            user = userRepository.saveAndFlush(user);

            DispatcherProfile profile = DispatcherProfileMapper.toNewEntity(user, request);
            profile = dispatcherProfileRepository.saveAndFlush(profile);

            credentialNotificationService.sendInitialPassword(user, keycloakAdminProperties.getRoles().getDispatcher(), generatedPassword);
            return DispatcherProfileMapper.toResponse(profile);
        } catch (RuntimeException ex) {
            cleanupCreatedKeycloakUser(keycloakUserId);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DispatcherResponse getById(UUID dispatcherProfileId) {
        return DispatcherProfileMapper.toResponse(getDispatcherProfile(dispatcherProfileId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DispatcherResponse> search(String regionCode, Boolean active, Pageable pageable) {
        return dispatcherProfileRepository
                .findAll(DispatcherProfileSpecifications.withFilters(regionCode, active), pageable)
                .map(DispatcherProfileMapper::toResponse);
    }

    @Override
    @Transactional
    public DispatcherResponse update(UUID dispatcherProfileId, UpdateDispatcherRequest request) {
        DispatcherProfile dispatcherProfile = getDispatcherProfile(dispatcherProfileId);
        User user = dispatcherProfile.getUser();

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
            DispatcherProfileMapper.updateEntity(dispatcherProfile, request);

            DispatcherProfile savedProfile = dispatcherProfileRepository.saveAndFlush(dispatcherProfile);
            return DispatcherProfileMapper.toResponse(savedProfile);
        } catch (RuntimeException ex) {
            compensateKeycloakUpdate(snapshot, user.getKeycloakUserId());
            throw ex;
        }
    }

    private DispatcherProfile getDispatcherProfile(UUID dispatcherProfileId) {
        return dispatcherProfileRepository.findById(dispatcherProfileId)
                .orElseThrow(() -> new NotFoundException("Dispatcher profile was not found"));
    }

    private void ensureEmailIsUnique(String email, UUID currentUserId) {
        boolean exists = currentUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentUserId);
        if (exists) {
            throw new ConflictException("Email address is already used by another user");
        }
    }

    private void cleanupCreatedKeycloakUser(UUID keycloakUserId) {
        if (keycloakUserId == null) {
            return;
        }
        try {
            keycloakAdminService.deleteUser(keycloakUserId);
        } catch (RuntimeException cleanupEx) {
            throw new DataIntegrityViolationException("Dispatcher creation failed and Keycloak cleanup also failed", cleanupEx);
        }
    }

    private void compensateKeycloakUpdate(UserSnapshot snapshot, UUID keycloakUserId) {
        try {
            keycloakAdminService.updateUser(
                    keycloakUserId,
                    snapshot.email(),
                    snapshot.firstName(),
                    snapshot.lastName(),
                    snapshot.active()
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
