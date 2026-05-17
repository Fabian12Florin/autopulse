package com.autopulse.fleet_service.service;

import com.autopulse.common.exception.NotFoundException;
import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleMaintenance;
import com.autopulse.fleet_service.repository.VehicleMaintenanceRepository;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.CreateVehicleMaintenanceRequest;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.UpdateVehicleMaintenanceRequest;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.VehicleMaintenanceResponse;
import com.autopulse.fleet_service.web.mapper.VehicleMaintenanceMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(VehicleMaintenanceService.class);

    private final VehicleMaintenanceRepository repository;
    private final VehicleService vehicleService;
    private final VehicleMaintenanceMapper vehicleMaintenanceMapper;

    public VehicleMaintenanceService(
            VehicleMaintenanceRepository repository,
            VehicleService vehicleService,
            VehicleMaintenanceMapper vehicleMaintenanceMapper
    ) {
        this.repository = repository;
        this.vehicleService = vehicleService;
        this.vehicleMaintenanceMapper = vehicleMaintenanceMapper;
    }

    public VehicleMaintenanceResponse create(UUID vehicleId, CreateVehicleMaintenanceRequest request) {
        log.info("Creating vehicle maintenance record: vehicleId={}, serviceDate={}, mileage={}, cost={}",
                vehicleId, request.serviceDate(), request.mileage(), request.cost());

        Vehicle vehicle = vehicleService.getEntity(vehicleId);
        VehicleMaintenance maintenance = vehicleMaintenanceMapper.toEntity(request, vehicle);

        VehicleMaintenance savedMaintenance = repository.save(maintenance);
        log.info("Vehicle maintenance record created successfully: maintenanceId={}, vehicleId={}, serviceDate={}",
                savedMaintenance.getId(), vehicleId, savedMaintenance.getServiceDate());

        return vehicleMaintenanceMapper.toResponse(savedMaintenance);
    }

    @Transactional(readOnly = true)
    public Page<VehicleMaintenanceResponse> getByVehicleId(UUID vehicleId, Pageable pageable) {
        log.debug("Fetching maintenance records by vehicle: vehicleId={}, page={}, size={}, sort={}",
                vehicleId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return repository.findByVehicleIdOrderByServiceDateDesc(vehicleId, pageable)
                .map(vehicleMaintenanceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleMaintenanceResponse getById(UUID id) {
        log.debug("Fetching vehicle maintenance record by id={}", id);
        return vehicleMaintenanceMapper.toResponse(getEntity(id));
    }

    public VehicleMaintenanceResponse update(UUID id, UpdateVehicleMaintenanceRequest request) {
        log.info("Updating vehicle maintenance record: maintenanceId={}, serviceDate={}, mileage={}, cost={}",
                id, request.serviceDate(), request.mileage(), request.cost());

        VehicleMaintenance maintenance = getEntity(id);
        vehicleMaintenanceMapper.mapForUpdate(request, maintenance);

        VehicleMaintenance savedMaintenance = repository.save(maintenance);
        log.info("Vehicle maintenance record updated successfully: maintenanceId={}, vehicleId={}, serviceDate={}",
                savedMaintenance.getId(), savedMaintenance.getVehicle().getId(), savedMaintenance.getServiceDate());

        return vehicleMaintenanceMapper.toResponse(savedMaintenance);
    }

    public void delete(UUID id) {
        log.info("Deleting vehicle maintenance record: maintenanceId={}", id);

        VehicleMaintenance maintenance = getEntity(id);
        repository.delete(maintenance);

        log.info("Vehicle maintenance record deleted successfully: maintenanceId={}", id);
    }

    public VehicleMaintenance getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle maintenance record not found: maintenanceId={}", id);
                    return new NotFoundException("Vehicle maintenance not found");
                });
    }
}