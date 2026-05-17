package com.autopulse.parcelservice.mapper;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.model.Address;
import com.autopulse.common.model.Contact;
import com.autopulse.parcelservice.model.Parcel;
import com.autopulse.parcelservice.web.client.GeographyClient;
import com.autopulse.parcelservice.web.client.dto.CoordinatesResponse;
import com.autopulse.parcelservice.web.client.dto.GeocodeAddressRequest;
import com.autopulse.parcelservice.web.client.dto.LocalityReferenceResponse;
import com.autopulse.parcelservice.web.client.dto.RegionReferenceResponse;
import com.autopulse.parcelservice.web.dto.request.AddressRequest;
import com.autopulse.parcelservice.web.dto.request.ContactRequest;
import com.autopulse.parcelservice.web.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public final class ParcelMapper {

    private final GeographyClient geographyClient;

    public Contact toContact(ContactRequest dto) {
        Contact contact = new Contact();
        contact.setName(dto.name());
        contact.setEmail(dto.email());
        contact.setPhone(dto.phone());
        contact.setAddress(toAddress(dto.address()));
        return contact;
    }

    public ContactResponse toContactResponse(Contact contact) {
        if (contact == null) {
            return null;
        }
        return new ContactResponse(
                contact.getName(),
                contact.getEmail(),
                contact.getPhone(),
                toAddressResponse(contact.getAddress())
        );
    }

    public Address toAddress(AddressRequest dto) {
        Address address = new Address();
        address.setCityId(dto.cityId());
        address.setStreet(dto.street());
        address.setNumber(dto.number());

        if (dto.latitude() != null && dto.longitude() != null) {
            address.setLatitude(dto.latitude());
            address.setLongitude(dto.longitude());
            return address;
        }

        LocalityReferenceResponse locality = geographyClient.getLocality(dto.cityId());

        CoordinatesResponse coordinates = geographyClient.resolveCoordinates(
                new GeocodeAddressRequest(
                        buildStreet(dto.street(), dto.number()),
                        requiredText(locality.name(), "Locality name"),
                        countyName(locality.region())
                )
        );

        address.setLatitude(requiredDouble(coordinates.y(), "Address latitude"));
        address.setLongitude(requiredDouble(coordinates.x(), "Address longitude"));

        return address;
    }

    public AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(address.getCityId(), address.getStreet(), address.getNumber(), address.getLongitude(), address.getLatitude());
    }

    public ParcelResponse toParcelResponse(Parcel parcel) {
        return new ParcelResponse(
                parcel.getId(),
                parcel.getAwb(),
                parcel.getDepotCode(),
                toContactResponse(parcel.getSenderContact()),
                toContactResponse(parcel.getReceiverContact()),
                parcel.getWeight(),
                parcel.getVolume(),
                parcel.getStatus(),
                Boolean.TRUE.equals(parcel.getPaymentRequired()),
                parcel.getPaymentAmount(),
                parcel.getDeclaredValue(),
                parcel.getObservations(),
                parcel.getCreatedAt(),
                parcel.getUpdatedAt()
        );
    }

    public static ParcelSummaryResponse toSummaryResponse(Parcel parcel) {
        return new ParcelSummaryResponse(
                parcel.getId(),
                parcel.getAwb(),
                parcel.getDepotCode(),
                parcel.getReceiverContact().getName(),
                parcel.getReceiverContact().getAddress().getCityId(),
                parcel.getWeight(),
                parcel.getVolume(),
                parcel.getStatus(),
                parcel.getCreatedAt()
        );
    }

    public RoutableParcelResponse toRoutableParcelResponse(Parcel parcel) {
        AddressResponse receiverAddress = toAddressResponse(parcel.getReceiverContact().getAddress());
        return new RoutableParcelResponse(
                parcel.getId(),
                parcel.getAwb(),
                parcel.getReceiverContact().getName(),
                parcel.getReceiverContact().getPhone(),
                receiverAddress.cityId(),
                receiverAddress.street(),
                receiverAddress.number(),
                receiverAddress.longitude(),
                receiverAddress.latitude(),
                parcel.getWeight(),
                parcel.getVolume()
        );
    }

    private String buildStreet(String street, String number) {
        String normalizedStreet = requiredText(street, "Street");
        if (number == null || number.isBlank()) {
            return normalizedStreet;
        }
        return normalizedStreet + " " + number.trim();
    }

    private String countyName(RegionReferenceResponse region) {
        return region == null ? null : region.name();
    }

    private double requiredDouble(Double value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " is required.");
        }
        return value;
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required.");
        }
        return value.trim();
    }
}
