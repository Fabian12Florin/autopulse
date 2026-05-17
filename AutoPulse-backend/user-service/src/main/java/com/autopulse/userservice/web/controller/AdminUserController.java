package com.autopulse.userservice.web.controller;

import com.autopulse.common.api.ErrorResponse;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.DispatcherService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.CreateCourierRequest;
import com.autopulse.userservice.web.dto.CreateDispatcherRequest;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.PasswordResetResponse;
import com.autopulse.userservice.web.dto.UpdateCourierRequest;
import com.autopulse.userservice.web.dto.UpdateDispatcherRequest;
import com.autopulse.userservice.web.dto.UpdateUserRequest;
import com.autopulse.userservice.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Admin-only operations for user lifecycle, dispatcher management, and courier management.")
@SecurityRequirement(name = "bearer-jwt")
public class AdminUserController {

    private final UserService userService;
    private final DispatcherService dispatcherService;
    private final CourierService courierService;

    public AdminUserController(UserService userService,
                               DispatcherService dispatcherService,
                               CourierService courierService) {
        this.userService = userService;
        this.dispatcherService = dispatcherService;
        this.courierService = courierService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by internal id", description = "Returns the business user aggregate stored in user-service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Update base user data", description = "Updates identity-related business fields stored locally and synchronizes mutable identity attributes in Keycloak.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(userId, request));
    }

    @PostMapping("/dispatchers")
    @Operation(summary = "Create dispatcher", description = "Creates a Keycloak identity, assigns the DISPATCHER role, persists the local user and dispatcher profile, and publishes the initial password notification through Kafka.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dispatcher created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate email or profile conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DispatcherResponse> createDispatcher(@Valid @RequestBody CreateDispatcherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dispatcherService.create(request));
    }

    @PatchMapping("/dispatchers/{dispatcherProfileId}")
    @Operation(summary = "Update dispatcher profile", description = "Updates dispatcher business data and synchronizes mutable identity fields with Keycloak when required.")
    public ResponseEntity<DispatcherResponse> updateDispatcher(@PathVariable UUID dispatcherProfileId,
                                                               @Valid @RequestBody UpdateDispatcherRequest request) {
        return ResponseEntity.ok(dispatcherService.update(dispatcherProfileId, request));
    }

    @PostMapping("/couriers")
    @Operation(summary = "Create courier", description = "Creates a Keycloak identity, assigns the COURIER role, persists the local user and courier profile, and publishes the generated initial password through Kafka.")
    public ResponseEntity<CourierResponse> createCourier(@Valid @RequestBody CreateCourierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courierService.create(request));
    }

    @PatchMapping("/couriers/{courierProfileId}")
    @Operation(summary = "Update courier profile", description = "Updates courier profile data, subject to business rules such as region access and operational state validation.")
    public ResponseEntity<CourierResponse> updateCourier(@PathVariable UUID courierProfileId,
                                                         @Valid @RequestBody UpdateCourierRequest request) {
        return ResponseEntity.ok(courierService.update(courierProfileId, request));
    }

    @PatchMapping("/{userId}/activate")
    @Operation(summary = "Activate user", description = "Re-enables the local user and the underlying Keycloak identity.")
    public ResponseEntity<UserResponse> activateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.activate(userId));
    }

    @PatchMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivates the local user and Keycloak identity. Couriers that are currently ON_ROUTE must be rejected by business rules.")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.deactivate(userId));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Reset password for a target user", description = "Generates a new password in Keycloak and publishes a reset-password email through Kafka.")
    public ResponseEntity<PasswordResetResponse> resetPassword(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.resetPassword(userId));
    }
}
