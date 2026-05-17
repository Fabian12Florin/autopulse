package com.autopulse.geographyservice.service;

import com.autopulse.geographyservice.web.dto.CoordinatesResponse;
import com.autopulse.geographyservice.web.dto.GeocodeAddressRequest;

public interface GeocodingService {

    CoordinatesResponse resolveCoordinates(GeocodeAddressRequest request);
}
