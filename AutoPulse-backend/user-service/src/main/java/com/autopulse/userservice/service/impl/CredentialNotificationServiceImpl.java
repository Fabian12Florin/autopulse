package com.autopulse.userservice.service.impl;

import com.autopulse.common.kafka.KafkaTopics;
import com.autopulse.common.kafka.event.UserCreatedSendPasswordEvent;
import com.autopulse.common.kafka.event.UserResetPasswordEvent;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.DispatcherProfileRepository;
import com.autopulse.userservice.service.CredentialNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CredentialNotificationServiceImpl implements CredentialNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CredentialNotificationServiceImpl.class);
    private static final long SEND_TIMEOUT_SECONDS = 10L;

    private final KafkaTemplate<String, Object> credentialNotificationKafkaTemplate;
    private final CourierProfileRepository courierProfileRepository;
    private final DispatcherProfileRepository dispatcherProfileRepository;
    private final KeycloakAdminProperties keycloakAdminProperties;

    public CredentialNotificationServiceImpl(@Qualifier("credentialNotificationKafkaTemplate") KafkaTemplate<String, Object> credentialNotificationKafkaTemplate,
                                             CourierProfileRepository courierProfileRepository,
                                             DispatcherProfileRepository dispatcherProfileRepository,
                                             KeycloakAdminProperties keycloakAdminProperties) {
        this.credentialNotificationKafkaTemplate = credentialNotificationKafkaTemplate;
        this.courierProfileRepository = courierProfileRepository;
        this.dispatcherProfileRepository = dispatcherProfileRepository;
        this.keycloakAdminProperties = keycloakAdminProperties;
    }

    @Override
    public void sendInitialPassword(User user, String role, String temporaryPassword) {
        UserCreatedSendPasswordEvent event = new UserCreatedSendPasswordEvent(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                temporaryPassword,
                role
        );
        publish(KafkaTopics.USER_CREATED_SEND_PASSWORD, String.valueOf(user.getId()), event,
                "initial credential notification");
    }

    @Override
    public void sendResetPassword(User user, String temporaryPassword) {
        UserResetPasswordEvent event = new UserResetPasswordEvent(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                temporaryPassword,
                resolveRole(user)
        );
        publish(KafkaTopics.USER_RESET_PASSWORD, String.valueOf(user.getId()), event,
                "password reset notification");
    }

    private void publish(String topic, String key, Object event, String action) {
        try {
            SendResult<String, Object> result = credentialNotificationKafkaTemplate.send(topic, key, event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Queued {} topic={} partition={} offset={} key={}",
                    action,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    key);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for {} to send notification", action);
        } catch (ExecutionException | TimeoutException ex) {
            log.error("Exception while sending notification", ex);
        }
    }

    private String resolveRole(User user) {
        if (courierProfileRepository.existsByUserId(user.getId())) {
            return keycloakAdminProperties.getRoles().getCourier();
        }
        if (dispatcherProfileRepository.existsByUserId(user.getId())) {
            return keycloakAdminProperties.getRoles().getDispatcher();
        }
        return keycloakAdminProperties.getRoles().getAdmin();
    }
}
