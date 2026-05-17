package com.autopulse.deliveryexecutionservice.web.controller;

import com.autopulse.deliveryexecutionservice.service.DeliveryRunService;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.DeliveryRunDetailsDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.DeliveryRunDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.StartDeliveryRunRequestDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.VerifyDeliveryCodeRequest;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryExecution.DeliveryParcelExecutionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-execution/runs")
@RequiredArgsConstructor
public class DeliveryRunController {

    private final DeliveryRunService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryRunDto create(@Valid @RequestBody StartDeliveryRunRequestDto request) {
        return service.createRun(request);
    }

    @GetMapping
    public Page<DeliveryRunDto> getByCourierAndDate(
            @RequestParam(required = false) UUID courierId,
            @RequestParam(required = false) LocalDate routeDate,
            @RequestParam(required = false) String status,
            Pageable pageable
    ) {
        return service.getByCourierAndDate(courierId, routeDate, status, pageable);
    }

    @PostMapping("/{deliveryRunId}/start")
    public DeliveryRunDto markInProgress(@PathVariable UUID deliveryRunId) {
        return service.markInProgress(deliveryRunId);
    }

    @PostMapping("/{deliveryRunId}/abort")
    public DeliveryRunDto markAborted(@PathVariable UUID deliveryRunId) {
        return service.markAborted(deliveryRunId);
    }

    @PostMapping("/{deliveryRunId}/complete")
    public DeliveryRunDto complete(@PathVariable UUID deliveryRunId) {
        return service.completeRun(deliveryRunId);
    }

    @GetMapping("/by-route/{routeId}")
    public DeliveryRunDto getByRouteId(@PathVariable UUID routeId) {
        return service.getByRouteId(routeId);
    }

    @GetMapping("/{deliveryRunId}")
    public DeliveryRunDetailsDto getDetails(@PathVariable UUID deliveryRunId) {
        return service.getDetails(deliveryRunId);
    }

    @PostMapping("/{deliveryRunId}/stops/{deliveryStopExecutionId}/arrive")
    public DeliveryRunDetailsDto arriveAtStop(
            @PathVariable UUID deliveryRunId,
            @PathVariable UUID deliveryStopExecutionId
    ) {
        return service.arriveAtStop(deliveryRunId, deliveryStopExecutionId);
    }

    @PostMapping("/{deliveryRunId}/stops/{deliveryStopExecutionId}/parcels/verify-code")
    public DeliveryParcelExecutionDto verifyDeliveryCode(
            @PathVariable UUID deliveryRunId,
            @PathVariable UUID deliveryStopExecutionId,
            @Valid @RequestBody VerifyDeliveryCodeRequest request
    ) {
        return service.verifyDeliveryCode(deliveryRunId, deliveryStopExecutionId, request);
    }

    @DeleteMapping("/{deliveryRunId}")
    public void delete(@PathVariable UUID deliveryRunId) {
        service.delete(deliveryRunId);
    }
}
