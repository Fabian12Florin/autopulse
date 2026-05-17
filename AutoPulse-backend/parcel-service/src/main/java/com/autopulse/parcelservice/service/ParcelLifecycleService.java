package com.autopulse.parcelservice.service;

import com.autopulse.parcelservice.model.enums.ParcelStatus;

import java.util.UUID;

public interface ParcelLifecycleService {

    void updateStatus(UUID parcelId, ParcelStatus status);
}
