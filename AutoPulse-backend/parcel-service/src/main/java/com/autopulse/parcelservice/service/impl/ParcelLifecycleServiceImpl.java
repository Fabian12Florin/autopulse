package com.autopulse.parcelservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.parcelservice.model.Parcel;
import com.autopulse.parcelservice.model.enums.ParcelStatus;
import com.autopulse.parcelservice.repository.ParcelRepository;
import com.autopulse.parcelservice.service.ParcelLifecycleService;
import com.autopulse.parcelservice.web.dto.request.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParcelLifecycleServiceImpl implements ParcelLifecycleService {

    private final ParcelRepository parcelRepository;

    @Override
    @Transactional
    public void updateStatus(UUID parcelId, ParcelStatus status) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new NotFoundException("Parcel not found."));


        if (parcel.getStatus() == ParcelStatus.DELIVERED) {
            throw new BadRequestException("Final parcel status cannot be changed.");
        }

        parcel.setStatus(status);

        parcelRepository.save(parcel);
    }
}