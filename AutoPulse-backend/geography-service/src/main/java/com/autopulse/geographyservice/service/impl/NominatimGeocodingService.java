package com.autopulse.geographyservice.service.impl;

import com.autopulse.common.exception.NotFoundException;
import com.autopulse.geographyservice.client.NominatimClient;
import com.autopulse.geographyservice.mapper.GeocodingMapper;
import com.autopulse.geographyservice.service.GeocodingService;
import com.autopulse.geographyservice.web.dto.CoordinatesResponse;
import com.autopulse.geographyservice.web.dto.GeocodeAddressRequest;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NominatimGeocodingService implements GeocodingService {

    private final NominatimClient nominatimClient;
    private final Map<String, CoordinatesResponse> cache = new ConcurrentHashMap<>();

    public NominatimGeocodingService(NominatimClient nominatimClient) {
        this.nominatimClient = nominatimClient;
    }

    @Override
    public CoordinatesResponse resolveCoordinates(GeocodeAddressRequest request) {
        String cacheKey = buildCacheKey(request);
        CoordinatesResponse cachedResponse = cache.get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        CoordinatesResponse resolvedResponse = nominatimClient.searchFirst(request)
                .map(GeocodingMapper::toCoordinatesResponse)
                .orElseThrow(() -> new NotFoundException("Coordinates were not found for provided address"));

        cache.put(cacheKey, resolvedResponse);
        return resolvedResponse;
    }

    private String buildCacheKey(GeocodeAddressRequest request) {
        return normalize(request.street()) + "|" + normalize(request.city()) + "|" + normalize(request.county());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
