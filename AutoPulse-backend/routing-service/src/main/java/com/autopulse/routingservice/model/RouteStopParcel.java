package com.autopulse.routingservice.model;

import com.autopulse.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "route_stop_parcel")
@Getter
@Setter
@NoArgsConstructor
public class RouteStopParcel extends BaseEntity {

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
}
