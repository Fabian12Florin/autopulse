package com.autopulse.parcelservice.web.client.dto;

public record GeocodeAddressRequest(
        String street,
        String city,
        String county
) {
}
