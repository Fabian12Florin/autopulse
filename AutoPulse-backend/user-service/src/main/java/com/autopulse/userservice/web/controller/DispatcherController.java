package com.autopulse.userservice.web.controller;

import com.autopulse.common.api.ErrorResponse;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.DispatcherService;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.CreateCourierRequest;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.UpdateCourierRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/dispatcher")
@PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
@Tag(name = "Dispatcher Operations", description = "Endpoints used by dispatchers to manage couriers in their authorized region and to inspect dispatcher profile data.")
@SecurityRequirement(name = "bearer-jwt")
public class DispatcherController {

    private final DispatcherService dispatcherService;
    private final CourierService courierService;

    public DispatcherController(DispatcherService dispatcherService,
                                CourierService courierService) {
        this.dispatcherService = dispatcherService;
        this.courierService = courierService;
    }

    @GetMapping("/profiles/{dispatcherProfileId}")
    @Operation(summary = "Get dispatcher profile", description = "Returns a dispatcher profile. Admins can inspect any profile; dispatchers are limited by service-layer rules if needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispatcher profile found"),
            @ApiResponse(responseCode = "404", description = "Dispatcher profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DispatcherResponse> getDispatcher(@PathVariable UUID dispatcherProfileId) {
        return ResponseEntity.ok(dispatcherService.getById(dispatcherProfileId));
    }

    @GetMapping("/couriers")
    @Operation(summary = "Search couriers", description = "Searches couriers using optional depot, region, active-state, and availability filters. Dispatcher visibility is expected to be narrowed by business rules to the assigned region.")
    public ResponseEntity<Page<CourierResponse>> searchCouriers(
            @RequestParam(required = false) UUID depotId,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) AvailabilityStatus availabilityStatus,
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Standard Spring pagination and sorting")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(courierService.search(depotId, regionCode, availabilityStatus, active, pageable));
    }

    @GetMapping("/couriers/{courierProfileId}")
    @Operation(summary = "Get courier details", description = "Returns a courier profile and base user information for dispatcher operational usage.")
    public ResponseEntity<CourierResponse> getCourier(@PathVariable UUID courierProfileId) {
        return ResponseEntity.ok(courierService.getById(courierProfileId));
    }

    @PostMapping("/couriers")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Create courier as dispatcher", description = "Creates a courier. Service-layer validation must ensure that a dispatcher can only create couriers inside the dispatcher's region.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Courier created"),
            @ApiResponse(responseCode = "403", description = "Region access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate identity or profile conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourierResponse> createCourier(@Valid @RequestBody CreateCourierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courierService.create(request));
    }

    @PatchMapping("/couriers/{courierProfileId}")
    @Operation(summary = "Update courier as dispatcher", description = "Updates mutable courier fields. Dispatcher access is restricted by region and operational state rules inside the service layer.")
    public ResponseEntity<CourierResponse> updateCourier(@PathVariable UUID courierProfileId,
                                                         @Valid @RequestBody UpdateCourierRequest request) {
        return ResponseEntity.ok(courierService.update(courierProfileId, request));
    }
}
