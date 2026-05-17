package com.autopulse.geographyservice.client;

import com.autopulse.geographyservice.client.dto.NominatimSearchResult;
import com.autopulse.geographyservice.config.NominatimProperties;
import com.autopulse.geographyservice.exception.ExternalGeocodingException;
import com.autopulse.geographyservice.web.dto.GeocodeAddressRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class NominatimClient {

    private static final ParameterizedTypeReference<List<NominatimSearchResult>> SEARCH_RESULT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final NominatimProperties properties;
    private final NominatimRateLimiter rateLimiter;

    public NominatimClient(@Qualifier("nominatimRestClient") RestClient restClient,
                           NominatimProperties properties,
                           NominatimRateLimiter rateLimiter) {
        this.restClient = restClient;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    public Optional<NominatimSearchResult> searchFirst(GeocodeAddressRequest request) {
        rateLimiter.acquire();

        try {
            List<NominatimSearchResult> results = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/search")
                                .queryParam("street", request.street().trim())
                                .queryParam("city", request.city().trim())
                                .queryParam("format", "json")
                                .queryParam("limit", properties.getLimit())
                                .queryParam("addressdetails", properties.isAddressDetails() ? 1 : 0);

                        if (request.county() != null && !request.county().isBlank()) {
                            builder.queryParam("county", request.county().trim());
                        }

                        if (properties.getCountryCodes() != null && !properties.getCountryCodes().isBlank()) {
                            builder.queryParam("countrycodes", properties.getCountryCodes().trim());
                        }

                        return builder.build();
                    })
                    .retrieve()
                    .body(SEARCH_RESULT_TYPE);

            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(results.getFirst());
        } catch (RestClientException ex) {
            throw new ExternalGeocodingException(
                    "Failed to retrieve geocoding data from Nominatim",
                    Map.of("error", ex.getMessage())
            );
        }
    }
}
