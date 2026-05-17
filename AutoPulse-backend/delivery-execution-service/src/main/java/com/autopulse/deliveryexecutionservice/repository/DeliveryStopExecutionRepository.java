package com.autopulse.deliveryexecutionservice.repository;

import com.autopulse.deliveryexecutionservice.model.DeliveryStopExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryStopExecutionRepository extends JpaRepository<DeliveryStopExecution, UUID> {

    List<DeliveryStopExecution> findByDeliveryRunIdOrderByStopOrderAsc(UUID deliveryRunId);
}
