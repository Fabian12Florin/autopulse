package com.autopulse.fleet_service.service;

import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.fleet_service.model.Depot;
import com.autopulse.fleet_service.repository.DepotRepository;
import com.autopulse.fleet_service.web.dto.depot.CreateDepotRequest;
import com.autopulse.fleet_service.web.dto.depot.DepotCoordinatesResponse;
import com.autopulse.fleet_service.web.dto.depot.DepotResponse;
import com.autopulse.fleet_service.web.dto.depot.UpdateDepotRequest;
import com.autopulse.fleet_service.web.mapper.DepotMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepotService {

    private static final Logger log = LoggerFactory.getLogger(DepotService.class);

    private final DepotRepository depotRepository;
    private final DepotMapper depotMapper;

    public DepotService(DepotRepository depotRepository, DepotMapper depotMapper) {
        this.depotRepository = depotRepository;
        this.depotMapper = depotMapper;
    }

    public DepotResponse create(CreateDepotRequest request) {
        log.info("Creating depot with depotCode={}", request.depotCode());

        if (depotRepository.existsByDepotCode(request.depotCode())) {
            log.warn("Depot creation failed. Depot code already exists: depotCode={}", request.depotCode());
            throw new ConflictException("Depot code already exists");
        }

        Depot depot = new Depot();
        depotMapper.mapForCreate(request, depot);
        depot.setActive(true);

        Depot savedDepot = depotRepository.save(depot);
        log.info("Depot created successfully: depotId={}, depotCode={}", savedDepot.getId(), savedDepot.getDepotCode());

        return depotMapper.toResponse(savedDepot);
    }

    @Transactional(readOnly = true)
    public Page<DepotResponse> getAll(Pageable pageable) {
        log.debug("Fetching depots page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return depotRepository.findAll(pageable).map(depotMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DepotResponse getById(UUID id) {
        log.debug("Fetching depot by id={}", id);
        return depotMapper.toResponse(getEntity(id));
    }

    public DepotResponse update(UUID id, UpdateDepotRequest request) {
        log.info("Updating depot: depotId={}, depotCode={}", id, request.depotCode());

        Depot depot = getEntity(id);

        if (depotRepository.existsByDepotCodeAndIdNot(request.depotCode(), id)) {
            log.warn("Depot update failed. Depot code already exists: depotId={}, depotCode={}", id, request.depotCode());
            throw new ConflictException("Depot code already exists");
        }

        depotMapper.mapForUpdate(request, depot);

        Depot savedDepot = depotRepository.save(depot);
        log.info("Depot updated successfully: depotId={}, depotCode={}", savedDepot.getId(), savedDepot.getDepotCode());

        return depotMapper.toResponse(savedDepot);
    }

    public DepotResponse activate(UUID id) {
        log.info("Activating depot: depotId={}", id);

        Depot depot = getEntity(id);
        depot.setActive(true);

        Depot savedDepot = depotRepository.save(depot);
        log.info("Depot activated successfully: depotId={}", savedDepot.getId());

        return depotMapper.toResponse(savedDepot);
    }

    public DepotResponse deactivate(UUID id) {
        log.info("Deactivating depot: depotId={}", id);

        Depot depot = getEntity(id);
        depot.setActive(false);

        Depot savedDepot = depotRepository.save(depot);
        log.info("Depot deactivated successfully: depotId={}", savedDepot.getId());

        return depotMapper.toResponse(savedDepot);
    }

    public Depot getEntity(UUID id) {
        return depotRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Depot not found: depotId={}", id);
                    return new NotFoundException("Depot not found");
                });
    }

    @Transactional(readOnly = true)
    public DepotCoordinatesResponse getDepotCoordinates(UUID id) {
        log.debug("Fetching depot coordinates: depotId={}", id);

        Depot depot = getEntity(id);

        return new DepotCoordinatesResponse(
                depot.getId(),
                depot.getContact().getAddress().getLatitude(),
                depot.getContact().getAddress().getLongitude()
        );
    }

    public boolean verifyDepotCode(String depotCode) {
        return depotRepository.existsByDepotCode(depotCode);
    }
}