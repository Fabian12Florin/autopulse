package com.autopulse.notificationservice.service.impl;

import com.autopulse.common.kafka.event.UserCreatedSendPasswordEvent;
import com.autopulse.common.kafka.event.UserResetPasswordEvent;
import com.autopulse.common.kafka.event.VehicleDocumentExpiringEvent;
import com.autopulse.notificationservice.service.NotificationProcessor;
import com.autopulse.notificationservice.service.mail.EmailMessage;
import com.autopulse.notificationservice.service.mail.EmailSender;
import com.autopulse.notificationservice.service.mail.UserCreatedSendPasswordEmailFactory;
import com.autopulse.notificationservice.service.mail.UserResetPasswordEmailFactory;
import com.autopulse.notificationservice.service.mail.VehicleDocumentExpiringEmailFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessorImpl implements NotificationProcessor {

    private final UserCreatedSendPasswordEmailFactory userCreatedSendPasswordEmailFactory;
    private final UserResetPasswordEmailFactory userResetPasswordEmailFactory;
    private final VehicleDocumentExpiringEmailFactory vehicleDocumentExpiringEmailFactory;
    private final EmailSender emailSender;

    @Override
    public void handle(UserCreatedSendPasswordEvent event) {
        validate(event.email(), event.password());

        EmailMessage emailMessage = userCreatedSendPasswordEmailFactory.build(event);
        emailSender.send(emailMessage);

        log.info("Initial credential email sent successfully to userId={} email={}", event.userId(), event.email());
    }

    @Override
    public void handle(UserResetPasswordEvent event) {
        validate(event.email(), event.password());

        EmailMessage emailMessage = userResetPasswordEmailFactory.build(event);
        emailSender.send(emailMessage);

        log.info("Password reset email sent successfully to userId={} email={}", event.userId(), event.email());
    }

    @Override
    public void handle(VehicleDocumentExpiringEvent event) {
        if (isBlank(event.recipientEmail())) {
            throw new IllegalArgumentException("Recipient email must not be blank");
        }

        EmailMessage emailMessage = vehicleDocumentExpiringEmailFactory.build(event);
        emailSender.send(emailMessage);

        log.info(
                "Vehicle document expiration email sent successfully documentId={} vehicleId={} email={}",
                event.documentId(),
                event.vehicleId(),
                event.recipientEmail()
        );
    }

    private void validate(String email, String password) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Event email must not be blank");
        }
        if (isBlank(password)) {
            throw new IllegalArgumentException("Event password must not be blank");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
