package com.autopulse.parcelservice.service.impl;

import com.autopulse.parcelservice.repository.ParcelRepository;
import com.autopulse.parcelservice.service.AwbService;
import com.autopulse.parcelservice.web.dto.request.GenerateAwbRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AwbServiceImpl implements AwbService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_ATTEMPTS = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParcelRepository parcelRepository;

    @Override
    public String generateAwb(GenerateAwbRequest context) {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String depotPart = context.depotCode() == null
                ? "NA"
                : context.depotCode().toUpperCase();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String candidate = "AWB-" + datePart + "-" + depotPart + "-" + String.format("%09d", RANDOM.nextInt(1_000_000_000));
            if (parcelRepository.findByAwb(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique AWB after " + MAX_ATTEMPTS + " attempts.");
    }
}

