package com.autopulse.geographyservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.geocoding.nominatim")
public class NominatimProperties {

    private String baseUrl = "https://nominatim.openstreetmap.org";
    private String userAgent = "AutoPulse-Geography-Service/1.0";
    private String countryCodes = "ro";
    private int limit = 1;
    private boolean addressDetails = true;
    private Duration minimumRequestInterval = Duration.ofSeconds(1);

}
