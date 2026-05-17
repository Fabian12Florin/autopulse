package com.autopulse.routingservice.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.routingservice.model.enums.RouteStopStatus;
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
@Table(name = "route_stop")
@Getter
@Setter
@NoArgsConstructor
public class RouteStop extends BaseEntity {

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RouteStopStatus routeStopStatus;

    @Column(name = "parcel_count")
    private Integer parcelCount;

    @Column(name = "total_weight")
    private Double totalWeight;

    @Column(name = "total_volume")
    private Double totalVolume;
}
