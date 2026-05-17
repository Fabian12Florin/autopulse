package com.autopulse.routingservice.service;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.routingservice.service.client.FleetClient;
import com.autopulse.routingservice.service.client.ParcelClient;
import com.autopulse.routingservice.service.client.UserClient;
import com.autopulse.routingservice.service.client.dto.FleetAvailableVehicleResponse;
import com.autopulse.routingservice.service.client.dto.FleetDepotResponse;
import com.autopulse.routingservice.service.client.dto.PageResponse;
import com.autopulse.routingservice.service.client.dto.ParcelRoutableResponse;
import com.autopulse.routingservice.service.client.dto.UserCourierResponse;
import com.autopulse.routingservice.web.dto.RoutingJobCreateRequest;
import com.autopulse.routingservice.web.dto.fleet.AvailableVehicleDto;
import com.autopulse.routingservice.web.dto.fleet.DepotDto;
import com.autopulse.routingservice.web.dto.parcel.RoutableParcelDto;
import com.autopulse.routingservice.web.dto.user.AvailableCourierDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingInputProvider {

    private static final int ROUTING_INPUT_LIMIT = 500;

    private final FleetClient fleetClient;
    private final UserClient userClient;
    private final ParcelClient parcelClient;

    public RoutingInput load(RoutingJobCreateRequest request) {
        //TODO: depotCode instead of depotId
        FleetDepotResponse depotResponse = fleetClient.getDepot(request.depotId());
        DepotDto depot = toDepotDto(depotResponse);

        List<AvailableVehicleDto> availableVehicles = toAvailableVehicles(
                fleetClient.getAvailableVehicles(request.depotId(), 0, ROUTING_INPUT_LIMIT)
        );
        List<AvailableCourierDto> availableCouriers = toAvailableCouriers(
                userClient.getAvailableCouriers(request.depotId(), 0, ROUTING_INPUT_LIMIT)
        );
        List<RoutableParcelDto> routableParcels = toRoutableParcels(
                parcelClient.getRoutableParcels(requiredDepotCode(depotResponse), ROUTING_INPUT_LIMIT, request.routeType())
        );

        validateInput(availableVehicles, availableCouriers, routableParcels);

        return new RoutingInput(depot, availableVehicles, availableCouriers, routableParcels);
    }

    private DepotDto toDepotDto(FleetDepotResponse depot) {
        if (depot == null || depot.id() == null) {
            throw new BadRequestException("Depot response is missing required data.");
        }

        return new DepotDto(
                depot.id(),
                depot.depotCode(),
                depot.latitude().doubleValue(),
                depot.longitude().doubleValue()
        );
    }

    private List<AvailableVehicleDto> toAvailableVehicles(PageResponse<FleetAvailableVehicleResponse> response) {
        return safeContent(response).stream()
                .map(vehicle -> new AvailableVehicleDto(
                        vehicle.id(),
                        positiveInt(vehicle.capacityWeight(), "Vehicle capacityWeight"),
                        positiveInt(vehicle.capacityVolume(), "Vehicle capacityVolume")
                ))
                .toList();
    }

    private List<AvailableCourierDto> toAvailableCouriers(PageResponse<UserCourierResponse> response) {
        return safeContent(response).stream()
                .filter(courier -> courier.courierProfileId() != null)
                .map(courier -> new AvailableCourierDto(courier.courierProfileId()))
                .toList();
    }

    private List<RoutableParcelDto> toRoutableParcels(List<ParcelRoutableResponse> parcels) {
        return (parcels == null ? List.<ParcelRoutableResponse>of() : parcels).stream()
                .map(this::toRoutableParcel)
                .toList();
    }

    private RoutableParcelDto toRoutableParcel(ParcelRoutableResponse parcel) {
        return new RoutableParcelDto(
                parcel.parcelId(),
                parcel.awb(),
                parcel.receiverName(),
                parcel.receiverPhone(),
                requiredDouble(parcel.receiverLatitude(), "Parcel latitude"),
                requiredDouble(parcel.receiverLongitude(), "Parcel longitude"),
                positiveInt(parcel.weight(), "Parcel weight"),
                positiveInt(parcel.volume(), "Parcel volume")
        );
    }

    private void validateInput(
            List<AvailableVehicleDto> availableVehicles,
            List<AvailableCourierDto> availableCouriers,
            List<RoutableParcelDto> routableParcels
    ) {
        if (availableVehicles.isEmpty()) {
            throw new BadRequestException("No available vehicles found for routing.");
        }
        if (availableCouriers.isEmpty()) {
            throw new BadRequestException("No available couriers found for routing.");
        }
        if (routableParcels.isEmpty()) {
            throw new BadRequestException("No routable parcels found for routing.");
        }
    }

    private String requiredDepotCode(FleetDepotResponse depot) {
        String value = depot.depotCode();
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Depot code" + " is required for routing.");
        }
        return value.trim();
    }

    private double requiredDouble(Double value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " is required for routing.");
        }
        return value;
    }

    private int positiveInt(Double value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BadRequestException(fieldName + " must be greater than zero.");
        }
        return (int) Math.ceil(value);
    }

    private <T> List<T> safeContent(PageResponse<T> response) {
        if (response == null || response.content() == null) {
            return List.of();
        }
        return response.content();
    }
}
