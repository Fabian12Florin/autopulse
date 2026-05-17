package com.autopulse.routingservice.web.controller;

import com.autopulse.routingservice.model.enums.RouteType;
import com.autopulse.routingservice.model.enums.RoutingJobStatus;
import com.autopulse.routingservice.service.RoutingJobService;
import com.autopulse.routingservice.web.dto.RoutingJobCreateRequest;
import com.autopulse.routingservice.web.dto.RoutingJobQueryRequest;
import com.autopulse.routingservice.web.dto.RoutingJobResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/routing/routing-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class RoutingJobController {

    private final RoutingJobService routingJobService;

    @PostMapping("/create")
    public ResponseEntity<RoutingJobResponse> createRoutingJob(@Valid @RequestBody RoutingJobCreateRequest request) {
        RoutingJobResponse response = routingJobService.createRoutingJob(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{routingJobId}/select")
    public ResponseEntity<RoutingJobResponse> selectRoutingJob(@PathVariable UUID routingJobId) {
        RoutingJobResponse response = routingJobService.selectRoutingJob(routingJobId);
        return ResponseEntity.
                status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<RoutingJobResponse>> queryRoutingJobs(
            @RequestParam(required = false) String depotCode,
            @RequestParam(required = false) LocalDate routeDate,
            @RequestParam(required = false) RouteType routeType,
            @RequestParam(required = false) RoutingJobStatus status,
            Pageable pageable
    ) {
        RoutingJobQueryRequest request = new RoutingJobQueryRequest(
                depotCode,
                routeDate,
                routeType,
                status
        );

        Page<RoutingJobResponse> response = routingJobService.queryRoutingJobs(request, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
