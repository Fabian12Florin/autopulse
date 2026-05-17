package com.autopulse.geographyservice.web.controller;

import com.autopulse.common.api.ErrorResponse;
import com.autopulse.geographyservice.service.LocalityService;
import com.autopulse.geographyservice.service.RegionService;
import com.autopulse.geographyservice.web.dto.CreateLocalityRequest;
import com.autopulse.geographyservice.web.dto.CreateRegionRequest;
import com.autopulse.geographyservice.web.dto.LocalityResponse;
import com.autopulse.geographyservice.web.dto.RegionResponse;
import com.autopulse.geographyservice.web.dto.UpdateLocalityRequest;
import com.autopulse.geographyservice.web.dto.UpdateRegionRequest;
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
@RequestMapping("/api/geography/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Geography Management", description = "Admin-only operations for managing regions and localities used as master geography data across AutoPulse.")
@SecurityRequirement(name = "bearer-jwt")
public class AdminGeographyController {

    private final RegionService regionService;
    private final LocalityService localityService;

    public AdminGeographyController(RegionService regionService,
                                    LocalityService localityService) {
        this.regionService = regionService;
        this.localityService = localityService;
    }

    @GetMapping("/regions/{regionId}")
    @Operation(summary = "Get region by id", description = "Returns a single region aggregate intended for administrative management flows.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Region found"),
            @ApiResponse(responseCode = "404", description = "Region not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RegionResponse> getRegion(@PathVariable UUID regionId) {
        return ResponseEntity.ok(regionService.getById(regionId));
    }

    @PostMapping("/regions")
    @Operation(summary = "Create region", description = "Creates a region master-data record with a stable business code used throughout the platform.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Region created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Region code already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RegionResponse> createRegion(@Valid @RequestBody CreateRegionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regionService.create(request));
    }

    @PatchMapping("/regions/{regionId}")
    @Operation(summary = "Update region", description = "Updates mutable region fields while preserving uniqueness rules for the region code.")
    public ResponseEntity<RegionResponse> updateRegion(@PathVariable UUID regionId,
                                                       @Valid @RequestBody UpdateRegionRequest request) {
        return ResponseEntity.ok(regionService.update(regionId, request));
    }

    @GetMapping("/localities/{localityId}")
    @Operation(summary = "Get locality by id", description = "Returns a single locality aggregate including the parent region reference.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locality found"),
            @ApiResponse(responseCode = "404", description = "Locality or region not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LocalityResponse> getLocality(@PathVariable UUID localityId) {
        return ResponseEntity.ok(localityService.getById(localityId));
    }

    @PostMapping("/localities")
    @Operation(summary = "Create locality", description = "Creates a locality and links it to an existing region. The locality code must remain unique across the service.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Locality created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Region not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Locality code already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LocalityResponse> createLocality(@Valid @RequestBody CreateLocalityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(localityService.create(request));
    }

    @PatchMapping("/localities/{localityId}")
    @Operation(summary = "Update locality", description = "Updates locality master data and optionally moves the locality to another existing region.")
    public ResponseEntity<LocalityResponse> updateLocality(@PathVariable UUID localityId,
                                                           @Valid @RequestBody UpdateLocalityRequest request) {
        return ResponseEntity.ok(localityService.update(localityId, request));
    }
}
