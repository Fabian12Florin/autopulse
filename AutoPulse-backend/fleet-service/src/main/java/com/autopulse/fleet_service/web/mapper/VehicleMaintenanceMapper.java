package com.autopulse.fleet_service.web.mapper;

import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleMaintenance;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.CreateVehicleMaintenanceRequest;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.UpdateVehicleMaintenanceRequest;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.VehicleMaintenanceResponse;
import org.springframework.stereotype.Component;

@Component
public class VehicleMaintenanceMapper {

    public VehicleMaintenance toEntity(CreateVehicleMaintenanceRequest request, Vehicle vehicle) {
        VehicleMaintenance maintenance = new VehicleMaintenance();
        maintenance.setVehicle(vehicle);
        maintenance.setMileage(request.mileage());
        maintenance.setServiceDate(request.serviceDate());
        maintenance.setCost(request.cost());
        maintenance.setDescription(request.description());
        maintenance.setServiceProvider(request.serviceProvider());
        return maintenance;
    }

    public void mapForUpdate(UpdateVehicleMaintenanceRequest request, VehicleMaintenance maintenance) {
        maintenance.setMileage(request.mileage());
        maintenance.setServiceDate(request.serviceDate());
        maintenance.setCost(request.cost());
        maintenance.setDescription(request.description());
        maintenance.setServiceProvider(request.serviceProvider());
    }

    public VehicleMaintenanceResponse toResponse(VehicleMaintenance maintenance) {
        return new VehicleMaintenanceResponse(
                maintenance.getId(),
                maintenance.getVehicle().getId(),
                maintenance.getMileage(),
                maintenance.getServiceDate(),
                maintenance.getCost(),
                maintenance.getDescription(),
                maintenance.getServiceProvider(),
                maintenance.getCreatedAt(),
                maintenance.getUpdatedAt()
        );
    }
}