package com.autopulse.notificationservice.service.mail;

import com.autopulse.common.kafka.event.VehicleDocumentExpiringEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

@Component
public class VehicleDocumentExpiringEmailFactory {

    private static final String SUBJECT = "AutoPulse - Vehicle document expiring soon";

    private final TemplateEmailRenderer templateEmailRenderer;
    private final String mailUsername;
    private final String supportEmail;

    public VehicleDocumentExpiringEmailFactory(
            TemplateEmailRenderer templateEmailRenderer,
            @Value("${spring.mail.username}") String mailUsername,
            @Value("${SUPPORT_EMAIL:}") String supportEmail
    ) {
        this.templateEmailRenderer = templateEmailRenderer;
        this.mailUsername = mailUsername;
        this.supportEmail = supportEmail;
    }

    public EmailMessage build(VehicleDocumentExpiringEvent event) {
        String effectiveSupportEmail = isBlank(supportEmail) ? mailUsername : supportEmail;

        Map<String, Object> contentModel = new HashMap<>();
        contentModel.put("recipientName", isBlank(event.recipientName()) ? "there" : event.recipientName());
        contentModel.put("documentId", event.documentId());
        contentModel.put("vehicleId", event.vehicleId());
        contentModel.put("documentType", event.documentType());
        contentModel.put("expiresAt", event.expiresAt());

        String contentHtml = templateEmailRenderer.render("emails/vehicle-document-expiring", contentModel);

        Map<String, Object> baseModel = new HashMap<>();
        baseModel.put("subject", SUBJECT);
        baseModel.put("year", Year.now().getValue());
        baseModel.put("supportEmail", effectiveSupportEmail);
        baseModel.put("contentHtml", contentHtml);

        String htmlBody = templateEmailRenderer.render("emails/base", baseModel);

        return new EmailMessage(
                event.recipientEmail(),
                effectiveSupportEmail,
                SUBJECT,
                htmlBody
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}