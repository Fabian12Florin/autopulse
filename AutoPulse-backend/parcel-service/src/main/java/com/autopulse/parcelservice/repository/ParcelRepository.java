package com.autopulse.parcelservice.repository;

import com.autopulse.parcelservice.model.Parcel;
import com.autopulse.parcelservice.model.enums.ParcelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParcelRepository extends JpaRepository<Parcel, UUID> {

    Optional<Parcel> findByAwb(String awb);

    Page<Parcel> findAll(Specification<Parcel> specification, Pageable pageable);

    List<Parcel> findByDepotCodeAndStatus(
            String depotCode,
            ParcelStatus status,
            Pageable pageable
    );
}
