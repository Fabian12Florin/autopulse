package com.autopulse.notificationservice.service;

import com.autopulse.common.kafka.event.UserCreatedSendPasswordEvent;
import com.autopulse.common.kafka.event.UserResetPasswordEvent;
import com.autopulse.common.kafka.event.VehicleDocumentExpiringEvent;

public interface NotificationProcessor {

    void handle(UserCreatedSendPasswordEvent event);

    void handle(UserResetPasswordEvent event);

    void handle(VehicleDocumentExpiringEvent event);
}
