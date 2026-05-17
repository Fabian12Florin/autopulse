package com.autopulse.geographyservice.client;

import com.autopulse.geographyservice.config.NominatimProperties;
import com.autopulse.geographyservice.exception.ExternalGeocodingException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class NominatimRateLimiter {

    private final Object lock = new Object();
    private final Duration minimumRequestInterval;
    private Instant lastRequestAt = Instant.EPOCH;

    public NominatimRateLimiter(NominatimProperties properties) {
        this.minimumRequestInterval = properties.getMinimumRequestInterval();
    }

    public void acquire() {
        if (minimumRequestInterval == null || minimumRequestInterval.isZero() || minimumRequestInterval.isNegative()) {
            return;
        }

        synchronized (lock) {
            Instant now = Instant.now();
            long elapsedMillis = Duration.between(lastRequestAt, now).toMillis();
            long delayMillis = minimumRequestInterval.toMillis() - elapsedMillis;

            if (delayMillis > 0) {
                sleep(delayMillis);
            }

            lastRequestAt = Instant.now();
        }
    }

    private void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalGeocodingException("Nominatim request was interrupted");
        }
    }
}
