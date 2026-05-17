package com.autopulse.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Contact {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "phone", nullable = false, length = 40)
    private String phone;

    @Embedded
    private Address address;
}
