package com.autopulse.deliveryexecutionservice.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryStopStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_stop_executions")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryStopExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_run_id", nullable = false)
    private DeliveryRun deliveryRun;

    @Column(name = "route_stop_id", nullable = false)
    private UUID routeStopId;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "parcel_count", nullable = false)
    private Integer parcelCount;

    @Column(name = "total_weight", nullable = false)
    private Double totalWeight;

    @Column(name = "total_volume", nullable = false)
    private Double totalVolume;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStopStatus status = DeliveryStopStatus.PENDING;

    @Column(name = "arrived_at")
    private Instant arrivedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
