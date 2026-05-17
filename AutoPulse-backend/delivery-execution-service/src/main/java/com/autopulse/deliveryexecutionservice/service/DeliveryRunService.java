package com.autopulse.deliveryexecutionservice.service;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.deliveryexecutionservice.mapper.DeliveryExecutionMapper;
import com.autopulse.deliveryexecutionservice.model.DeliveryParcelExecution;
import com.autopulse.deliveryexecutionservice.model.DeliveryRun;
import com.autopulse.deliveryexecutionservice.model.DeliveryStopExecution;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryOutcome;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryRunStatus;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryStopStatus;
import com.autopulse.deliveryexecutionservice.repository.DeliveryParcelExecutionRepository;
import com.autopulse.deliveryexecutionservice.repository.DeliveryRunRepository;
import com.autopulse.deliveryexecutionservice.repository.DeliveryStopExecutionRepository;
import com.autopulse.deliveryexecutionservice.service.client.ParcelClient;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryExecution.DeliveryParcelExecutionDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.CompleteParcelDeliveryRequest;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.CreateDeliveryParcelRequestDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.CreateDeliveryStopRequestDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.DeliveryRunDetailsDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.DeliveryRunDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.StartDeliveryRunRequestDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.VerifyDeliveryCodeRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelDeliveryCodeVerificationRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelDeliveryCodeVerificationResponse;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelDeliveryResultRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelFailureResultRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelRejectionResultRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelWaitingPickupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryRunService {

    private final DeliveryRunRepository deliveryRunRepository;
    private final DeliveryStopExecutionRepository deliveryStopExecutionRepository;
    private final DeliveryParcelExecutionRepository deliveryParcelExecutionRepository;
    private final ParcelClient parcelClient;
    private final DeliveryExecutionMapper mapper;

    @Transactional
    public DeliveryRunDto createRun(StartDeliveryRunRequestDto request) {
        if (deliveryRunRepository.existsByRouteId(request.routeId())) {
            throw new ConflictException("Delivery run already exists for route: " + request.routeId());
        }

        validateCreateRunRequest(request);

        DeliveryRun savedRun = deliveryRunRepository.save(createDeliveryRun(request));

        for (CreateDeliveryStopRequestDto stopRequest : request.stops()) {
            DeliveryStopExecution savedStop = deliveryStopExecutionRepository.save(createDeliveryStop(savedRun, stopRequest));
            saveDeliveryParcels(savedRun, savedStop, stopRequest.parcels());
        }

        return mapper.toDto(savedRun);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryRunDto> getByCourierAndDate(UUID courierId, LocalDate routeDate, String status, Pageable pageable) {
        DeliveryRunStatus requestedStatus = parseStatus(status);

        Specification<DeliveryRun> spec = (root, query, cb) -> cb.conjunction();
        if (courierId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("courierId"), courierId));
        }
        if (routeDate != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("routeDate"), routeDate));
        }
        if (requestedStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), requestedStatus));
        }
        return deliveryRunRepository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional
    public DeliveryRunDto markInProgress(UUID deliveryRunId) {
        DeliveryRun run = findByIdOrThrow(deliveryRunId);

        if (run.getStatus() == DeliveryRunStatus.IN_PROGRESS) {
            return mapper.toDto(run);
        }

        if (run.getStatus() != DeliveryRunStatus.NOT_STARTED) {
            throw new BadRequestException("Run with id " + deliveryRunId + " can be moved to IN_PROGRESS only from NOT_STARTED");
        }

        run.setStatus(DeliveryRunStatus.IN_PROGRESS);
        if (run.getStartedAt() == null) {
            run.setStartedAt(Instant.now());
        }

        DeliveryRun savedRun = deliveryRunRepository.save(run);
        markRunParcelsOutForDelivery(savedRun.getId());
        return mapper.toDto(savedRun);
    }

    @Transactional
    public DeliveryRunDto markAborted(UUID deliveryRunId) {
        DeliveryRun run = findByIdOrThrow(deliveryRunId);

        if (run.getStatus() != DeliveryRunStatus.NOT_STARTED && run.getStatus() != DeliveryRunStatus.IN_PROGRESS) {
            throw new BadRequestException("Run with id " + deliveryRunId + " can be aborted only from NOT_STARTED or IN_PROGRESS");
        }

        run.setStatus(DeliveryRunStatus.ABORTED);
        if (run.getFinishedAt() == null) {
            run.setFinishedAt(Instant.now());
        }

        DeliveryRun savedRun = deliveryRunRepository.save(run);
        return mapper.toDto(savedRun);
    }

    @Transactional
    public DeliveryRunDto completeRun(UUID deliveryRunId) {
        DeliveryRun run = findByIdOrThrow(deliveryRunId);

        if (run.getStatus() == DeliveryRunStatus.COMPLETED) {
            return mapper.toDto(run);
        }

        if (run.getStatus() != DeliveryRunStatus.IN_PROGRESS) {
            throw new BadRequestException("Run with id " + deliveryRunId + " can be completed only from IN_PROGRESS");
        }

        ensureAllStopsCompleted(run.getId());

        run.setFinishedAt(Instant.now());
        run.setStatus(DeliveryRunStatus.COMPLETED);

        DeliveryRun savedRun = deliveryRunRepository.save(run);
        return mapper.toDto(savedRun);
    }

    @Transactional(readOnly = true)
    public DeliveryRunDto getByRouteId(UUID routeId) {
        DeliveryRun run = findByRouteIdOrThrow(routeId);
        return mapper.toDto(run);
    }

    @Transactional(readOnly = true)
    public DeliveryRunDetailsDto getDetails(UUID deliveryRunId) {
        DeliveryRun run = findByIdOrThrow(deliveryRunId);
        List<DeliveryStopExecution> stops = deliveryStopExecutionRepository.findByDeliveryRunIdOrderByStopOrderAsc(deliveryRunId);
        Map<UUID, List<DeliveryParcelExecution>> parcelsByStopId = stops.stream().collect(Collectors.toMap(DeliveryStopExecution::getId, stop -> deliveryParcelExecutionRepository.findByDeliveryStopExecutionId(stop.getId())));

        return mapper.toDetailsDto(run, stops, parcelsByStopId);
    }

    @Transactional
    public DeliveryRunDetailsDto arriveAtStop(UUID deliveryRunId, UUID deliveryStopExecutionId) {
        DeliveryRun run = findByIdOrThrow(deliveryRunId);
        ensureRunInProgress(run);

        DeliveryStopExecution stop = deliveryStopExecutionRepository.findById(deliveryStopExecutionId).orElseThrow(() -> new NotFoundException("Delivery stop execution not found with id: " + deliveryStopExecutionId));
        ensureStopBelongsToRun(stop, run);
        ensureCurrentStop(stop, run.getId());
        markStopInProgress(stop, Instant.now());

        return getDetails(deliveryRunId);
    }

    @Transactional
    public DeliveryParcelExecutionDto verifyDeliveryCode(UUID deliveryRunId, UUID deliveryStopExecutionId, VerifyDeliveryCodeRequest request) {
        DeliveryRun run = findByIdOrThrow(deliveryRunId);
        ensureRunInProgress(run);

        DeliveryStopExecution stop = deliveryStopExecutionRepository.findById(deliveryStopExecutionId).orElseThrow(() -> new NotFoundException("Delivery stop execution not found with id: " + deliveryStopExecutionId));
        ensureStopBelongsToRun(stop, run);
        ensureCurrentStop(stop, run.getId());

        DeliveryParcelExecution parcel = deliveryParcelExecutionRepository.findByDeliveryStopExecutionIdAndAwb(stop.getId(), request.awb()).orElseThrow(() -> new NotFoundException("Delivery parcel execution not found for AWB: " + request.awb()));

        if (parcel.getCompletedAt() != null) {
            throw new ConflictException("Delivery parcel execution is already completed");
        }

        ParcelDeliveryCodeVerificationResponse verification = parcelClient.verifyDeliveryCode(new ParcelDeliveryCodeVerificationRequest(request.awb(), request.pin()));

        if (verification == null || !Boolean.TRUE.equals(verification.valid())) {
            throw new BadRequestException("Invalid delivery code for AWB: " + request.awb());
        }

        Instant now = Instant.now();
        markStopInProgress(stop, now);
        parcel.setDeliveryCodeVerified(true);

        DeliveryParcelExecution savedParcel = deliveryParcelExecutionRepository.save(parcel);
        return mapper.toDto(savedParcel);
    }

    @Transactional
    public DeliveryParcelExecutionDto completeParcel(UUID deliveryRunId, UUID deliveryParcelExecutionId, CompleteParcelDeliveryRequest request) {
        DeliveryParcelExecution parcel = deliveryParcelExecutionRepository.findById(deliveryParcelExecutionId).orElseThrow(() -> new NotFoundException("Delivery parcel execution not found with id: " + deliveryParcelExecutionId));
        DeliveryRun run = parcel.getDeliveryRun();
        if (!run.getId().equals(deliveryRunId)) {
            throw new BadRequestException("Delivery parcel execution does not belong to delivery run: " + deliveryRunId);
        }
        ensureRunInProgress(run);

        if (parcel.getCompletedAt() != null) {
            throw new ConflictException("Delivery parcel execution is already completed");
        }

        if (request.outcome() == DeliveryOutcome.DELIVERED && !Boolean.TRUE.equals(parcel.getDeliveryCodeVerified())) {
            throw new BadRequestException("Parcel cannot be marked DELIVERED until delivery code is validated");
        }

        Instant now = Instant.now();
        DeliveryStopExecution stop = parcel.getDeliveryStopExecution();
        ensureCurrentStop(stop, run.getId());
        markStopInProgress(stop, now);

        parcel.setOutcome(request.outcome());
        parcel.setCompletedAt(now);

        DeliveryParcelExecution savedParcel = deliveryParcelExecutionRepository.save(parcel);
        synchronizeParcelOutcome(savedParcel, request, now);

        completeStopIfNeeded(stop, now);
        completeRunIfNeeded(run, now);

        return mapper.toDto(savedParcel);
    }

    @Transactional
    public void delete(UUID deliveryRunId) {
        DeliveryRun run = findByIdOrThrow(deliveryRunId);

        if (run.getStatus() != DeliveryRunStatus.NOT_STARTED && run.getStatus() != DeliveryRunStatus.COMPLETED && run.getStatus() != DeliveryRunStatus.ABORTED) {
            throw new BadRequestException("Run with id " + deliveryRunId + " can be deleted only when status is NOT_STARTED, COMPLETED or ABORTED");
        }

        deliveryRunRepository.delete(run);
    }

    private void validateCreateRunRequest(StartDeliveryRunRequestDto request) {
        int expectedParcelCount = request.stops().stream().map(CreateDeliveryStopRequestDto::parcelCount).reduce(0, Integer::sum);

        if (!request.totalParcelCount().equals(expectedParcelCount)) {
            throw new BadRequestException("Run totalParcelCount must match the sum of stop parcel counts");
        }

        Set<UUID> routeStopIds = new HashSet<>();
        Set<UUID> parcelIds = new HashSet<>();

        for (CreateDeliveryStopRequestDto stop : request.stops()) {
            if (!routeStopIds.add(stop.routeStopId())) {
                throw new BadRequestException("Duplicate routeStopId in delivery run request: " + stop.routeStopId());
            }

            if (stop.parcels().size() != stop.parcelCount()) {
                throw new BadRequestException("Stop parcelCount must match the number of parcel entries for routeStopId: " + stop.routeStopId());
            }

            for (CreateDeliveryParcelRequestDto parcel : stop.parcels()) {
                if (!parcelIds.add(parcel.parcelId())) {
                    throw new BadRequestException("Duplicate parcelId in delivery run request: " + parcel.parcelId());
                }
            }
        }
    }

    private DeliveryRun createDeliveryRun(StartDeliveryRunRequestDto request) {
        DeliveryRun run = new DeliveryRun();
        run.setRouteId(request.routeId());
        run.setCourierId(request.courierId());
        run.setVehicleId(request.vehicleId());
        run.setRouteDate(request.routeDate());
        run.setGoogleMapsUrl(request.googleMapsUrl());
        run.setTotalParcelCount(request.totalParcelCount());
        run.setTotalWeight(request.totalWeight());
        run.setTotalVolume(request.totalVolume());
        run.setStatus(DeliveryRunStatus.NOT_STARTED);
        run.setStartedAt(null);
        run.setFinishedAt(null);
        return run;
    }

    private DeliveryStopExecution createDeliveryStop(DeliveryRun run, CreateDeliveryStopRequestDto request) {
        DeliveryStopExecution stop = new DeliveryStopExecution();
        stop.setDeliveryRun(run);
        stop.setRouteStopId(request.routeStopId());
        stop.setStopOrder(request.stopOrder());
        stop.setLatitude(request.latitude());
        stop.setLongitude(request.longitude());
        stop.setParcelCount(request.parcelCount());
        stop.setTotalWeight(request.totalWeight());
        stop.setTotalVolume(request.totalVolume());
        stop.setStatus(DeliveryStopStatus.PENDING);
        return stop;
    }

    private void saveDeliveryParcels(DeliveryRun run, DeliveryStopExecution stop, List<CreateDeliveryParcelRequestDto> parcelRequests) {
        List<DeliveryParcelExecution> parcels = parcelRequests.stream().map(parcelRequest -> createDeliveryParcel(run, stop, parcelRequest)).toList();

        deliveryParcelExecutionRepository.saveAll(parcels);
    }

    private DeliveryParcelExecution createDeliveryParcel(DeliveryRun run, DeliveryStopExecution stop, CreateDeliveryParcelRequestDto request) {
        DeliveryParcelExecution parcel = new DeliveryParcelExecution();
        parcel.setDeliveryRun(run);
        parcel.setDeliveryStopExecution(stop);
        parcel.setRouteStopId(stop.getRouteStopId());
        parcel.setParcelId(request.parcelId());
        parcel.setAwb(request.awb());
        parcel.setReceiverName(request.receiverName());
        parcel.setReceiverPhone(request.receiverPhone());
        parcel.setWeight(request.weight());
        parcel.setVolume(request.volume());
        parcel.setDeliveryCodeVerified(false);
        parcel.setOutcome(null);
        parcel.setCompletedAt(null);
        return parcel;
    }

    private void markRunParcelsOutForDelivery(UUID deliveryRunId) {
        deliveryParcelExecutionRepository.findByDeliveryRunId(deliveryRunId).stream()
                .map(DeliveryParcelExecution::getParcelId)
                .distinct()
                .forEach(parcelClient::markOutForDelivery);
    }

    private void synchronizeParcelOutcome(
            DeliveryParcelExecution parcel,
            CompleteParcelDeliveryRequest request,
            Instant completedAt
    ) {
        String notes = defaultText(request.notes(), "Delivery execution outcome: " + request.outcome());

        switch (request.outcome()) {
            case DELIVERED -> parcelClient.markDelivered(
                    parcel.getParcelId(),
                    new ParcelDeliveryResultRequest(
                            completedAt,
                            true,
                            notes,
                            request.paymentCollected()
                    )
            );
            case FAILED -> parcelClient.markFailed(
                    parcel.getParcelId(),
                    new ParcelFailureResultRequest(completedAt, notes)
            );
            case REJECTED -> parcelClient.markRejected(
                    parcel.getParcelId(),
                    new ParcelRejectionResultRequest(completedAt, notes)
            );
            case WAITING_PICKUP -> parcelClient.markWaitingPickup(
                    parcel.getParcelId(),
                    new ParcelWaitingPickupRequest(completedAt, notes)
            );
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void completeStopIfNeeded(DeliveryStopExecution stop, Instant now) {
        boolean allParcelsCompleted = deliveryParcelExecutionRepository.findByDeliveryStopExecutionId(stop.getId()).stream().allMatch(this::isParcelCompleted);

        if (allParcelsCompleted) {
            stop.setStatus(DeliveryStopStatus.COMPLETED);
            stop.setCompletedAt(now);
            deliveryStopExecutionRepository.save(stop);
        }
    }

    private void completeRunIfNeeded(DeliveryRun run, Instant now) {
        boolean allStopsCompleted = deliveryStopExecutionRepository.findByDeliveryRunIdOrderByStopOrderAsc(run.getId()).stream().allMatch(stop -> stop.getStatus() == DeliveryStopStatus.COMPLETED);

        if (allStopsCompleted) {
            run.setStatus(DeliveryRunStatus.COMPLETED);
            run.setFinishedAt(now);
            deliveryRunRepository.save(run);
        }
    }

    private void ensureAllStopsCompleted(UUID deliveryRunId) {
        boolean allStopsCompleted = deliveryStopExecutionRepository.findByDeliveryRunIdOrderByStopOrderAsc(deliveryRunId).stream().allMatch(stop -> stop.getStatus() == DeliveryStopStatus.COMPLETED);

        if (!allStopsCompleted) {
            throw new BadRequestException("Delivery run can be completed only after all stops are completed");
        }
    }

    private void ensureRunInProgress(DeliveryRun run) {
        if (run.getStatus() != DeliveryRunStatus.IN_PROGRESS) {
            throw new BadRequestException("Delivery run must be IN_PROGRESS");
        }
    }

    private void ensureStopBelongsToRun(DeliveryStopExecution stop, DeliveryRun run) {
        if (!run.getId().equals(stop.getDeliveryRun().getId())) {
            throw new BadRequestException("Delivery stop execution does not belong to delivery run: " + run.getId());
        }
    }

    private void ensureCurrentStop(DeliveryStopExecution stop, UUID deliveryRunId) {
        DeliveryStopExecution currentStop = deliveryStopExecutionRepository.findByDeliveryRunIdOrderByStopOrderAsc(deliveryRunId).stream().filter(candidate -> candidate.getStatus() != DeliveryStopStatus.COMPLETED).findFirst().orElseThrow(() -> new BadRequestException("Delivery run has no active stop"));

        if (!currentStop.getId().equals(stop.getId())) {
            throw new BadRequestException("Parcel can be verified only for the current stop");
        }
    }

    private void markStopInProgress(DeliveryStopExecution stop, Instant now) {
        if (stop.getStatus() == DeliveryStopStatus.PENDING) {
            stop.setStatus(DeliveryStopStatus.IN_PROGRESS);
            stop.setArrivedAt(now);
            deliveryStopExecutionRepository.save(stop);
        }
    }

    private boolean isParcelCompleted(DeliveryParcelExecution parcel) {
        return parcel.getOutcome() != null && parcel.getCompletedAt() != null;
    }

    private DeliveryRunStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return DeliveryRunStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported delivery run status: " + status);
        }
    }

    private DeliveryRun findByIdOrThrow(UUID deliveryRunId) {
        return deliveryRunRepository.findById(deliveryRunId).orElseThrow(() -> new NotFoundException("Delivery run not found with id: " + deliveryRunId));
    }

    private DeliveryRun findByRouteIdOrThrow(UUID routeId) {
        return deliveryRunRepository.findByRouteId(routeId).orElseThrow(() -> new NotFoundException("Delivery run not found for routeId: " + routeId));
    }
}
