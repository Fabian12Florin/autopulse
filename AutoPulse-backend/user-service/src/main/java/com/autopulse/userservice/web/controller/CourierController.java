package com.autopulse.userservice.web.controller;

import com.autopulse.common.api.ErrorResponse;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.web.dto.ChangePasswordRequest;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.PasswordChangeResponse;
import com.autopulse.userservice.web.dto.UpdateCourierAvailabilityRequest;
import com.autopulse.userservice.web.dto.UpdateMyUserRequest;
import com.autopulse.userservice.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Current User / Courier Self-Service", description = "Endpoints for the authenticated principal to inspect profile data, change password, and manage courier availability.")
@SecurityRequirement(name = "bearer-jwt")
public class CourierController {

    private final UserService userService;
    private final CourierService courierService;

    public CourierController(UserService userService,
                             CourierService courierService) {
        this.userService = userService;
        this.courierService = courierService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user", description = "Returns the current business user resolved from the JWT subject / Keycloak user id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user resolved"),
            @ApiResponse(responseCode = "401", description = "Caller not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PatchMapping
    @Operation(summary = "Update my base user data", description = "Updates the authenticated user's email, first name, last name, and phone number both locally and in Keycloak.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateMyUserRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change my password", description = "Changes the authenticated user's password after verifying the current password against Keycloak. Use this when the user knows their current password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Invalid request or invalid current password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Caller not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PasswordChangeResponse> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changeMyPassword(request));
    }

    @PatchMapping("/courier/availability")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update my courier availability", description = "Allows a courier to switch operational availability where the requested transition is valid. Invalid transitions must be rejected by business rules.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability updated"),
            @ApiResponse(responseCode = "400", description = "Invalid transition or request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Courier profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourierResponse> updateMyCourierAvailability(@Valid @RequestBody UpdateCourierAvailabilityRequest request) {
        return ResponseEntity.ok(courierService.updateMyAvailability(request));
    }
}
