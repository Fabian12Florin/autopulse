package com.autopulse.fleet_service.web.mapper;

import com.autopulse.fleet_service.model.Depot;
import com.autopulse.fleet_service.model.FuelType;
import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleCategory;
import com.autopulse.fleet_service.model.VehicleStatus;
import com.autopulse.fleet_service.web.dto.vehicle.AvailableVehicleSummaryResponse;
import com.autopulse.fleet_service.web.dto.vehicle.CreateVehicleRequest;
import com.autopulse.fleet_service.web.dto.vehicle.UpdateVehicleRequest;
import com.autopulse.fleet_service.web.dto.vehicle.VehicleResponse;
import java.time.Year;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public void mapForCreate(CreateVehicleRequest request, Vehicle vehicle, Depot depot) {
        mapCommon(
                depot,
                request.licensePlate(),
                request.brand(),
                request.model(),
                request.vin(),
                request.year(),
                request.category(),
                request.fuelType(),
                request.capacityWeight(),
                request.capacityVolume(),
                vehicle
        );
        vehicle.setStatus(VehicleStatus.AVAILABLE);
    }

    public void mapForUpdate(UpdateVehicleRequest request, Vehicle vehicle, Depot depot) {
        mapCommon(
                depot,
                request.licensePlate(),
                request.brand(),
                request.model(),
                request.vin(),
                request.year(),
                request.category(),
                request.fuelType(),
                request.capacityWeight(),
                request.capacityVolume(),
                vehicle
        );
    }

    private void mapCommon(
            Depot depot,
            String licensePlate,
            String brand,
            String model,
            String vin,
            Integer year,
            VehicleCategory category,
            FuelType fuelType,
            Double capacityWeight,
            Double capacityVolume,
            Vehicle vehicle
    ) {
        vehicle.setDepot(depot);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setVin(vin);
        vehicle.setYear(Year.of(year));
        vehicle.setCategory(category);
        vehicle.setFuelType(fuelType);
        vehicle.setCapacityWeight(capacityWeight);
        vehicle.setCapacityVolume(capacityVolume);
    }

    public VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getDepot().getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVin(),
                vehicle.getYear() != null ? vehicle.getYear().getValue() : null,
                vehicle.getCategory(),
                vehicle.getFuelType(),
                vehicle.getCapacityWeight(),
                vehicle.getCapacityVolume(),
                vehicle.getStatus(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }

    public AvailableVehicleSummaryResponse toAvailableSummary(Vehicle vehicle) {
        return new AvailableVehicleSummaryResponse(
                vehicle.getId(),
                vehicle.getCapacityVolume(),
                vehicle.getCapacityWeight()
        );
    }
}