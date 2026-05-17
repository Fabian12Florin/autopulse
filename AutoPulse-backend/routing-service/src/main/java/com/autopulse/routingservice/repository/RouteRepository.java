package com.autopulse.routingservice.repository;

import com.autopulse.routingservice.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {
    List<Route> findByRoutingJobId(UUID routingJobId);
}
