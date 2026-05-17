package com.autopulse.fleet_service.repository;

import com.autopulse.fleet_service.model.Depot;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepotRepository extends JpaRepository<Depot, UUID> {
    boolean existsByDepotCode(String depotCode);
    boolean existsByDepotCodeAndIdNot(String depotCode, UUID id);
}