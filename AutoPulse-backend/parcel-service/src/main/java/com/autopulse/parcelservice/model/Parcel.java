package com.autopulse.parcelservice.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.common.model.Contact;
import com.autopulse.parcelservice.model.enums.ParcelStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "parcels")
public class Parcel extends BaseEntity {

    @Column(name = "awb", nullable = false, unique = true, length = 40)
    private String awb;

    @Column(name = "depot_code", nullable = false, length = 10)
    private String depotCode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "sender_name", nullable = false, length = 120)),
            @AttributeOverride(name = "email", column = @Column(name = "sender_email", length = 180)),
            @AttributeOverride(name = "phone", column = @Column(name = "sender_phone", nullable = false, length = 40)),
            @AttributeOverride(name = "address.cityId", column = @Column(name = "sender_city_id", nullable = false)),
            @AttributeOverride(name = "address.street", column = @Column(name = "sender_street", nullable = false, length = 180)),
            @AttributeOverride(name = "address.number", column = @Column(name = "sender_number", nullable = false, length = 40)),
            @AttributeOverride(name = "address.longitude", column = @Column(name = "sender_longitude", nullable = false)),
            @AttributeOverride(name = "address.latitude", column = @Column(name = "sender_latitude", nullable = false))
    })
    private Contact senderContact;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "receiver_name", nullable = false, length = 120)),
            @AttributeOverride(name = "email", column = @Column(name = "receiver_email", length = 180)),
            @AttributeOverride(name = "phone", column = @Column(name = "receiver_phone", nullable = false, length = 40)),
            @AttributeOverride(name = "address.cityId", column = @Column(name = "receiver_city_id", nullable = false)),
            @AttributeOverride(name = "address.street", column = @Column(name = "receiver_street", nullable = false, length = 180)),
            @AttributeOverride(name = "address.number", column = @Column(name = "receiver_number", nullable = false, length = 40)),
            @AttributeOverride(name = "address.longitude", column = @Column(name = "receiver_longitude", nullable = false)),
            @AttributeOverride(name = "address.latitude", column = @Column(name = "receiver_latitude", nullable = false))
    })
    private Contact receiverContact;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "volume", nullable = false)
    private Double volume;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParcelStatus status;

    @Column(name = "payment_required", nullable = false)
    private Boolean paymentRequired;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "declared_value", precision = 10, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "observations", length = 256)
    private String observations;

    @Column(name = "pin", nullable = false, length = 4)
    private String pin;
}
