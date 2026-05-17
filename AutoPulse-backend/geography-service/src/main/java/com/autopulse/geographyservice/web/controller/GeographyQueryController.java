package com.autopulse.geographyservice.web.controller;

import com.autopulse.common.api.ErrorResponse;
import com.autopulse.geographyservice.service.GeocodingService;
import com.autopulse.geographyservice.service.LocalityService;
import com.autopulse.geographyservice.service.RegionService;
import com.autopulse.geographyservice.web.dto.CoordinatesResponse;
import com.autopulse.geographyservice.web.dto.GeocodeAddressRequest;
import com.autopulse.geographyservice.web.dto.LocalityResponse;
import com.autopulse.geographyservice.web.dto.RegionResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/geography/query")
@Tag(name = "Geography Queries", description = "Read-only query endpoints for region, locality, and coordinate lookup used by admin, dispatcher, and courier facing flows.")
@SecurityRequirement(name = "bearer-jwt")
public class GeographyQueryController {

    private final RegionService regionService;
    private final LocalityService localityService;
    private final GeocodingService geocodingService;

    public GeographyQueryController(RegionService regionService,
                                    LocalityService localityService,
                                    GeocodingService geocodingService) {
        this.regionService = regionService;
        this.localityService = localityService;
        this.geocodingService = geocodingService;
    }

    @GetMapping("/regions")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Search regions", description = "Returns a paginated region listing with optional filters by region name and code.")
    public ResponseEntity<Page<RegionResponse>> searchRegions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @Parameter(description = "Standard Spring pagination and sorting")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(regionService.search(name, code, pageable));
    }

    @GetMapping("/regions/{regionId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Get region by id", description = "Returns a single region projection for read-only usage.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Region found"),
            @ApiResponse(responseCode = "404", description = "Region not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RegionResponse> getRegion(@PathVariable UUID regionId) {
        return ResponseEntity.ok(regionService.getById(regionId));
    }

    @GetMapping("/regions/by-code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Get region by code", description = "Looks up a region using its stable business code.")
    public ResponseEntity<RegionResponse> getRegionByCode(@PathVariable String code) {
        return ResponseEntity.ok(regionService.getByCode(code));
    }

    @GetMapping("/localities")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Search localities", description = "Returns a paginated locality listing filtered by locality name, locality code, region id, and region code.")
    public ResponseEntity<Page<LocalityResponse>> searchLocalities(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) String regionCode,
            @Parameter(description = "Standard Spring pagination and sorting")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(localityService.search(name, code, regionId, regionCode, pageable));
    }

    @GetMapping("/localities/{localityId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Get locality by id", description = "Returns a single locality projection including the parent region reference.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locality found"),
            @ApiResponse(responseCode = "404", description = "Locality not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LocalityResponse> getLocality(@PathVariable UUID localityId) {
        return ResponseEntity.ok(localityService.getById(localityId));
    }

    @GetMapping("/localities/by-code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Get locality by code", description = "Looks up a locality using its stable business code.")
    public ResponseEntity<LocalityResponse> getLocalityByCode(@PathVariable String code) {
        return ResponseEntity.ok(localityService.getByCode(code));
    }

    @PostMapping("/coordinates")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Resolve address coordinates", description = "Uses Nominatim to resolve an address composed of street, city, and optional county into X/Y coordinates.")
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
