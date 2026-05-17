package com.autopulse.parcelservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.parcelservice.mapper.ParcelMapper;
import com.autopulse.parcelservice.model.Parcel;
import com.autopulse.parcelservice.model.enums.ParcelStatus;
import com.autopulse.parcelservice.model.enums.RouteType;
import com.autopulse.parcelservice.repository.ParcelRepository;
import com.autopulse.parcelservice.service.AwbService;
import com.autopulse.parcelservice.service.ParcelApplicationService;
import com.autopulse.parcelservice.web.client.FleetClient;
import com.autopulse.parcelservice.web.dto.query.ParcelSearchRequest;
import com.autopulse.parcelservice.web.dto.request.*;
import com.autopulse.parcelservice.web.dto.response.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ParcelApplicationServiceImpl implements ParcelApplicationService {

    private static final Set<ParcelStatus> UPDATE_ALLOWED_STATUSES = Set.of(
            ParcelStatus.CREATED,
            ParcelStatus.IN_SENDER_REGIONAL_DEPOSIT,
            ParcelStatus.IN_RECEIVER_REGIONAL_DEPOSIT,
            ParcelStatus.WAITING_IN_DEPOT
    );

    private static final Map<RouteType, ParcelStatus> ROUTE_TYPE_STATUS = Map.of(
            RouteType.SENDER_TO_REGIONAL, ParcelStatus.CREATED,
            RouteType.REGIONAL_TO_REGIONAL, ParcelStatus.IN_SENDER_REGIONAL_DEPOSIT,
            RouteType.REGIONAL_TO_RECEIVER, ParcelStatus.IN_RECEIVER_REGIONAL_DEPOSIT
    );

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParcelRepository parcelRepository;
    private final AwbService awbService;
    private final ParcelMapper parcelMapper;

    private final FleetClient fleetClient;

    @Override
    @Transactional
    public ParcelResponse createParcel(CreateParcelRequest req) {
        if(!fleetClient.verifyDepotCode(req.depotCode()))
            throw new BadRequestException("Invalid depot code: " + req.depotCode());
        Parcel parcel = buildParcel(req);
        Parcel saved = parcelRepository.saveAndFlush(parcel);
        return parcelMapper.toParcelResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelResponse getParcel(UUID parcelId) {
        Parcel parcel = findParcelOrThrow(parcelId);
        return parcelMapper.toParcelResponse(parcel);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelResponse getParcelByAwb(String awb) {
        Parcel parcel = parcelRepository.findByAwb(awb)
                .orElseThrow(() -> new NotFoundException("Parcel with AWB " + awb + " was not found."));
        return parcelMapper.toParcelResponse(parcel);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelPinResponse getParcelPinByAwb(String awb) {
        Parcel parcel = parcelRepository.findByAwb(awb)
                .orElseThrow(() -> new NotFoundException("Parcel with AWB " + awb + " was not found."));
        return new ParcelPinResponse(parcel.getPin());
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryCodeVerificationResponse verifyDeliveryCode(VerifyDeliveryCodeRequest request) {
        boolean valid = parcelRepository.findByAwb(request.awb())
                .map(parcel -> parcel.getPin().equals(request.pin()))
                .orElse(false);
        return new DeliveryCodeVerificationResponse(valid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutableParcelResponse> getRoutableParcels(
            String depotCode,
            RouteType routeType,
            int limit
    ) {
        if (depotCode == null || depotCode.isBlank()) {
            throw new BadRequestException("depotCode must not be blank.");
        }

        if (routeType == null) {
            throw new BadRequestException("routeType must not be null.");
        }

        ParcelStatus status = ROUTE_TYPE_STATUS.get(routeType);

        if (status == null) {
            throw new BadRequestException("Unsupported routeType: " + routeType);
        }

        int safeLimit = Math.clamp(limit, 1, 500);

        Pageable pageable = PageRequest.of(
                0,
                safeLimit,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        return parcelRepository
                .findByDepotCodeAndStatus(depotCode.trim(), status, pageable)
                .stream()
                .map(parcelMapper::toRoutableParcelResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ParcelSummaryResponse> searchParcels(ParcelSearchRequest criteria) {
        Specification<Parcel> specification = buildSpecification(criteria);
        Pageable pageable = buildPageable(criteria);
        Page<Parcel> page = parcelRepository.findAll(specification, pageable);
        return page.map(ParcelMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public ParcelResponse updateParcel(UUID parcelId, UpdateParcelRequest req) {
        Parcel parcel = findUpdatableParcelOrThrow(parcelId);

        applyUpdateRequest(parcel, req);

        Parcel saved = parcelRepository.save(parcel);
        return parcelMapper.toParcelResponse(saved);
    }

    private Parcel findParcelOrThrow(UUID parcelId) {
        return parcelRepository.findById(parcelId)
                .orElseThrow(() -> new NotFoundException("Parcel " + parcelId + " was not found."));
    }

    private Pageable buildPageable(ParcelSearchRequest criteria) {
        int page = Math.max(criteria.getPage(), 0);
        int size = Math.max(criteria.getSize(), 1);
        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        String sortBy = criteria.getSortBy();
        if (!List.of("createdAt", "updatedAt", "awb", "status", "weight", "volume").contains(sortBy)) {
            sortBy = "createdAt";
        }
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private Specification<Parcel> buildSpecification(ParcelSearchRequest criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getDepotCode() != null) {
                predicates.add(builder.equal(root.get("depotCode"), criteria.getDepotCode()));
            }
            if (criteria.getAwb() != null && !criteria.getAwb().isBlank()) {
                predicates.add(builder.like(builder.upper(root.get("awb")), "%" + criteria.getAwb().trim().toUpperCase() + "%"));
            }
            if (criteria.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getCreatedFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedFrom()));
            }
            if (criteria.getCreatedTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedTo()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Parcel findUpdatableParcelOrThrow(UUID parcelId) {
        Parcel parcel = findParcelOrThrow(parcelId);

        if (!UPDATE_ALLOWED_STATUSES.contains(parcel.getStatus())) {
            throw new BadRequestException(
                    "Parcel cannot be updated in status " + parcel.getStatus()
            );
        }

        return parcel;
    }

    private Parcel buildParcel(CreateParcelRequest req) {
        Parcel parcel = new Parcel();
        parcel.setAwb(awbService.generateAwb(
                new GenerateAwbRequest(req.depotCode())
        ));
        parcel.setDepotCode(req.depotCode());
        parcel.setSenderContact(parcelMapper.toContact(req.senderContact()));
        parcel.setReceiverContact(parcelMapper.toContact(req.receiverContact()));
        parcel.setWeight(req.weight());
        parcel.setVolume(req.volume());
        parcel.setStatus(ParcelStatus.CREATED);
        parcel.setPaymentRequired(req.paymentRequired());
        parcel.setPaymentAmount(req.paymentAmount());
        parcel.setDeclaredValue(req.declaredValue());
        parcel.setObservations(req.observations());
        parcel.setPin(generatePin());
        return parcel;
    }

    private String generatePin() {
        int randomPin = RANDOM.nextInt(10_000);
        return String.format("%04d", randomPin);
    }

    private void applyUpdateRequest(Parcel parcel, UpdateParcelRequest req) {
        if (req.depotCode() != null) {
            parcel.setDepotCode(req.depotCode());
        }
        if (req.senderContact() != null) {
            parcel.setSenderContact(parcelMapper.toContact(req.senderContact()));
        }
        if (req.receiverContact() != null) {
            parcel.setReceiverContact(parcelMapper.toContact(req.receiverContact()));
        }
        if (req.weight() != null) {
            parcel.setWeight(req.weight());
        }
        if (req.volume() != null) {
            parcel.setVolume(req.volume());
        }
        if (req.paymentRequired() != null) {
            parcel.setPaymentRequired(req.paymentRequired());
        }
        if (req.paymentAmount() != null) {
            parcel.setPaymentAmount(req.paymentAmount());
        }
        if (req.declaredValue() != null) {
            parcel.setDeclaredValue(req.declaredValue());
        }
        if (req.observations() != null) {
            parcel.setObservations(req.observations());
        }
    }
}
