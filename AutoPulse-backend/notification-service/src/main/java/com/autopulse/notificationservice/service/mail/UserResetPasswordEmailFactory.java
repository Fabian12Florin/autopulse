package com.autopulse.notificationservice.service.mail;

import com.autopulse.common.kafka.event.UserResetPasswordEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserResetPasswordEmailFactory {

    private static final String SUBJECT = "AutoPulse - Your password has been reset";

    private final TemplateEmailRenderer templateEmailRenderer;
    private final String loginUrl;
    private final String mailUsername;
    private final String supportEmail;

    public UserResetPasswordEmailFactory(
            TemplateEmailRenderer templateEmailRenderer,
            @Value("${APP_LOGIN_URL:http://localhost:3000/login}") String loginUrl,
            @Value("${spring.mail.username}") String mailUsername,
            @Value("${SUPPORT_EMAIL:}") String supportEmail
    ) {
        this.templateEmailRenderer = templateEmailRenderer;
        this.loginUrl = loginUrl;
        this.mailUsername = mailUsername;
        this.supportEmail = supportEmail;
    }

    public EmailMessage build(UserResetPasswordEvent event) {
        String displayName = buildDisplayName(event.firstName(), event.lastName());
        String effectiveSupportEmail = isBlank(supportEmail) ? mailUsername : supportEmail;

        Map<String, Object> contentModel = new HashMap<>();
        contentModel.put("displayName", displayName);
        contentModel.put("email", event.email());
        contentModel.put("role", isBlank(event.role()) ? "USER" : event.role());
        contentModel.put("temporaryPassword", event.password());
        contentModel.put("loginUrl", loginUrl);

        String contentHtml = templateEmailRenderer.render("emails/user-reset-password", contentModel);

        Map<String, Object> baseModel = new HashMap<>();
        baseModel.put("subject", SUBJECT);
        baseModel.put("year", Year.now().getValue());
        baseModel.put("supportEmail", effectiveSupportEmail);
        baseModel.put("contentHtml", contentHtml);

        String htmlBody = templateEmailRenderer.render("emails/base", baseModel);

        return new EmailMessage(
                event.email(),
                effectiveSupportEmail,
                SUBJECT,
                htmlBody
        );
    }

    private String buildDisplayName(String firstName, String lastName) {
        String value = String.format("%s %s",
                firstName == null ? "" : firstName.trim(),
                lastName == null ? "" : lastName.trim()).trim();
        return value.isBlank() ? "there" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
