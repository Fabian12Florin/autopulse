package com.autopulse.userservice.web.controller;

import com.autopulse.common.api.ErrorResponse;
import com.autopulse.userservice.service.AuthService;
import com.autopulse.userservice.web.dto.LoginRequest;
import com.autopulse.userservice.web.dto.LogoutRequest;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.PasswordResetRequest;
import com.autopulse.userservice.web.dto.RefreshTokenRequest;
import com.autopulse.userservice.web.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/auth")
@Tag(name = "Authentication", description = "Public authentication operations backed by Keycloak: login, refresh, logout, and forgot-password reset.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate a user",
            description = "Validates the supplied email and password against Keycloak and returns a JWT access token, " +
                    "refresh token, expiry metadata, and token type.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid credentials\"}"))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Uses a valid refresh token to obtain a fresh access token from Keycloak.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout a session",
            description = "Revokes the supplied refresh token in Keycloak. This endpoint is intentionally public because the refresh token itself is the revocation credential.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout completed"),
            @ApiResponse(responseCode = "400", description = "Invalid logout request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Request password reset",
            description = "Starts the forgot-password flow using only an email address. If the email belongs to an active account, " +
                    "a newly generated password is set in Keycloak and sent through the notification pipeline. The response is intentionally generic to avoid account enumeration.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PasswordResetAcceptedResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(authService.requestPasswordReset(request));
    }
}
