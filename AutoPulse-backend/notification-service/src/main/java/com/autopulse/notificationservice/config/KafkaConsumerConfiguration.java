package com.autopulse.notificationservice.config;

import com.autopulse.common.kafka.event.UserCreatedSendPasswordEvent;
import com.autopulse.common.kafka.event.UserResetPasswordEvent;
import com.autopulse.common.kafka.event.VehicleDocumentExpiringEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfiguration.class);

    @Bean
    public ConsumerFactory<String, UserCreatedSendPasswordEvent> userCreatedSendPasswordConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(baseConsumerProperties(kafkaProperties, UserCreatedSendPasswordEvent.class));
    }

    @Bean
    public ConsumerFactory<String, UserResetPasswordEvent> userResetPasswordConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(baseConsumerProperties(kafkaProperties, UserResetPasswordEvent.class));
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        ConsumerRecordRecoverer recoverer = (record, exception) ->
                log.error("Kafka record permanently failed topic={} partition={} offset={} due to {}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception.getMessage(),
                        exception);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(5000L, 2L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, DeserializationException.class);
        errorHandler.setCommitRecovered(true);
        errorHandler.setAckAfterHandle(true);
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserCreatedSendPasswordEvent>
    userCreatedSendPasswordKafkaListenerContainerFactory(
            ConsumerFactory<String, UserCreatedSendPasswordEvent> userCreatedSendPasswordConsumerFactory,
            CommonErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, UserCreatedSendPasswordEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(userCreatedSendPasswordConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserResetPasswordEvent>
    userResetPasswordKafkaListenerContainerFactory(
            ConsumerFactory<String, UserResetPasswordEvent> userResetPasswordConsumerFactory,
            CommonErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, UserResetPasswordEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(userResetPasswordConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, VehicleDocumentExpiringEvent> vehicleDocumentExpiringConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProperties(
                        kafkaProperties,
                        VehicleDocumentExpiringEvent.class
                )
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VehicleDocumentExpiringEvent> vehicleDocumentExpiringKafkaListenerContainerFactory(
            ConsumerFactory<String, VehicleDocumentExpiringEvent> consumerFactory,
            CommonErrorHandler kafkaErrorHandler
    ) {

        ConcurrentKafkaListenerContainerFactory<String, VehicleDocumentExpiringEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.setCommonErrorHandler(kafkaErrorHandler);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

    private <T> Map<String, Object> baseConsumerProperties(KafkaProperties kafkaProperties, Class<T> payloadClass) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class.getName());
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, payloadClass.getName());
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, payloadClass.getPackageName());
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }
}
