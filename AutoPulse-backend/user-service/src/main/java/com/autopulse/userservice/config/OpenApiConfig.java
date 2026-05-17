package com.autopulse.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AutoPulse User Service API")
                        .version("v1")
                        .description("Production-oriented API documentation for authentication, user management, " +
                                "dispatcher management, courier management, and courier operational availability. " +
                                "Authentication is delegated to Keycloak, while the user-service owns business user data, " +
                                "profiles, active state, depot/region assignment, and courier availability.")
                        .contact(new Contact()
                                .name("AutoPulse Backend Team")
                                .email("backend@autopulse.local"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://autopulse.local/internal")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local development"),
                        new Server().url("http://localhost:8765/user-service").description("Behind API Gateway example")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token issued by Keycloak after login or refresh.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
