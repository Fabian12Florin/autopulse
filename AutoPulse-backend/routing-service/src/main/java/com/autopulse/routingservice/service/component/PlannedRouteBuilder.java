package com.autopulse.routingservice.service.component;

import com.autopulse.routingservice.vroom.dto.route.PlannedRoute;
import com.autopulse.routingservice.vroom.dto.route.PlannedStop;
import com.autopulse.routingservice.vroom.dto.request.VroomRequestData;
import com.autopulse.routingservice.web.dto.parcel.RoutableParcelDto;
import com.autopulse.routingservice.vroom.dto.mapping.SelectedParcelJob;
import com.autopulse.routingservice.vroom.dto.mapping.SelectedVehicleCourier;
import com.autopulse.routingservice.vroom.dto.response.VroomResponse;
import com.autopulse.routingservice.vroom.dto.response.VroomRoute;
import com.autopulse.routingservice.vroom.dto.response.VroomStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PlannedRouteBuilder {

    private static final String GOOGLE_MAPS_DIRECTIONS_URL = "https://www.google.com/maps/dir/";

    public List<PlannedRoute> createPlannedRoutes(
            VroomResponse vroomResponse,
            VroomRequestData vroomRequestData,
            List<RoutableParcelDto> routableParcels
    ) {
        if (vroomResponse.routes() == null) {
            return List.of();
        }

        Map<Long, SelectedVehicleCourier> vehicleCourierByVroomId = vroomRequestData.vehicleMappings().stream()
                .collect(Collectors.toMap(SelectedVehicleCourier::vroomVehicleId, Function.identity()));

        Map<Long, UUID> parcelIdByJobId = vroomRequestData.jobMappings().stream()
                .collect(Collectors.toMap(SelectedParcelJob::vroomJobId, SelectedParcelJob::parcelId));

        Map<UUID, RoutableParcelDto> parcelById = routableParcels.stream()
                .collect(Collectors.toMap(RoutableParcelDto::parcelId, Function.identity()));

        return vroomResponse.routes().stream()
                .map(route -> createPlannedRoute(route, vehicleCourierByVroomId, parcelIdByJobId, parcelById))
                .toList();
    }

    private PlannedRoute createPlannedRoute(
            VroomRoute vroomRoute,
            Map<Long, SelectedVehicleCourier> vehicleCourierByVroomId,
            Map<Long, UUID> parcelIdByJobId,
            Map<UUID, RoutableParcelDto> parcelById
    ) {
        SelectedVehicleCourier assignedVehicle = vehicleCourierByVroomId.get(vroomRoute.vehicle());
        List<PlannedStop> plannedStops = createPlannedStops(vroomRoute.steps(), parcelIdByJobId, parcelById);

        return new PlannedRoute(
                assignedVehicle == null ? null : assignedVehicle.vehicleId(),
                assignedVehicle == null ? null : assignedVehicle.courierId(),
                sumParcelCount(plannedStops),
                sumWeight(plannedStops),
                sumVolume(plannedStops),
                buildGoogleMapsUrl(vroomRoute.steps(), plannedStops),
                plannedStops
        );
    }

    private List<PlannedStop> createPlannedStops(
            List<VroomStep> steps,
            Map<Long, UUID> parcelIdByJobId,
            Map<UUID, RoutableParcelDto> parcelById
    ) {
        if (steps == null) {
            return List.of();
        }

        List<PlannedStop> plannedStops = new ArrayList<>();
        for (VroomStep step : steps) {
            if (!"job".equalsIgnoreCase(step.type()) || step.job() == null) {
                continue;
            }

            UUID parcelId = parcelIdByJobId.get(step.job());
            RoutableParcelDto parcel = parcelById.get(parcelId);
            if (parcel == null) {
                continue;
            }

            plannedStops.add(createPlannedStop(plannedStops.size() + 1, parcel));
        }

        return plannedStops;
    }

    private PlannedStop createPlannedStop(int stopOrder, RoutableParcelDto parcel) {
        return new PlannedStop(
                stopOrder,
                parcel.deliveryLatitude(),
                parcel.deliveryLongitude(),
                1,
                safeInt(parcel.weight()),
                safeInt(parcel.volume()),
                List.of(parcel)
        );
    }

    private String buildGoogleMapsUrl(List<VroomStep> steps, List<PlannedStop> plannedStops) {
        List<String> points = new ArrayList<>();
        addDepotPoint(points, steps, "start");
        plannedStops.stream()
                .map(stop -> stop.latitude() + "," + stop.longitude())
                .forEach(points::add);
        addDepotPoint(points, steps, "end");

        if (points.size() < 2) {
            return null;
        }

        return GOOGLE_MAPS_DIRECTIONS_URL + String.join("/", points);
    }

    private void addDepotPoint(List<String> points, List<VroomStep> steps, String stepType) {
        if (steps == null) {
            return;
        }

        steps.stream()
                .filter(step -> stepType.equalsIgnoreCase(step.type()))
                .filter(this::hasLocation)
                .findFirst()
                .ifPresent(step -> points.add(step.location().get(1) + "," + step.location().getFirst()));
    }

    private boolean hasLocation(VroomStep step) {
        return step.location() != null && step.location().size() == 2;
    }

    private int sumParcelCount(List<PlannedStop> plannedStops) {
        return plannedStops.stream().map(PlannedStop::parcelCount).reduce(0, Integer::sum);
    }

    private int sumWeight(List<PlannedStop> plannedStops) {
        return plannedStops.stream().map(PlannedStop::totalWeight).reduce(0, Integer::sum);
    }

    private int sumVolume(List<PlannedStop> plannedStops) {
        return plannedStops.stream().map(PlannedStop::totalVolume).reduce(0, Integer::sum);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
