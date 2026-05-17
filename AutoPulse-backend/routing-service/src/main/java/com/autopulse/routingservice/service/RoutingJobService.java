package com.autopulse.routingservice.service;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.routingservice.mapper.RoutingJobMapper;
import com.autopulse.routingservice.model.RoutingJob;
import com.autopulse.routingservice.model.Route;
import com.autopulse.routingservice.model.RouteStop;
import com.autopulse.routingservice.model.RouteStopParcel;
import com.autopulse.routingservice.model.enums.RouteStatus;
import com.autopulse.routingservice.model.enums.RoutingJobStatus;
import com.autopulse.routingservice.repository.RouteRepository;
import com.autopulse.routingservice.repository.RouteStopParcelRepository;
import com.autopulse.routingservice.repository.RouteStopRepository;
import com.autopulse.routingservice.repository.RoutingJobRepository;
import com.autopulse.routingservice.repository.specification.RoutingJobSpecification;
import com.autopulse.routingservice.service.client.DeliveryExecutionClient;
import com.autopulse.routingservice.service.component.PlannedRouteBuilder;
import com.autopulse.routingservice.service.component.RoutingJobWriter;
import com.autopulse.routingservice.service.client.VroomClient;
import com.autopulse.routingservice.service.component.VroomRequestDataBuilder;
import com.autopulse.routingservice.vroom.dto.request.VroomRequestData;
import com.autopulse.routingservice.vroom.dto.route.PlannedRoute;
import com.autopulse.routingservice.web.dto.RoutingJobCreateRequest;
import com.autopulse.routingservice.web.dto.RoutingJobQueryRequest;
import com.autopulse.routingservice.web.dto.RoutingJobResponse;
import com.autopulse.routingservice.web.dto.delivery.CreateDeliveryParcelRequest;
import com.autopulse.routingservice.web.dto.delivery.CreateDeliveryRunRequest;
import com.autopulse.routingservice.web.dto.delivery.CreateDeliveryStopRequest;
import com.autopulse.routingservice.vroom.dto.response.VroomResponse;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoutingJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingJobService.class);

    private final VroomRequestDataBuilder vroomRequestDataBuilder;
    private final VroomClient vroomClient;
    private final PlannedRouteBuilder plannedRouteBuilder;
    private final RoutingJobWriter routingJobWriter;
    private final RoutingInputProvider routingInputProvider;

    private final RoutingJobRepository routingJobRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final RouteStopParcelRepository routeStopParcelRepository;
    private final DeliveryExecutionClient deliveryExecutionClient;
    private final TransactionTemplate transactionTemplate;

    private final RoutingJobMapper routingJobMapper;

    @Transactional
    public RoutingJobResponse createRoutingJob(RoutingJobCreateRequest request) {
        RoutingInput routingInput = routingInputProvider.load(request);

        // Request for vroom app
        VroomRequestData vroomRequestData = vroomRequestDataBuilder.createVroomRequestData(
                routingInput.depot(),
                routingInput.availableVehicles(),
                routingInput.availableCouriers(),
                routingInput.routableParcels()
        );

        // Saving vroom request/response files
        Path requestPath = vroomClient.saveRequest(vroomRequestData.request());
        VroomResponse vroomResponse = vroomClient.call(vroomRequestData.request());
        Path responsePath = vroomClient.saveResponse(vroomResponse);

        // Getting planned routes from vroom response
        List<PlannedRoute> plannedRoutes = plannedRouteBuilder.createPlannedRoutes(
                vroomResponse,
                vroomRequestData,
                routingInput.routableParcels()
        );

        // Logging planned routes and file paths for debugging
        logPlannedRoutes(requestPath, responsePath, plannedRoutes);

        // Creating routing job and saving planned routes to DB
        RoutingJob routingJob = routingJobWriter.createRoutingJob(
                request,
                routingInput.availableCouriers().size(),
                routingInput.availableVehicles().size(),
                routingInput.routableParcels().size(),
                vroomResponse
        );

        routingJobWriter.savePlannedRoutes(routingJob, plannedRoutes);

        return toResponse(routingJob);
    }

    public RoutingJobResponse selectRoutingJob(UUID routingJobId) {
        RoutingJobSelectionPlan selectionPlan = transactionTemplate.execute(status -> buildSelectionPlan(routingJobId));
        if (selectionPlan == null) {
            throw new IllegalStateException("Failed to build routing job selection plan");
        }

        if (selectionPlan.alreadySelectedResponse() != null) {
            return selectionPlan.alreadySelectedResponse();
        }

        publishDeliveryRuns(selectionPlan.deliveryRuns());

        RoutingJobResponse selectedResponse = transactionTemplate.execute(status -> markRoutingJobSelected(routingJobId));
        if (selectedResponse == null) {
            throw new IllegalStateException("Failed to mark routing job as selected");
        }

        return selectedResponse;
    }

    private RoutingJobSelectionPlan buildSelectionPlan(UUID routingJobId) {
        RoutingJob routingJob = routingJobRepository.findById(routingJobId)
                .orElseThrow(() -> new NotFoundException("Routing job not found: " + routingJobId));

        if (routingJob.getRoutingJobStatus() == RoutingJobStatus.SELECTED) {
            return RoutingJobSelectionPlan.alreadySelected(toResponse(routingJob));
        }

        if (routingJob.getRoutingJobStatus() != RoutingJobStatus.PENDING) {
            throw new BadRequestException("Routing job can be selected only from PENDING status");
        }

        List<Route> routes = routeRepository.findByRoutingJobId(routingJobId);

        if (routes.isEmpty()) {
            throw new BadRequestException("Routing job has no planned routes");
        }

        return RoutingJobSelectionPlan.pending(
                routes.stream()
                        .map(route -> buildDeliveryRunRequest(routingJob, route))
                        .toList()
        );
    }

    private RoutingJobResponse markRoutingJobSelected(UUID routingJobId) {
        RoutingJob routingJob = routingJobRepository.findById(routingJobId)
                .orElseThrow(() -> new NotFoundException("Routing job not found: " + routingJobId));

        if (routingJob.getRoutingJobStatus() == RoutingJobStatus.SELECTED) {
            return toResponse(routingJob);
        }

        if (routingJob.getRoutingJobStatus() != RoutingJobStatus.PENDING) {
            throw new BadRequestException("Routing job can be selected only from PENDING status");
        }

        List<Route> routes = routeRepository.findByRoutingJobId(routingJobId);
        if (routes.isEmpty()) {
            throw new BadRequestException("Routing job has no planned routes");
        }

        routingJob.setRoutingJobStatus(RoutingJobStatus.SELECTED);

        for (Route route : routes) {
            route.setRouteStatus(RouteStatus.PUBLISHED);
        }

        routingJobRepository.save(routingJob);
        routeRepository.saveAll(routes);

        return toResponse(routingJob);
    }

    private void publishDeliveryRuns(List<CreateDeliveryRunRequest> deliveryRuns) {
        for (CreateDeliveryRunRequest request : deliveryRuns) {
            try {
                deliveryExecutionClient.createDeliveryRun(request);
            } catch (FeignException.Conflict conflict) {
                LOGGER.info("Delivery run for route {} already exists; treating it as published", request.routeId());
            }
        }
    }

    private RoutingJobResponse toResponse(RoutingJob routingJob) {
        return new RoutingJobResponse(
                routingJob.getId(),
                routingJob.getGeneratedByUserId(),
                routingJob.getDepotId(),
                routingJob.getRouteDate(),
                routingJob.getRouteType(),
                routingJob.getRoutingJobStatus(),
                routingJob.getInputParcelCount(),
                routingJob.getInputCourierCount(),
                routingJob.getInputVehicleCount(),
                routingJob.getAssignedParcelCount(),
                routingJob.getUnassignedParcelCount(),
                routingJob.getNumberOfRoutes()
        );
    }

    private CreateDeliveryRunRequest buildDeliveryRunRequest(RoutingJob routingJob, Route route) {
        return new CreateDeliveryRunRequest(
                route.getId(),
                route.getCourierId(),
                route.getVehicleId(),
                routingJob.getRouteDate(),
                route.getGoogleMapsUrl(),
                route.getTotalParcelCount(),
                route.getTotalWeight(),
                route.getTotalVolume(),
                buildDeliveryStops(route.getId())
        );
    }

    private List<CreateDeliveryStopRequest> buildDeliveryStops(UUID routeId) {
        return routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId).stream()
                .map(this::buildDeliveryStop)
                .toList();
    }

    private CreateDeliveryStopRequest buildDeliveryStop(RouteStop routeStop) {
        return new CreateDeliveryStopRequest(
                routeStop.getId(),
                routeStop.getStopOrder(),
                routeStop.getLatitude(),
                routeStop.getLongitude(),
                routeStop.getParcelCount(),
                routeStop.getTotalWeight(),
                routeStop.getTotalVolume(),
                buildDeliveryParcels(routeStop.getId())
        );
    }

    private List<CreateDeliveryParcelRequest> buildDeliveryParcels(UUID routeStopId) {
        return routeStopParcelRepository.findByRouteStopId(routeStopId).stream()
                .map(this::buildDeliveryParcel)
                .toList();
    }

    private CreateDeliveryParcelRequest buildDeliveryParcel(RouteStopParcel routeStopParcel) {
        return new CreateDeliveryParcelRequest(
                routeStopParcel.getParcelId(),
                routeStopParcel.getAwb(),
                routeStopParcel.getReceiverName(),
                routeStopParcel.getReceiverPhone(),
                routeStopParcel.getWeight(),
                routeStopParcel.getVolume()
        );
    }

    private void logPlannedRoutes(Path requestPath, Path responsePath, List<PlannedRoute> plannedRoutes) {
        LOGGER.info("Saved VROOM request to: {}", requestPath);
        LOGGER.info("Saved VROOM response to: {}", responsePath);
        LOGGER.info("Built planned routes: {}", plannedRoutes.size());

        for (int i = 0; i < plannedRoutes.size(); i++) {
            PlannedRoute route = plannedRoutes.get(i);
            LOGGER.info(
                    "Route {}: vehicleId={}, courierId={}, totalParcels={}, totalWeight={}, totalVolume={}, stopCount={}, googleMapsUrl={}",
                    i + 1,
                    route.vehicleId(),
                    route.courierId(),
                    route.totalParcelCount(),
                    route.totalWeight(),
                    route.totalVolume(),
                    route.stops().size(),
                    route.googleMapsUrl()
            );
        }
    }

    public Page<RoutingJobResponse> queryRoutingJobs(RoutingJobQueryRequest request, Pageable pageable) {
        return routingJobRepository
                .findAll(RoutingJobSpecification.withFilters(request), pageable)
                .map(routingJobMapper::toResponse);
    }

    private record RoutingJobSelectionPlan(
            RoutingJobResponse alreadySelectedResponse,
            List<CreateDeliveryRunRequest> deliveryRuns
    ) {
        private static RoutingJobSelectionPlan alreadySelected(RoutingJobResponse response) {
            return new RoutingJobSelectionPlan(response, List.of());
        }

        private static RoutingJobSelectionPlan pending(List<CreateDeliveryRunRequest> deliveryRuns) {
            return new RoutingJobSelectionPlan(null, deliveryRuns);
        }
    }

}
