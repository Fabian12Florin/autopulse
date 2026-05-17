package com.autopulse.routingservice.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.routingservice.model.enums.RouteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "route")
@Getter
@Setter
@NoArgsConstructor
public class Route extends BaseEntity {

    @Column(name = "routing_job_id", nullable = false)
    private UUID routingJobId;

    @Column(name = "courier_id", nullable = false)
    private UUID courierId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RouteStatus routeStatus;

    @Column(name = "google_maps_url", length = 2000)
    private String googleMapsUrl;

    @Column(name = "total_parcel_count")
    private Integer totalParcelCount;

    @Column(name = "total_weight")
    private Double totalWeight;

    @Column(name = "total_volume")
    private Double totalVolume;
}
