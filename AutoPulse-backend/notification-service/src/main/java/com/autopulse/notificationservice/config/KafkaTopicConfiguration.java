package com.autopulse.notificationservice.config;

import com.autopulse.common.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafkaProperties.getBootstrapServers()));
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userCreatedSendPasswordTopic() {
        return TopicBuilder.name(KafkaTopics.USER_CREATED_SEND_PASSWORD)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userResetPasswordTopic() {
        return TopicBuilder.name(KafkaTopics.USER_RESET_PASSWORD)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic vehicleDocumentExpiringTopic() {
        return TopicBuilder.name(KafkaTopics.VEHICLE_DOCUMENT_EXPIRING)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
