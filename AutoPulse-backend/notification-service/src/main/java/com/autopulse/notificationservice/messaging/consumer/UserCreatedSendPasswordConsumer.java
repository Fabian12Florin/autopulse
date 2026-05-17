package com.autopulse.notificationservice.messaging.consumer;

import com.autopulse.common.kafka.KafkaTopics;
import com.autopulse.common.kafka.event.UserCreatedSendPasswordEvent;
import com.autopulse.notificationservice.service.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedSendPasswordConsumer {

    private final NotificationProcessor notificationProcessor;

    @KafkaListener(
            topics = KafkaTopics.USER_CREATED_SEND_PASSWORD,
            containerFactory = "userCreatedSendPasswordKafkaListenerContainerFactory"
    )
    public void onMessage(
            @Payload UserCreatedSendPasswordEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Consumed user created password event topic={} partition={} offset={} userId={} email={}",
                topic, partition, offset, event.userId(), event.email());

        notificationProcessor.handle(event);
    }
}
