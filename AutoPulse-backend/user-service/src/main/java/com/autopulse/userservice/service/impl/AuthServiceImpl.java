package com.autopulse.userservice.service.impl;

import com.autopulse.userservice.service.AuthService;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.web.dto.LoginRequest;
import com.autopulse.userservice.web.dto.LogoutRequest;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.PasswordResetRequest;
import com.autopulse.userservice.web.dto.RefreshTokenRequest;
import com.autopulse.userservice.web.dto.TokenResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final KeycloakAdminService keycloakAdminService;
    private final UserService userService;

    public AuthServiceImpl(KeycloakAdminService keycloakAdminService,
                           UserService userService) {
        this.keycloakAdminService = keycloakAdminService;
        this.userService = userService;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        return keycloakAdminService.login(request.email().trim().toLowerCase(), request.password());
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        return keycloakAdminService.refresh(request.refreshToken());
    }

    @Override
    public void logout(LogoutRequest request) {
        keycloakAdminService.logout(request.refreshToken());
    }

    @Override
    public PasswordResetAcceptedResponse requestPasswordReset(PasswordResetRequest request) {
        return userService.requestPasswordReset(request);
    }
}
