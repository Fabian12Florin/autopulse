package com.autopulse.userservice.service;

import com.autopulse.userservice.web.dto.LoginRequest;
import com.autopulse.userservice.web.dto.LogoutRequest;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.PasswordResetRequest;
import com.autopulse.userservice.web.dto.RefreshTokenRequest;
import com.autopulse.userservice.web.dto.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    PasswordResetAcceptedResponse requestPasswordReset(PasswordResetRequest request);
}
