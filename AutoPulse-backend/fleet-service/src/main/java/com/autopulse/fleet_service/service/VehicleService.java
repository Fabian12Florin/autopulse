package com.autopulse.fleet_service.service;

import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.fleet_service.model.Depot;
import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleStatus;
import com.autopulse.fleet_service.repository.VehicleRepository;
import com.autopulse.fleet_service.web.dto.vehicle.AvailableVehicleSummaryResponse;
import com.autopulse.fleet_service.web.dto.vehicle.CreateVehicleRequest;
import com.autopulse.fleet_service.web.dto.vehicle.UpdateVehicleRequest;
import com.autopulse.fleet_service.web.dto.vehicle.UpdateVehicleStatusRequest;
import com.autopulse.fleet_service.web.dto.vehicle.VehicleResponse;
import com.autopulse.fleet_service.web.mapper.VehicleMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final DepotService depotService;
    private final VehicleMapper vehicleMapper;

    public VehicleService(
            VehicleRepository vehicleRepository,
            DepotService depotService,
            VehicleMapper vehicleMapper
    ) {
        this.vehicleRepository = vehicleRepository;
        this.depotService = depotService;
        this.vehicleMapper = vehicleMapper;
    }

    public VehicleResponse create(CreateVehicleRequest request) {
        log.info("Creating vehicle: licensePlate={}, vin={}, depotId={}",
                request.licensePlate(), request.vin(), request.depotId());

        if (vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            log.warn("Vehicle creation failed. License plate already exists: licensePlate={}", request.licensePlate());
            throw new ConflictException("License plate already exists");
        }

        if (vehicleRepository.existsByVin(request.vin())) {
            log.warn("Vehicle creation failed. VIN already exists: vin={}", request.vin());
            throw new ConflictException("VIN already exists");
        }

        Depot depot = depotService.getEntity(request.depotId());

        Vehicle vehicle = new Vehicle();
        vehicleMapper.mapForCreate(request, vehicle, depot);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle created successfully: vehicleId={}, licensePlate={}, status={}",
                savedVehicle.getId(), savedVehicle.getLicensePlate(), savedVehicle.getStatus());

        return vehicleMapper.toResponse(savedVehicle);
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAll(Pageable pageable) {
        log.debug("Fetching vehicles page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return vehicleRepository.findAll(pageable).map(vehicleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getById(UUID id) {
        log.debug("Fetching vehicle by id={}", id);
        return vehicleMapper.toResponse(getEntity(id));
    }

    public VehicleResponse update(UUID id, UpdateVehicleRequest request) {
        log.info("Updating vehicle: vehicleId={}, licensePlate={}, vin={}, depotId={}",
                id, request.licensePlate(), request.vin(), request.depotId());

        Vehicle vehicle = getEntity(id);

        if (vehicleRepository.existsByLicensePlateAndIdNot(request.licensePlate(), id)) {
            log.warn("Vehicle update failed. License plate already exists: vehicleId={}, licensePlate={}",
                    id, request.licensePlate());
            throw new ConflictException("License plate already exists");
        }

        if (vehicleRepository.existsByVinAndIdNot(request.vin(), id)) {
            log.warn("Vehicle update failed. VIN already exists: vehicleId={}, vin={}", id, request.vin());
            throw new ConflictException("VIN already exists");
        }

        Depot depot = depotService.getEntity(request.depotId());
        vehicleMapper.mapForUpdate(request, vehicle, depot);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully: vehicleId={}, licensePlate={}, status={}",
                savedVehicle.getId(), savedVehicle.getLicensePlate(), savedVehicle.getStatus());

        return vehicleMapper.toResponse(savedVehicle);
    }

    public VehicleResponse updateStatus(UUID id, UpdateVehicleStatusRequest request) {
        log.info("Updating vehicle status: vehicleId={}, newStatus={}", id, request.status());

        Vehicle vehicle = getEntity(id);
        VehicleStatus oldStatus = vehicle.getStatus();

        vehicle.setStatus(request.status());

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle status updated successfully: vehicleId={}, oldStatus={}, newStatus={}",
                savedVehicle.getId(), oldStatus, savedVehicle.getStatus());

        return vehicleMapper.toResponse(savedVehicle);
    }

    public VehicleResponse activate(UUID id) {
        log.info("Activating vehicle: vehicleId={}", id);

        Vehicle vehicle = getEntity(id);
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle activated successfully: vehicleId={}, status={}",
                savedVehicle.getId(), savedVehicle.getStatus());

        return vehicleMapper.toResponse(savedVehicle);
    }

    public VehicleResponse deactivate(UUID id) {
        log.info("Deactivating vehicle: vehicleId={}", id);

        Vehicle vehicle = getEntity(id);

        if (vehicle.getStatus() == VehicleStatus.IN_USE) {
            log.warn("Vehicle deactivation failed. Vehicle is currently in use: vehicleId={}", id);
            throw new ConflictException("Cannot deactivate a vehicle that is currently in use");
        }

        vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle deactivated successfully: vehicleId={}, status={}",
                savedVehicle.getId(), savedVehicle.getStatus());

        return vehicleMapper.toResponse(savedVehicle);
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAvailableByDepot(UUID depotId, Pageable pageable) {
        log.debug("Fetching available vehicles by depot: depotId={}, page={}, size={}, sort={}",
                depotId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return vehicleRepository.findByDepotIdAndStatus(depotId, VehicleStatus.AVAILABLE, pageable)
                .map(vehicleMapper::toResponse);
    }

    public Vehicle getEntity(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle not found: vehicleId={}", id);
                    return new NotFoundException("Vehicle not found");
                });
    }

    @Transactional(readOnly = true)
    public Page<AvailableVehicleSummaryResponse> getAvailableSummaryByDepot(UUID depotId, Pageable pageable) {
        log.debug("Fetching available vehicle summaries by depot: depotId={}, page={}, size={}, sort={}",
                depotId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return vehicleRepository.findByDepotIdAndStatus(depotId, VehicleStatus.AVAILABLE, pageable)
                .map(vehicleMapper::toAvailableSummary);
    }
}