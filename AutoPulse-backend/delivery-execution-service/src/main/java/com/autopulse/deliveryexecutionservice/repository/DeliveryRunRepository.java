package com.autopulse.deliveryexecutionservice.repository;

import com.autopulse.deliveryexecutionservice.model.DeliveryRun;
import com.autopulse.deliveryexecutionservice.model.enums.DeliveryRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRunRepository extends JpaRepository<DeliveryRun, UUID>, JpaSpecificationExecutor<DeliveryRun> {

    Optional<DeliveryRun> findByRouteId(UUID routeId);

    boolean existsByRouteId(UUID routeId);

}
