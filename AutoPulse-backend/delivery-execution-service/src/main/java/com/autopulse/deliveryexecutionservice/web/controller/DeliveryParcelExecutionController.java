package com.autopulse.deliveryexecutionservice.web.controller;

import com.autopulse.deliveryexecutionservice.service.DeliveryParcelExecutionService;
import com.autopulse.deliveryexecutionservice.service.DeliveryRunService;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.CompleteParcelDeliveryRequest;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryExecution.DeliveryParcelExecutionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-execution/runs/{deliveryRunId}/parcels")
@RequiredArgsConstructor
public class DeliveryParcelExecutionController {

    private final DeliveryParcelExecutionService parcelExecutionService;
    private final DeliveryRunService deliveryRunService;

    @GetMapping
    public Slice<DeliveryParcelExecutionDto> getByRunId(
            @PathVariable UUID deliveryRunId,
            Pageable pageable
    ) {
        return parcelExecutionService.getByRunId(deliveryRunId, pageable);
    }


    @PostMapping("/{deliveryParcelExecutionId}/complete")
    public DeliveryParcelExecutionDto completeParcel(
            @PathVariable UUID deliveryRunId,
            @PathVariable UUID deliveryParcelExecutionId,
            @Valid @RequestBody CompleteParcelDeliveryRequest request
    ) {
        return deliveryRunService.completeParcel(deliveryRunId, deliveryParcelExecutionId, request);
    }
}
