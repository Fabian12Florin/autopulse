package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.mapper.UserMapper;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.UserRepository;
import com.autopulse.userservice.repository.specification.UserSpecifications;
import com.autopulse.userservice.service.CredentialNotificationService;
import com.autopulse.userservice.service.CurrentUserService;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.web.dto.ChangePasswordRequest;
import com.autopulse.userservice.web.dto.PasswordChangeResponse;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.PasswordResetRequest;
import com.autopulse.userservice.web.dto.PasswordResetResponse;
import com.autopulse.userservice.web.dto.TokenResponse;
import com.autopulse.userservice.web.dto.UpdateMyUserRequest;
import com.autopulse.userservice.web.dto.UpdateUserRequest;
import com.autopulse.userservice.web.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String PASSWORD_RESET_ACCEPTED_MESSAGE =
            "If the email is registered, a password reset message will be sent";
    private static final String PASSWORD_RESET_QUEUED_MESSAGE = "Password reset was queued for delivery";
    private static final String PASSWORD_CHANGED_MESSAGE = "Password was changed successfully";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%&*!?";
    private static final String ALL_PASSWORD_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final int GENERATED_PASSWORD_LENGTH = 18;

    private final UserRepository userRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final CredentialNotificationService credentialNotificationService;
    private final CurrentUserService currentUserService;

    public UserServiceImpl(UserRepository userRepository,
                           CourierProfileRepository courierProfileRepository,
                           KeycloakAdminService keycloakAdminService,
                           CredentialNotificationService credentialNotificationService,
                           CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.courierProfileRepository = courierProfileRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.credentialNotificationService = credentialNotificationService;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        return UserMapper.toResponse(getUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return UserMapper.toResponse(currentUserService.requireLocalUser());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(String email,
                                     String firstName,
                                     String lastName,
                                     String phoneNumber,
                                     Boolean active,
                                     Pageable pageable) {
        return userRepository.findAll(
                        UserSpecifications.withFilters(email, firstName, lastName, phoneNumber, active),
                        pageable
                )
                .map(UserMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        User user = getUser(userId);
        String normalizedEmail = UserMapper.normalizeEmail(request.email());
        ensureEmailIsUnique(normalizedEmail, user.getId());

        if (!request.active()) {
            ensureUserCanBeDeactivated(user);
        }

        UserSnapshot snapshot = UserSnapshot.from(user);
        try {
            keycloakAdminService.updateUser(
                    user.getKeycloakUserId(),
                    normalizedEmail,
                    UserMapper.normalizeText(request.firstName()),
                    UserMapper.normalizeText(request.lastName()),
                    request.active()
            );

            UserMapper.updateUser(user, request);
            User savedUser = userRepository.saveAndFlush(user);
            return UserMapper.toResponse(savedUser);
        } catch (RuntimeException ex) {
            compensateKeycloakUpdate(snapshot, user.getKeycloakUserId());
            throw ex;
        }
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UpdateMyUserRequest request) {
        User user = currentUserService.requireLocalUser();
        String normalizedEmail = UserMapper.normalizeEmail(request.email());
        ensureEmailIsUnique(normalizedEmail, user.getId());

        UserSnapshot snapshot = UserSnapshot.from(user);
        try {
            keycloakAdminService.updateUser(
                    user.getKeycloakUserId(),
                    normalizedEmail,
                    UserMapper.normalizeText(request.firstName()),
                    UserMapper.normalizeText(request.lastName()),
                    Boolean.TRUE.equals(user.getActive())
            );

            UserMapper.updateCurrentUser(user, request);
            User savedUser = userRepository.saveAndFlush(user);
            return UserMapper.toResponse(savedUser);
        } catch (RuntimeException ex) {
            compensateKeycloakUpdate(snapshot, user.getKeycloakUserId());
            throw ex;
        }
    }

    @Override
    @Transactional
    public UserResponse activate(UUID userId) {
        User user = getUser(userId);
        Boolean previousActive = user.getActive();

        try {
            keycloakAdminService.setEnabled(user.getKeycloakUserId(), true);
            user.setActive(Boolean.TRUE);
            User savedUser = userRepository.saveAndFlush(user);
            return UserMapper.toResponse(savedUser);
        } catch (RuntimeException ex) {
            safelyRestoreEnabledState(user.getKeycloakUserId(), previousActive);
            throw ex;
        }
    }

    @Override
    @Transactional
    public UserResponse deactivate(UUID userId) {
        User user = getUser(userId);
        ensureUserCanBeDeactivated(user);
        Boolean previousActive = user.getActive();

        try {
            keycloakAdminService.setEnabled(user.getKeycloakUserId(), false);
            user.setActive(Boolean.FALSE);
            User savedUser = userRepository.saveAndFlush(user);
            return UserMapper.toResponse(savedUser);
        } catch (RuntimeException ex) {
            safelyRestoreEnabledState(user.getKeycloakUserId(), previousActive);
            throw ex;
        }
    }

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(UUID userId) {
        User user = getUser(userId);
        String password = generateStrongPassword();
        keycloakAdminService.resetPassword(user.getKeycloakUserId(), password, false);
        credentialNotificationService.sendResetPassword(user, password);
        return new PasswordResetResponse(user.getId(), user.getEmail(), PASSWORD_RESET_QUEUED_MESSAGE);
    }

    @Override
    @Transactional
    public PasswordResetAcceptedResponse requestPasswordReset(PasswordResetRequest request) {
        String normalizedEmail = UserMapper.normalizeEmail(request.email());

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(user -> {
            if (!Boolean.TRUE.equals(user.getActive())) {
                log.info("Ignoring public password reset request for inactive user email={}", normalizedEmail);
                return;
            }

            String password = generateStrongPassword();
            keycloakAdminService.resetPassword(user.getKeycloakUserId(), password, false);
            credentialNotificationService.sendResetPassword(user, password);
        }, () -> log.info("Ignoring public password reset request for unknown email={}", normalizedEmail));

        return new PasswordResetAcceptedResponse(PASSWORD_RESET_ACCEPTED_MESSAGE);
    }

    @Override
    @Transactional
    public PasswordChangeResponse changeMyPassword(ChangePasswordRequest request) {
        User user = currentUserService.requireLocalUser();

        TokenResponse verificationToken = keycloakAdminService.login(user.getEmail(), request.currentPassword());
        safelyRevokeVerificationRefreshToken(verificationToken);

        keycloakAdminService.resetPassword(user.getKeycloakUserId(), request.newPassword(), false);
        return new PasswordChangeResponse(user.getId(), user.getEmail(), PASSWORD_CHANGED_MESSAGE);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found"));
    }

    private void ensureEmailIsUnique(String email, UUID currentUserId) {
        boolean exists = currentUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentUserId);
        if (exists) {
            throw new ConflictException("Email address is already used by another user");
        }
    }

    private void ensureUserCanBeDeactivated(User user) {
        courierProfileRepository.findByUserId(user.getId()).ifPresent(courierProfile -> {
            if (courierProfile.getAvailabilityStatus() == AvailabilityStatus.ON_ROUTE) {
                throw new BadRequestException("Courier cannot be deactivated while currently on route");
            }
        });
    }

    private void safelyRestoreEnabledState(UUID keycloakUserId, Boolean active) {
        try {
            keycloakAdminService.setEnabled(keycloakUserId, Boolean.TRUE.equals(active));
        } catch (RuntimeException ignored) {

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

    private void safelyRevokeVerificationRefreshToken(TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.refreshToken() == null || tokenResponse.refreshToken().isBlank()) {
            return;
        }

        try {
            keycloakAdminService.logout(tokenResponse.refreshToken());
        } catch (RuntimeException ex) {
            log.warn("Could not revoke refresh token obtained during password-change verification", ex);
        }
    }

    private String generateStrongPassword() {
        List<Character> characters = new ArrayList<>(GENERATED_PASSWORD_LENGTH);
        characters.add(randomCharacterFrom(UPPERCASE));
        characters.add(randomCharacterFrom(LOWERCASE));
        characters.add(randomCharacterFrom(DIGITS));
        characters.add(randomCharacterFrom(SPECIAL));

        while (characters.size() < GENERATED_PASSWORD_LENGTH) {
            characters.add(randomCharacterFrom(ALL_PASSWORD_CHARACTERS));
        }

        Collections.shuffle(characters, SECURE_RANDOM);

        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (Character character : characters) {
            password.append(character);
        }
        return password.toString();
    }

    private char randomCharacterFrom(String allowedCharacters) {
        return allowedCharacters.charAt(SECURE_RANDOM.nextInt(allowedCharacters.length()));
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
