package com.autopulse.notificationservice.messaging.consumer;

import com.autopulse.common.kafka.KafkaTopics;
import com.autopulse.common.kafka.event.VehicleDocumentExpiringEvent;
import com.autopulse.notificationservice.service.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleDocumentExpiringConsumer {

    private final NotificationProcessor notificationProcessor;

    @KafkaListener(
            topics = KafkaTopics.VEHICLE_DOCUMENT_EXPIRING,
            containerFactory = "vehicleDocumentExpiringKafkaListenerContainerFactory"
    )
    public void onMessage(VehicleDocumentExpiringEvent event) {
        log.info(
                "Consumed vehicle document expiring event documentId={} vehicleId={} documentType={} expiresAt={}",
                event.documentId(),
                event.vehicleId(),
                event.documentType(),
                event.expiresAt()
        );

        notificationProcessor.handle(event);
    }
}