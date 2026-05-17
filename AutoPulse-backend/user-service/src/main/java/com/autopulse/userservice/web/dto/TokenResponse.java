package com.autopulse.userservice.web.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        long refreshExpiresIn,
        String tokenType,
        String scope,
        String sessionState
) {
}
