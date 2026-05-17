package com.autopulse.routingservice.service.component;

import com.autopulse.routingservice.vroom.dto.route.PlannedRoute;
import com.autopulse.routingservice.vroom.dto.route.PlannedStop;
import com.autopulse.routingservice.model.Route;
import com.autopulse.routingservice.model.enums.RouteStatus;
import com.autopulse.routingservice.model.RouteStop;
import com.autopulse.routingservice.model.RouteStopParcel;
import com.autopulse.routingservice.model.RoutingJob;
import com.autopulse.routingservice.model.enums.RouteStopStatus;
import com.autopulse.routingservice.model.enums.RoutingJobStatus;
import com.autopulse.routingservice.repository.RouteRepository;
import com.autopulse.routingservice.repository.RouteStopParcelRepository;
import com.autopulse.routingservice.repository.RouteStopRepository;
import com.autopulse.routingservice.repository.RoutingJobRepository;
import com.autopulse.routingservice.web.dto.RoutingJobCreateRequest;
import com.autopulse.routingservice.vroom.dto.response.VroomResponse;
import com.autopulse.routingservice.web.dto.parcel.RoutableParcelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoutingJobWriter {

    private final RoutingJobRepository routingJobRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final RouteStopParcelRepository routeStopParcelRepository;

    private final CurrentUserProvider currentUserProvider;

    public RoutingJob createRoutingJob(
            RoutingJobCreateRequest request,
            int inputCourierCount,
            int inputVehicleCount,
            int inputParcelCount,
            VroomResponse vroomResponse
    ) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        int unassignedParcelCount = extractUnassignedCount(vroomResponse);

        RoutingJob routingJob = new RoutingJob();
        routingJob.setGeneratedByUserId(currentUserId);
        routingJob.setDepotId(request.depotId());
        routingJob.setRouteDate(request.routeDate());
        routingJob.setRouteType(request.routeType());
        routingJob.setRoutingJobStatus(RoutingJobStatus.PENDING);
        routingJob.setInputParcelCount(inputParcelCount);
        routingJob.setInputCourierCount(inputCourierCount);
        routingJob.setInputVehicleCount(inputVehicleCount);
        routingJob.setAssignedParcelCount(inputParcelCount - unassignedParcelCount);
        routingJob.setUnassignedParcelCount(unassignedParcelCount);
        routingJob.setNumberOfRoutes(vroomResponse.routes() != null ? vroomResponse.routes().size() : 0);
        return routingJobRepository.save(routingJob);
    }

    public void savePlannedRoutes(RoutingJob routingJob, List<PlannedRoute> plannedRoutes) {
        for (PlannedRoute plannedRoute : plannedRoutes) {
            Route savedRoute = routeRepository.save(createRoute(routingJob, plannedRoute));
            savePlannedStops(savedRoute, plannedRoute.stops());
        }
    }

    private Route createRoute(RoutingJob routingJob, PlannedRoute plannedRoute) {
        Route route = new Route();
        route.setRoutingJobId(routingJob.getId());
        route.setCourierId(plannedRoute.courierId());
        route.setVehicleId(plannedRoute.vehicleId());
        route.setRouteStatus(RouteStatus.PLANNED);
        route.setGoogleMapsUrl(plannedRoute.googleMapsUrl());
        route.setTotalParcelCount(plannedRoute.totalParcelCount());
        route.setTotalWeight((double) plannedRoute.totalWeight());
        route.setTotalVolume((double) plannedRoute.totalVolume());
        return route;
    }

    private void savePlannedStops(Route route, List<PlannedStop> plannedStops) {
        for (PlannedStop plannedStop : plannedStops) {
            RouteStop savedStop = routeStopRepository.save(createRouteStop(route, plannedStop));

            plannedStop.parcels().stream()
                    .map(parcel -> createRouteStopParcel(savedStop, parcel))
                    .forEach(routeStopParcelRepository::save);
        }
    }

    private RouteStop createRouteStop(Route route, PlannedStop plannedStop) {
        RouteStop routeStop = new RouteStop();
        routeStop.setRouteId(route.getId());
        routeStop.setStopOrder(plannedStop.stopOrder());
        routeStop.setLatitude(plannedStop.latitude());
        routeStop.setLongitude(plannedStop.longitude());
        routeStop.setRouteStopStatus(RouteStopStatus.PENDING);
        routeStop.setParcelCount(plannedStop.parcelCount());
        routeStop.setTotalWeight((double) plannedStop.totalWeight());
        routeStop.setTotalVolume((double) plannedStop.totalVolume());
        return routeStop;
    }

    private RouteStopParcel createRouteStopParcel(RouteStop routeStop, RoutableParcelDto parcel) {
        RouteStopParcel routeStopParcel = new RouteStopParcel();
        routeStopParcel.setRouteStopId(routeStop.getId());
        routeStopParcel.setParcelId(parcel.parcelId());
        routeStopParcel.setAwb(parcel.awb());
        routeStopParcel.setReceiverName(parcel.receiverName());
        routeStopParcel.setReceiverPhone(parcel.receiverPhone());
        routeStopParcel.setWeight(parcel.weight());
        routeStopParcel.setVolume(parcel.volume());
        return routeStopParcel;
    }

    private int extractUnassignedCount(VroomResponse vroomResponse) {
        if (vroomResponse.summary() != null && vroomResponse.summary().unassigned() != null) {
            return vroomResponse.summary().unassigned();
        }
        return 0;
    }
}
