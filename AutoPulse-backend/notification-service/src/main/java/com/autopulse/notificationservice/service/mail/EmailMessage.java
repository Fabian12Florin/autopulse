package com.autopulse.notificationservice.service.mail;

public record EmailMessage(
        String to,
        String replyTo,
        String subject,
        String htmlBody
) {
}
