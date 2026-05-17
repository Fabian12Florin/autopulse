package com.autopulse.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(name = "street", nullable = false, length = 180)
    private String street;

    @Column(name = "number", nullable = false, length = 40)
    private String number;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;
}
