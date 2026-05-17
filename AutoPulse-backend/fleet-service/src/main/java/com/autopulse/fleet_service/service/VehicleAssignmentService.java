package com.autopulse.fleet_service.service;

import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleAssignment;
import com.autopulse.fleet_service.model.VehicleAssignmentStatus;
import com.autopulse.fleet_service.model.VehicleStatus;
import com.autopulse.fleet_service.repository.VehicleAssignmentRepository;
import com.autopulse.fleet_service.web.dto.vehicle_assignment.CreateVehicleAssignmentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_assignment.VehicleAssignmentResponse;
import com.autopulse.fleet_service.web.mapper.VehicleAssignmentMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(VehicleAssignmentService.class);

    private final VehicleAssignmentRepository repository;
    private final VehicleService vehicleService;
    private final VehicleAssignmentMapper vehicleAssignmentMapper;

    public VehicleAssignmentService(
            VehicleAssignmentRepository repository,
            VehicleService vehicleService,
            VehicleAssignmentMapper vehicleAssignmentMapper
    ) {
        this.repository = repository;
        this.vehicleService = vehicleService;
        this.vehicleAssignmentMapper = vehicleAssignmentMapper;
    }

    public VehicleAssignmentResponse assign(CreateVehicleAssignmentRequest request) {
        log.info("Assigning vehicle to courier: vehicleId={}, courierId={}",
                request.vehicleId(), request.courierId());

        Vehicle vehicle = vehicleService.getEntity(request.vehicleId());

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            log.warn("Vehicle assignment failed. Vehicle is not available: vehicleId={}, status={}",
                    vehicle.getId(), vehicle.getStatus());
            throw new ConflictException("Vehicle is not available");
        }

        repository.findFirstByVehicleIdAndStatus(vehicle.getId(), VehicleAssignmentStatus.ACTIVE)
                .ifPresent(existing -> {
                    log.warn("Vehicle assignment failed. Vehicle already has active assignment: vehicleId={}, assignmentId={}",
                            vehicle.getId(), existing.getId());
                    throw new ConflictException("Vehicle already has an active assignment");
                });

        repository.findFirstByCourierIdAndStatus(request.courierId(), VehicleAssignmentStatus.ACTIVE)
                .ifPresent(existing -> {
                    log.warn("Vehicle assignment failed. Courier already has active assignment: courierId={}, assignmentId={}",
                            request.courierId(), existing.getId());
                    throw new ConflictException("Courier already has an active assignment");
                });

        VehicleAssignment assignment = vehicleAssignmentMapper.toEntity(request, vehicle);
        vehicle.setStatus(VehicleStatus.IN_USE);

        VehicleAssignment savedAssignment = repository.save(assignment);
        log.info("Vehicle assigned successfully: assignmentId={}, vehicleId={}, courierId={}",
                savedAssignment.getId(), vehicle.getId(), savedAssignment.getCourierId());

        return vehicleAssignmentMapper.toResponse(savedAssignment);
    }

    public VehicleAssignmentResponse endAssignment(UUID id) {
        log.info("Ending vehicle assignment: assignmentId={}", id);

        VehicleAssignment assignment = getEntity(id);

        if (assignment.getStatus() != VehicleAssignmentStatus.ACTIVE) {
            log.warn("End assignment failed. Assignment is not active: assignmentId={}, status={}",
                    id, assignment.getStatus());
            throw new ConflictException("Only active assignments can be ended");
        }

        assignment.setUnassignedAt(LocalDateTime.now());
        assignment.setStatus(VehicleAssignmentStatus.ENDED);

        Vehicle vehicle = assignment.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        VehicleAssignment savedAssignment = repository.save(assignment);
        log.info("Vehicle assignment ended successfully: assignmentId={}, vehicleId={}, courierId={}",
                savedAssignment.getId(), vehicle.getId(), savedAssignment.getCourierId());

        return vehicleAssignmentMapper.toResponse(savedAssignment);
    }

    @Transactional(readOnly = true)
    public VehicleAssignmentResponse getById(UUID id) {
        log.debug("Fetching vehicle assignment by id={}", id);
        return vehicleAssignmentMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<VehicleAssignmentResponse> getAll(Pageable pageable) {
        log.debug("Fetching vehicle assignments page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return repository.findAll(pageable).map(vehicleAssignmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VehicleAssignmentResponse> getByCourierId(UUID courierId, Pageable pageable) {
        log.debug("Fetching vehicle assignments by courier: courierId={}, page={}, size={}, sort={}",
                courierId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return repository.findByCourierId(courierId, pageable).map(vehicleAssignmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleAssignmentResponse getActiveByCourierId(UUID courierId) {
        log.debug("Fetching active vehicle assignment by courierId={}", courierId);

        return repository.findFirstByCourierIdAndStatus(courierId, VehicleAssignmentStatus.ACTIVE)
                .map(vehicleAssignmentMapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("No active vehicle assignment found for courierId={}", courierId);
                    return new NotFoundException("No active assignment for courier");
                });
    }

    public VehicleAssignment getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle assignment not found: assignmentId={}", id);
                    return new NotFoundException("Vehicle assignment not found");
                });
    }
}