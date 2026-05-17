package com.autopulse.userservice.service.impl;

import com.autopulse.common.kafka.KafkaTopics;
import com.autopulse.common.kafka.event.UserCreatedSendPasswordEvent;
import com.autopulse.common.kafka.event.UserResetPasswordEvent;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.repository.CourierProfileRepository;
import com.autopulse.userservice.repository.DispatcherProfileRepository;
import com.autopulse.userservice.testutil.TestDataFactory;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CredentialNotificationServiceImplTest {

    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final CourierProfileRepository courierProfileRepository = mock(CourierProfileRepository.class);
    private final DispatcherProfileRepository dispatcherProfileRepository = mock(DispatcherProfileRepository.class);
    private final KeycloakAdminProperties keycloakAdminProperties = TestDataFactory.keycloakProperties();
    private final CredentialNotificationServiceImpl service = new CredentialNotificationServiceImpl(
            kafkaTemplate,
            courierProfileRepository,
            dispatcherProfileRepository,
            keycloakAdminProperties
    );

    @Test
    void sendsInitialPasswordEvent() {
        User user = TestDataFactory.user();
        when(kafkaTemplate.send(eq(KafkaTopics.USER_CREATED_SEND_PASSWORD), eq(String.valueOf(TestDataFactory.USER_ID)), any()))
                .thenReturn(successfulSend(KafkaTopics.USER_CREATED_SEND_PASSWORD));

        service.sendInitialPassword(user, "COURIER", "TempPass123");

        ArgumentCaptor<UserCreatedSendPasswordEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedSendPasswordEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.USER_CREATED_SEND_PASSWORD), eq(String.valueOf(TestDataFactory.USER_ID)), eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo(user.getEmail());
        assertThat(eventCaptor.getValue().role()).isEqualTo("COURIER");
        assertThat(eventCaptor.getValue().password()).isEqualTo("TempPass123");
    }

    @Test
    void resetPasswordResolvesCourierDispatcherAndAdminRoles() {
        User user = TestDataFactory.user();
        when(kafkaTemplate.send(eq(KafkaTopics.USER_RESET_PASSWORD), eq(String.valueOf(TestDataFactory.USER_ID)), any()))
                .thenReturn(successfulSend(KafkaTopics.USER_RESET_PASSWORD));

        when(courierProfileRepository.existsByUserId(TestDataFactory.USER_ID)).thenReturn(true);
        service.sendResetPassword(user, "TempPass123");

        when(courierProfileRepository.existsByUserId(TestDataFactory.USER_ID)).thenReturn(false);
        when(dispatcherProfileRepository.existsByUserId(TestDataFactory.USER_ID)).thenReturn(true);
        service.sendResetPassword(user, "TempPass123");

        when(dispatcherProfileRepository.existsByUserId(TestDataFactory.USER_ID)).thenReturn(false);
        service.sendResetPassword(user, "TempPass123");

        ArgumentCaptor<UserResetPasswordEvent> eventCaptor = ArgumentCaptor.forClass(UserResetPasswordEvent.class);
        verify(kafkaTemplate, times(3)).send(eq(KafkaTopics.USER_RESET_PASSWORD), eq(String.valueOf(TestDataFactory.USER_ID)), eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(UserResetPasswordEvent::role)
                .containsExactly("COURIER", "DISPATCHER", "ADMIN");
    }

    @Test
    void publishHandlesInterruptedAndExecutionFailuresWithoutThrowing() throws Exception {
        User user = TestDataFactory.user();
        CompletableFuture<SendResult<String, Object>> interrupted = mock(CompletableFuture.class);
        when(interrupted.get(10L, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));
        when(kafkaTemplate.send(eq(KafkaTopics.USER_CREATED_SEND_PASSWORD), eq(String.valueOf(TestDataFactory.USER_ID)), any()))
                .thenReturn(interrupted);

        service.sendInitialPassword(user, "ADMIN", "TempPass123");
        assertThat(Thread.interrupted()).isTrue();

        CompletableFuture<SendResult<String, Object>> failed = mock(CompletableFuture.class);
        when(failed.get(10L, TimeUnit.SECONDS)).thenThrow(new ExecutionException(new IllegalStateException("send failed")));
        when(kafkaTemplate.send(eq(KafkaTopics.USER_CREATED_SEND_PASSWORD), eq(String.valueOf(TestDataFactory.USER_ID)), any()))
                .thenReturn(failed);

        service.sendInitialPassword(user, "ADMIN", "TempPass123");
    }

    private CompletableFuture<SendResult<String, Object>> successfulSend(String topic) {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 0L, 1, 0L, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(null, metadata));
    }

}
