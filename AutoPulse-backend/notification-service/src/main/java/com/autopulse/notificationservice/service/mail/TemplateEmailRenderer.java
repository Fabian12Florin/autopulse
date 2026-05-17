package com.autopulse.notificationservice.service.mail;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Component
public class TemplateEmailRenderer {

    private final TemplateEngine templateEngine;

    public TemplateEmailRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String template, Map<String, Object> model) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(model);
        return templateEngine.process(template, context);
    }
}
