package com.autopulse.deliveryexecutionservice.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryOutcome;
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
@Table(name = "delivery_parcel_executions")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryParcelExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_run_id", nullable = false)
    private DeliveryRun deliveryRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_stop_execution_id", nullable = false)
    private DeliveryStopExecution deliveryStopExecution;

    @Column(name = "route_stop_id", nullable = false)
    private UUID routeStopId;

    @Column(name = "parcel_id", nullable = false)
    private UUID parcelId;

    @Column(name = "awb")
    private String awb;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "volume")
    private Integer volume;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 30)
    private DeliveryOutcome outcome;

    @Column(name = "delivery_code_verified", nullable = false)
    private Boolean deliveryCodeVerified = false;
}
