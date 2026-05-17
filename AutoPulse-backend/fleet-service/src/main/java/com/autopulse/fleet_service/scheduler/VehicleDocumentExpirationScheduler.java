package com.autopulse.fleet_service.scheduler;

import com.autopulse.common.kafka.KafkaTopics;
import com.autopulse.common.kafka.event.VehicleDocumentExpiringEvent;
import com.autopulse.fleet_service.model.VehicleDocument;
import com.autopulse.fleet_service.repository.VehicleDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VehicleDocumentExpirationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(VehicleDocumentExpirationScheduler.class);

    private static final int PAGE_SIZE = 100;

    private final VehicleDocumentRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String expirationNotificationEmail;

    public VehicleDocumentExpirationScheduler(
            VehicleDocumentRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${fleet.expiration-notification.email}") String expirationNotificationEmail
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.expirationNotificationEmail = expirationNotificationEmail;
    }

    @Transactional
    @Scheduled(fixedRate = 30000)
    public void checkExpiringDocuments() {
        LocalDate threshold = LocalDate.now().plusDays(7);

        Page<VehicleDocument> page;

        do {
            page = repository.findByExpiresAtLessThanEqualAndExpirationNotificationSentFalse(
                    threshold,
                    PageRequest.of(0, PAGE_SIZE)
            );

            for (VehicleDocument document : page.getContent()) {
                publishExpirationEvent(document);
            }

        } while (page.hasNext());
    }

    private void publishExpirationEvent(VehicleDocument document) {
        VehicleDocumentExpiringEvent event = new VehicleDocumentExpiringEvent(
                document.getId(),
                document.getVehicle().getId(),
                document.getDocumentType().name(),
                document.getExpiresAt(),
                expirationNotificationEmail,
                "AutoPulse Admin"
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    KafkaTopics.VEHICLE_DOCUMENT_EXPIRING,
                    document.getId().toString(),
                    payload
            );

            document.setExpirationNotificationSent(true);
            repository.save(document);

            log.info(
                    "Published vehicle document expiring event documentId={} vehicleId={} documentType={} expiresAt={}",
                    document.getId(),
                    document.getVehicle().getId(),
                    document.getDocumentType(),
                    document.getExpiresAt()
            );
        } catch (JsonProcessingException exception) {
            log.error(
                    "Failed to serialize vehicle document expiring event documentId={}",
                    document.getId(),
                    exception
            );
        }
    }
}