package com.autopulse.userservice.web.controller;

import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.DispatcherService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/query")
@Tag(name = "User Queries", description = "Read-only query endpoints for user, dispatcher, and courier search.")
@SecurityRequirement(name = "bearer-jwt")
public class UserQueryController {

    private final UserService userService;
    private final DispatcherService dispatcherService;
    private final CourierService courierService;

    public UserQueryController(UserService userService,
                               DispatcherService dispatcherService,
                               CourierService courierService) {
        this.userService = userService;
        this.dispatcherService = dispatcherService;
        this.courierService = courierService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search business users", description = "Returns a paginated user listing with optional filters by email, first name, last name, phone, and active state.")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Standard Spring pagination and sorting")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.search(email, firstName, lastName, phoneNumber, active, pageable));
    }

    @GetMapping("/dispatchers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search dispatchers", description = "Returns a paginated dispatcher listing filtered by region and active state.")
    public ResponseEntity<Page<DispatcherResponse>> searchDispatchers(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(dispatcherService.search(regionCode, active, pageable));
    }

    @GetMapping("/couriers")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Search couriers", description = "Returns a paginated courier listing filtered by depot, region, availability, and active state.")
    public ResponseEntity<Page<CourierResponse>> searchCouriers(
            @RequestParam(required = false) UUID depotId,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) AvailabilityStatus availabilityStatus,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(courierService.search(depotId, regionCode, availabilityStatus, active, pageable));
    }

    @GetMapping("/couriers/available")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "List available couriers by depot", description = "Returns a page of active couriers that are AVAILABLE for the given depot id. Dispatcher callers remain region-scoped by service rules.")
    public ResponseEntity<Page<CourierResponse>> searchAvailableCouriersByDepot(
            @RequestParam UUID depotId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(courierService.searchAvailableByDepot(depotId, pageable));
    }

    @GetMapping("/couriers/{courierProfileId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','COURIER')")
    @Operation(summary = "Get courier by profile id", description = "Returns a single courier projection with user base data and operational availability.")
    public ResponseEntity<CourierResponse> getCourier(@PathVariable UUID courierProfileId) {
        return ResponseEntity.ok(courierService.getById(courierProfileId));
    }
}
