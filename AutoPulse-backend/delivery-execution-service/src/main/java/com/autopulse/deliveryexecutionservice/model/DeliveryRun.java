package com.autopulse.deliveryexecutionservice.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "delivery_runs")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryRun extends BaseEntity {

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "courier_id", nullable = false)
    private UUID courierId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "route_date", nullable = false)
    private LocalDate routeDate;

    @Column(name = "google_maps_url", nullable = false, length = 3000)
    private String googleMapsUrl;

    @Column(name = "total_parcel_count", nullable = false)
    private Integer totalParcelCount;

    @Column(name = "total_weight", nullable = false)
    private Double totalWeight;

    @Column(name = "total_volume", nullable = false)
    private Double totalVolume;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryRunStatus status = DeliveryRunStatus.NOT_STARTED;
}
