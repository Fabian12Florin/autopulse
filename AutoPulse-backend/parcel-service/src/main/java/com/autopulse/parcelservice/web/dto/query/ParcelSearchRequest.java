package com.autopulse.parcelservice.web.dto.query;

import com.autopulse.parcelservice.model.enums.ParcelStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class ParcelSearchRequest {
    private String depotCode;
    private String awb;
    private ParcelStatus status;
    private Instant createdFrom;
    private Instant createdTo;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}

