package com.autopulse.userservice.service;

import com.autopulse.userservice.web.dto.TokenResponse;

import java.util.UUID;

public interface KeycloakAdminService {

    UUID createUser(String email,
                    String firstName,
                    String lastName,
                    String rawPassword,
                    boolean enabled,
                    String realmRoleName);

    void updateUser(UUID keycloakUserId,
                    String email,
                    String firstName,
                    String lastName,
                    boolean enabled);

    void setEnabled(UUID keycloakUserId, boolean enabled);

    void resetPassword(UUID keycloakUserId, String rawPassword, boolean temporary);

    void deleteUser(UUID keycloakUserId);

    TokenResponse login(String username, String password);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
