package com.autopulse.fleet_service.service;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleDocument;
import com.autopulse.fleet_service.repository.VehicleDocumentRepository;
import com.autopulse.fleet_service.web.dto.vehicle_document.CreateVehicleDocumentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_document.UpdateVehicleDocumentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_document.VehicleDocumentResponse;
import com.autopulse.fleet_service.web.mapper.VehicleDocumentMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleDocumentService {

    private static final Logger log = LoggerFactory.getLogger(VehicleDocumentService.class);

    private final VehicleDocumentRepository repository;
    private final VehicleService vehicleService;
    private final VehicleDocumentMapper vehicleDocumentMapper;

    public VehicleDocumentService(
            VehicleDocumentRepository repository,
            VehicleService vehicleService,
            VehicleDocumentMapper vehicleDocumentMapper
    ) {
        this.repository = repository;
        this.vehicleService = vehicleService;
        this.vehicleDocumentMapper = vehicleDocumentMapper;
    }

    public VehicleDocumentResponse create(UUID vehicleId, CreateVehicleDocumentRequest request) {
        log.info("Creating vehicle document: vehicleId={}, issuedAt={}, expiresAt={}",
                vehicleId, request.issuedAt(), request.expiresAt());

        validateDates(request.issuedAt(), request.expiresAt());

        Vehicle vehicle = vehicleService.getEntity(vehicleId);
        VehicleDocument document = vehicleDocumentMapper.toEntity(request, vehicle);

        VehicleDocument savedDocument = repository.save(document);
        log.info("Vehicle document created successfully: documentId={}, vehicleId={}",
                savedDocument.getId(), vehicleId);

        return vehicleDocumentMapper.toResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public Page<VehicleDocumentResponse> getByVehicleId(UUID vehicleId, Pageable pageable) {
        log.debug("Fetching vehicle documents by vehicle: vehicleId={}, page={}, size={}, sort={}",
                vehicleId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return repository.findByVehicleId(vehicleId, pageable).map(vehicleDocumentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleDocumentResponse getById(UUID id) {
        log.debug("Fetching vehicle document by id={}", id);
        return vehicleDocumentMapper.toResponse(getEntity(id));
    }

    public VehicleDocumentResponse update(UUID id, UpdateVehicleDocumentRequest request) {
        log.info("Updating vehicle document: documentId={}, issuedAt={}, expiresAt={}",
                id, request.issuedAt(), request.expiresAt());

        validateDates(request.issuedAt(), request.expiresAt());

        VehicleDocument document = getEntity(id);
        vehicleDocumentMapper.mapForUpdate(request, document);

        VehicleDocument savedDocument = repository.save(document);
        log.info("Vehicle document updated successfully: documentId={}, vehicleId={}",
                savedDocument.getId(), savedDocument.getVehicle().getId());

        return vehicleDocumentMapper.toResponse(savedDocument);
    }

    public void delete(UUID id) {
        log.info("Deleting vehicle document: documentId={}", id);

        VehicleDocument document = getEntity(id);
        repository.delete(document);

        log.info("Vehicle document deleted successfully: documentId={}", id);
    }

    @Transactional(readOnly = true)
    public Page<VehicleDocumentResponse> getExpired(Pageable pageable) {
        LocalDate today = LocalDate.now();

        log.debug("Fetching expired vehicle documents before date={}, page={}, size={}, sort={}",
                today, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return repository.findByExpiresAtBefore(today, pageable)
                .map(vehicleDocumentMapper::toResponse);
    }

    public VehicleDocument getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle document not found: documentId={}", id);
                    return new NotFoundException("Vehicle document not found");
                });
    }

    private void validateDates(LocalDate issuedAt, LocalDate expiresAt) {
        if (expiresAt.isBefore(issuedAt)) {
            log.warn("Invalid vehicle document dates: issuedAt={}, expiresAt={}", issuedAt, expiresAt);
            throw new BadRequestException("Expiration date cannot be before issued date");
        }
    }
}