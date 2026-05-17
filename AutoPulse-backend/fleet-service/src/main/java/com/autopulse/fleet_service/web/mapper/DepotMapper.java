package com.autopulse.fleet_service.web.mapper;

import com.autopulse.common.model.Address;
import com.autopulse.common.model.Contact;
import com.autopulse.fleet_service.model.Depot;
import com.autopulse.fleet_service.web.dto.depot.CreateDepotRequest;
import com.autopulse.fleet_service.web.dto.depot.DepotResponse;
import com.autopulse.fleet_service.web.dto.depot.UpdateDepotRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DepotMapper {

    public void mapForCreate(CreateDepotRequest request, Depot depot) {
        mapCommon(
                depot,
                request.name(),
                request.addressStreet(),
                request.addressNumber(),
                request.localityId(),
                request.localityCode(),
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.depotCode(),
                request.latitude(),
                request.longitude()
        );
    }

    public void mapForUpdate(UpdateDepotRequest request, Depot depot) {
        mapCommon(
                depot,
                request.name(),
                request.addressStreet(),
                request.addressNumber(),
                request.localityId(),
                request.localityCode(),
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.depotCode(),
                request.latitude(),
                request.longitude()
        );
    }

    private void mapCommon(
            Depot depot,
            String name,
            String addressStreet,
            String addressNumber,
            UUID localityId,
            String localityCode,
            String contactName,
            String contactPhone,
            String contactEmail,
            String depotCode,
            Double latitude,
            Double longitude
    ) {
        Contact contact = ensureContact(depot);
        Address address = ensureAddress(contact);

        depot.setName(name);
        depot.setLocalityCode(localityCode);
        depot.setDepotCode(depotCode);

        contact.setName(contactName);
        contact.setPhone(contactPhone);
        contact.setEmail(contactEmail);

        address.setCityId(localityId);
        address.setStreet(addressStreet);
        address.setNumber(addressNumber);
        address.setLatitude(latitude);
        address.setLongitude(longitude);
    }

    private Contact ensureContact(Depot depot) {
        if (depot.getContact() == null) {
            depot.setContact(new Contact());
        }
        return depot.getContact();
    }

    private Address ensureAddress(Contact contact) {
        if (contact.getAddress() == null) {
            contact.setAddress(new Address());
        }
        return contact.getAddress();
    }

    public DepotResponse toResponse(Depot depot) {
        Contact contact = depot.getContact();
        Address address = contact != null ? contact.getAddress() : null;

        return new DepotResponse(
                depot.getId(),
                depot.getName(),
                address != null ? address.getStreet() : null,
                address != null ? address.getNumber() : null,
                address != null ? address.getCityId() : null,
                address != null ? address.getLatitude() : null,
                address != null ? address.getLongitude() : null,
                depot.getLocalityCode(),
                contact != null ? contact.getName() : null,
                contact != null ? contact.getPhone() : null,
                contact != null ? contact.getEmail() : null,
                depot.getDepotCode(),
                depot.getActive(),
                depot.getCreatedAt(),
                depot.getUpdatedAt()
        );
    }
}