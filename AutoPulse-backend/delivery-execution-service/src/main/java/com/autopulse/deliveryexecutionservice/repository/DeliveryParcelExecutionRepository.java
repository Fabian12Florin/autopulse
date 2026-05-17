package com.autopulse.deliveryexecutionservice.repository;

import com.autopulse.deliveryexecutionservice.model.DeliveryParcelExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryParcelExecutionRepository extends JpaRepository<DeliveryParcelExecution, UUID> {

    Slice<DeliveryParcelExecution> findAllByDeliveryRunId(UUID runId, Pageable pageable);

    List<DeliveryParcelExecution> findByDeliveryRunId(UUID deliveryRunId);

    List<DeliveryParcelExecution> findByDeliveryStopExecutionId(UUID deliveryStopExecutionId);

    Optional<DeliveryParcelExecution> findByDeliveryStopExecutionIdAndAwb(UUID deliveryStopExecutionId, String awb);
}
