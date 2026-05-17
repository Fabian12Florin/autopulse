package com.autopulse.deliveryexecutionservice.mapper;

import com.autopulse.deliveryexecutionservice.model.DeliveryParcelExecution;
import com.autopulse.deliveryexecutionservice.model.DeliveryRun;
import com.autopulse.deliveryexecutionservice.model.DeliveryStopExecution;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryStopStatus;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryExecution.DeliveryParcelExecutionDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.DeliveryRunDetailsDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.DeliveryRunDto;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryRun.DeliveryStopExecutionDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DeliveryExecutionMapper {

    private static final ZoneId BUCHAREST_ZONE = ZoneId.of("Europe/Bucharest");

    public DeliveryRunDto toDto(DeliveryRun entity) {
        return new DeliveryRunDto(
                entity.getId(),
                entity.getRouteId(),
                entity.getCourierId(),
                entity.getVehicleId(),
                entity.getRouteDate(),
                entity.getGoogleMapsUrl(),
                entity.getTotalParcelCount(),
                entity.getTotalWeight(),
                entity.getTotalVolume(),
                entity.getStatus(),
                toBucharest(entity.getStartedAt()),
                toBucharest(entity.getFinishedAt())
        );
    }

    public DeliveryParcelExecutionDto toDto(DeliveryParcelExecution entity) {
        return new DeliveryParcelExecutionDto(
                entity.getId(),
                entity.getParcelId(),
                entity.getAwb(),
                entity.getReceiverName(),
                entity.getReceiverPhone(),
                entity.getWeight(),
                entity.getVolume(),
                entity.getOutcome(),
                entity.getDeliveryCodeVerified(),
                toBucharest(entity.getCompletedAt())
        );
    }

    public DeliveryRunDetailsDto toDetailsDto(
            DeliveryRun entity,
            List<DeliveryStopExecution> stops,
            Map<UUID, List<DeliveryParcelExecution>> parcelsByStopId
    ) {
        List<DeliveryStopExecutionDto> stopDtos = stops.stream()
                .map(stop -> toDto(stop, parcelsByStopId.getOrDefault(stop.getId(), List.of())))
                .toList();

        DeliveryStopExecutionDto currentStop = stopDtos.stream()
                .filter(stop -> stop.status() != DeliveryStopStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        return new DeliveryRunDetailsDto(
                entity.getId(),
                entity.getRouteId(),
                entity.getCourierId(),
                entity.getVehicleId(),
                entity.getRouteDate(),
                entity.getGoogleMapsUrl(),
                entity.getTotalParcelCount(),
                entity.getTotalWeight(),
                entity.getTotalVolume(),
                entity.getStatus(),
                currentStop,
                stopDtos
        );
    }

    public DeliveryStopExecutionDto toDto(
            DeliveryStopExecution entity,
            List<DeliveryParcelExecution> parcels
    ) {
        return new DeliveryStopExecutionDto(
                entity.getId(),
                entity.getRouteStopId(),
                entity.getStopOrder(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getParcelCount(),
                entity.getTotalWeight(),
                entity.getTotalVolume(),
                entity.getStatus(),
                parcels.stream().map(this::toDto).toList()
        );
    }

    private OffsetDateTime toBucharest(Instant instant) {
        return instant == null ? null : instant.atZone(BUCHAREST_ZONE).toOffsetDateTime();
    }
}
