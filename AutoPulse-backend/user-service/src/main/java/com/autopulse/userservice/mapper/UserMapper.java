package com.autopulse.userservice.mapper;

import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.web.dto.UpdateMyUserRequest;
import com.autopulse.userservice.web.dto.UpdateUserRequest;
import com.autopulse.userservice.web.dto.UserResponse;

import java.util.Locale;
import java.util.UUID;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toNewUser(UUID keycloakUserId,
                                 String email,
                                 String firstName,
                                 String lastName,
                                 String phoneNumber) {
        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(normalizeEmail(email));
        user.setFirstName(normalizeText(firstName));
        user.setLastName(normalizeText(lastName));
        user.setPhoneNumber(normalizePhone(phoneNumber));
        user.setActive(Boolean.TRUE);
        return user;
    }

    public static void updateUser(User user,
                                  String email,
                                  String firstName,
                                  String lastName,
                                  String phoneNumber,
                                  boolean active) {
        updateBaseFields(user, email, firstName, lastName, phoneNumber);
        user.setActive(active);
    }

    public static void updateBaseFields(User user,
                                        String email,
                                        String firstName,
                                        String lastName,
                                        String phoneNumber) {
        user.setEmail(normalizeEmail(email));
        user.setFirstName(normalizeText(firstName));
        user.setLastName(normalizeText(lastName));
        user.setPhoneNumber(normalizePhone(phoneNumber));
    }

    public static void updateUser(User user, UpdateUserRequest request) {
        updateUser(user, request.email(), request.firstName(), request.lastName(), request.phoneNumber(), request.active());
    }

    public static void updateCurrentUser(User user, UpdateMyUserRequest request) {
        updateBaseFields(user, request.email(), request.firstName(), request.lastName(), request.phoneNumber());
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getKeycloakUserId(),
                user.getDispatcherProfile() == null ? null : user.getDispatcherProfile().getId(),
                user.getCourierProfile() == null ? null : user.getCourierProfile().getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                Boolean.TRUE.equals(user.getActive()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    public static String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.trim();
    }
}
