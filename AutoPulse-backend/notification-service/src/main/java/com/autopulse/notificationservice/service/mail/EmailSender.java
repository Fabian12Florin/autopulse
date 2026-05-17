package com.autopulse.notificationservice.service.mail;

import com.autopulse.notificationservice.exception.MailDeliveryException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class EmailSender {

    private static final String FROM_PERSONAL_NAME = "AutoPulse";

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailSender(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void send(EmailMessage emailMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(fromAddress, FROM_PERSONAL_NAME, StandardCharsets.UTF_8.name()));
            helper.setTo(emailMessage.to());
            helper.setReplyTo(emailMessage.replyTo());
            helper.setSubject(emailMessage.subject());
            helper.setSentDate(new Date());
            helper.setText(emailMessage.htmlBody(), true);

            mailSender.send(mimeMessage);
        } catch (MailException exception) {
            throw new MailDeliveryException("Failed to send email to " + emailMessage.to(), exception);
        } catch (Exception exception) {
            throw new MailDeliveryException("Failed to prepare email for " + emailMessage.to(), exception);
        }
    }
}
