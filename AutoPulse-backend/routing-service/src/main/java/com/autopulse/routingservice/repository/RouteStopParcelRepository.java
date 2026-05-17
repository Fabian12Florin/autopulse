package com.autopulse.routingservice.repository;

import com.autopulse.routingservice.model.RouteStopParcel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteStopParcelRepository extends JpaRepository<RouteStopParcel, UUID> {
    List<RouteStopParcel> findByRouteStopId(UUID routeStopId);
}
