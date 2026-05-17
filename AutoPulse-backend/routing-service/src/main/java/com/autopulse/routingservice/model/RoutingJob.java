package com.autopulse.routingservice.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.routingservice.model.enums.RouteType;
import com.autopulse.routingservice.model.enums.RoutingJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "routing_jobs")
@Getter
@Setter
@NoArgsConstructor
public class RoutingJob extends BaseEntity {

    @Column(name = "generated_by_user_id", nullable = false)
    private UUID generatedByUserId;

    @Column(name = "depot_id", nullable = false)
    private UUID depotId;

    @Column(name = "route_date", nullable = false)
    private LocalDate routeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false)
    private RouteType routeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_job_status", nullable = false)
    private RoutingJobStatus routingJobStatus;

    @Column(name = "input_parcel_count", nullable = false)
    private Integer inputParcelCount;

    @Column(name = "input_courier_count", nullable = false)
    private Integer inputCourierCount;

    @Column(name = "input_vehicle_count", nullable = false)
    private Integer inputVehicleCount;

    @Column(name = "assigned_parcel_count")
    private Integer assignedParcelCount;

    @Column(name = "unassigned_parcel_count")
    private Integer unassignedParcelCount;

    @Column(name = "number_of_routes")
    private Integer numberOfRoutes;
}
