package com.autopulse.routingservice.repository;

import com.autopulse.routingservice.model.RoutingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RoutingJobRepository extends JpaRepository<RoutingJob, UUID>, JpaSpecificationExecutor<RoutingJob> {
}
