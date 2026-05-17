package com.autopulse.geographyservice.web.controller;

import com.autopulse.common.api.ErrorResponse;
import com.autopulse.geographyservice.service.GeocodingService;
import com.autopulse.geographyservice.service.LocalityService;
import com.autopulse.geographyservice.service.RegionService;
import com.autopulse.geographyservice.web.dto.CoordinatesResponse;
import com.autopulse.geographyservice.web.dto.GeocodeAddressRequest;
import com.autopulse.geographyservice.web.dto.LocalityReferenceResponse;
import com.autopulse.geographyservice.web.dto.RegionReferenceResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/geography/internal")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Internal Geography APIs", description = "Internal lookup endpoints intended for service-to-service reference validation and enrichment.")
@SecurityRequirement(name = "bearer-jwt")
public class InternalGeographyController {

    private final RegionService regionService;
    private final LocalityService localityService;
    private final GeocodingService geocodingService;

    public InternalGeographyController(RegionService regionService,
                                       LocalityService localityService,
                                       GeocodingService geocodingService) {
        this.regionService = regionService;
        this.localityService = localityService;
        this.geocodingService = geocodingService;
    }

    @GetMapping("/regions/{regionId}")
    @Operation(summary = "Get region reference by id", description = "Returns a lightweight region reference projection for internal consumers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Region found"),
            @ApiResponse(responseCode = "404", description = "Region not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RegionReferenceResponse> getRegionReference(@PathVariable UUID regionId) {
        return ResponseEntity.ok(regionService.getReferenceById(regionId));
    }

    @GetMapping("/regions/by-code/{code}")
    @Operation(summary = "Get region reference by code", description = "Resolves a region code to the corresponding internal reference projection.")
    public ResponseEntity<RegionReferenceResponse> getRegionReferenceByCode(@PathVariable String code) {
        return ResponseEntity.ok(regionService.getReferenceByCode(code));
    }

    @GetMapping("/localities/{localityId}")
    @Operation(summary = "Get locality reference by id", description = "Returns a lightweight locality reference projection, including parent region data, for internal consumers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locality found"),
            @ApiResponse(responseCode = "404", description = "Locality not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LocalityReferenceResponse> getLocalityReference(@PathVariable UUID localityId) {
        return ResponseEntity.ok(localityService.getReferenceById(localityId));
    }

    @GetMapping("/localities/by-code/{code}")
    @Operation(summary = "Get locality reference by code", description = "Resolves a locality code to the corresponding internal reference projection.")
    public ResponseEntity<LocalityReferenceResponse> getLocalityReferenceByCode(@PathVariable String code) {
        return ResponseEntity.ok(localityService.getReferenceByCode(code));
    }

    @PostMapping("/coordinates")
    @Operation(summary = "Resolve address coordinates", description = "Resolves street, city, and optional county into X/Y coordinates for internal routing consumers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coordinates resolved"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coordinates not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "External geocoding provider error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CoordinatesResponse> resolveCoordinates(@Valid @RequestBody GeocodeAddressRequest request) {
        return ResponseEntity.ok(geocodingService.resolveCoordinates(request));
    }
}
