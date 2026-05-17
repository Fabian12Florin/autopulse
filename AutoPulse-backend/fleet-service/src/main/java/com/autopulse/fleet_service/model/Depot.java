package com.autopulse.fleet_service.model;

import com.autopulse.common.model.BaseEntity;
import com.autopulse.common.model.Contact;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "depots")
public class Depot extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "contact_name", nullable = false, length = 120)),
            @AttributeOverride(name = "email", column = @Column(name = "contact_email", nullable = false, length = 180)),
            @AttributeOverride(name = "phone", column = @Column(name = "contact_phone", nullable = false, length = 40)),
            @AttributeOverride(name = "address.cityId", column = @Column(name = "locality_id", nullable = false)),
            @AttributeOverride(name = "address.street", column = @Column(name = "address_street", nullable = false, length = 180)),
            @AttributeOverride(name = "address.number", column = @Column(name = "address_number", nullable = false, length = 40)),
            @AttributeOverride(name = "address.longitude", column = @Column(name = "depot_longitude", nullable = false)),
            @AttributeOverride(name = "address.latitude", column = @Column(name = "depot_latitude", nullable = false))
    })
    private Contact contact;

    @Column(length = 50)
    private String localityCode;

    @Column(nullable = false, length = 50, unique = true)
    private String depotCode;

    @Column(nullable = false)
    private Boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getLocalityCode() {
        return localityCode;
    }

    public void setLocalityCode(String localityCode) {
        this.localityCode = localityCode;
    }

    public String getDepotCode() {
        return depotCode;
    }

    public void setDepotCode(String depotCode) {
        this.depotCode = depotCode;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}