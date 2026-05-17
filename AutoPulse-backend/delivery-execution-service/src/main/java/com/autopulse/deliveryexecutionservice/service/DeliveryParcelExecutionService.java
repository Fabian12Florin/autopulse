package com.autopulse.deliveryexecutionservice.service;

import com.autopulse.deliveryexecutionservice.mapper.DeliveryExecutionMapper;
import com.autopulse.deliveryexecutionservice.repository.DeliveryParcelExecutionRepository;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryExecution.DeliveryParcelExecutionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryParcelExecutionService {

    private final DeliveryParcelExecutionRepository repository;
    private final DeliveryExecutionMapper mapper;

    public Slice<DeliveryParcelExecutionDto> getByRunId(UUID runId, Pageable pageable) {
        return repository.findAllByDeliveryRunId(runId, pageable).map(mapper::toDto);
    }
}
