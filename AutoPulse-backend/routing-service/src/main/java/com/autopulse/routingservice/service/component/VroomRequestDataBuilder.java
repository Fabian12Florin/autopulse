package com.autopulse.routingservice.service.component;

import com.autopulse.routingservice.vroom.dto.request.VroomRequestData;
import com.autopulse.routingservice.web.dto.fleet.AvailableVehicleDto;
import com.autopulse.routingservice.web.dto.fleet.DepotDto;
import com.autopulse.routingservice.web.dto.parcel.RoutableParcelDto;
import com.autopulse.routingservice.web.dto.user.AvailableCourierDto;
import com.autopulse.routingservice.vroom.dto.mapping.SelectedParcelJob;
import com.autopulse.routingservice.vroom.dto.mapping.SelectedVehicleCourier;
import com.autopulse.routingservice.vroom.dto.request.VroomJob;
import com.autopulse.routingservice.vroom.dto.request.VroomRequest;
import com.autopulse.routingservice.vroom.dto.request.VroomVehicle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VroomRequestDataBuilder {

    public VroomRequestData createVroomRequestData(
            DepotDto depot,
            List<AvailableVehicleDto> vehicles,
            List<AvailableCourierDto> couriers,
            List<RoutableParcelDto> parcels
    ) {
        int usableVehicleCount = calculateUsableVehicleCount(vehicles, couriers);

        List<SelectedVehicleCourier> vehicleCourierMappings =
                buildVehicleCourierMappings(vehicles, couriers, usableVehicleCount);

        List<VroomVehicle> vroomVehicles =
                buildVroomVehicles(depot, vehicles, usableVehicleCount);

        List<SelectedParcelJob> jobMappings =
                buildJobMappings(parcels);

        List<VroomJob> vroomJobs =
                buildVroomJobs(parcels);

        VroomRequest request = new VroomRequest(vroomVehicles, vroomJobs);

        return new VroomRequestData(request, vehicleCourierMappings, jobMappings);
    }

    private int calculateUsableVehicleCount(
            List<AvailableVehicleDto> vehicles,
            List<AvailableCourierDto> couriers
    ) {
        return Math.min(vehicles.size(), couriers.size());
    }

    private List<SelectedVehicleCourier> buildVehicleCourierMappings(
            List<AvailableVehicleDto> vehicles,
            List<AvailableCourierDto> couriers,
            int usableVehicleCount
    ) {
        List<SelectedVehicleCourier> mappings = new ArrayList<>();

        for (int i = 0; i < usableVehicleCount; i++) {
            AvailableVehicleDto vehicle = vehicles.get(i);
            AvailableCourierDto courier = couriers.get(i);

            mappings.add(new SelectedVehicleCourier(
                    i,
                    vehicle.vehicleId(),
                    courier.courierId()
            ));
        }

        return mappings;
    }

    private List<VroomVehicle> buildVroomVehicles(
            DepotDto depot,
            List<AvailableVehicleDto> vehicles,
            int usableVehicleCount
    ) {
        List<VroomVehicle> vroomVehicles = new ArrayList<>();

        for (int i = 0; i < usableVehicleCount; i++) {
            AvailableVehicleDto vehicle = vehicles.get(i);

            vroomVehicles.add(buildSingleVehicle(depot, vehicle, i));
        }

        return vroomVehicles;
    }

    private VroomVehicle buildSingleVehicle(
            DepotDto depot,
            AvailableVehicleDto vehicle,
            long vroomVehicleId
    ) {
        return new VroomVehicle(
                vroomVehicleId,
                buildDepotLocation(depot),
                buildDepotLocation(depot),
                buildVehicleCapacity(vehicle)
        );
    }

    private List<SelectedParcelJob> buildJobMappings(List<RoutableParcelDto> parcels) {
        List<SelectedParcelJob> mappings = new ArrayList<>();

        for (int i = 0; i < parcels.size(); i++) {
            RoutableParcelDto parcel = parcels.get(i);

            mappings.add(new SelectedParcelJob(
                    i,
                    parcel.parcelId()
            ));
        }

        return mappings;
    }

    private List<VroomJob> buildVroomJobs(List<RoutableParcelDto> parcels) {
        List<VroomJob> vroomJobs = new ArrayList<>();

        for (int i = 0; i < parcels.size(); i++) {
            RoutableParcelDto parcel = parcels.get(i);

            vroomJobs.add(buildSingleJob(parcel, i));
        }

        return vroomJobs;
    }

    private VroomJob buildSingleJob(RoutableParcelDto parcel, long vroomJobId) {
        return new VroomJob(
                vroomJobId,
                buildParcelLocation(parcel),
                buildParcelCapacity(parcel)
        );
    }

    private List<Double> buildDepotLocation(DepotDto depot) {
        return List.of(depot.longitude(), depot.latitude());
    }

    private List<Double> buildParcelLocation(RoutableParcelDto parcel) {
        return List.of(parcel.deliveryLongitude(), parcel.deliveryLatitude());
    }

    private List<Integer> buildVehicleCapacity(AvailableVehicleDto vehicle) {
        return List.of(
                safeInt(vehicle.capacityWeight()),
                safeInt(vehicle.capacityVolume())
        );
    }

    private List<Integer> buildParcelCapacity(RoutableParcelDto parcel) {
        return List.of(
                safeInt(parcel.weight()),
                safeInt(parcel.volume())
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
