package com.autopulse.geographyservice.config;

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
    public OpenAPI geographyServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AutoPulse Geography Service API")
                        .version("v1")
                        .description("Production-oriented API documentation for managing master geography data used across AutoPulse. " +
                                "The service owns regions and localities, exposes search and lookup capabilities, and provides " +
                                "reference data needed by parcel, fleet, routing, and other internal workflows.")
                        .contact(new Contact()
                                .name("AutoPulse Backend Team")
                                .email("backend@autopulse.local"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://autopulse.local/internal")))
                .servers(List.of(
                        new Server().url("http://localhost:8089").description("Local development"),
                        new Server().url("http://localhost:8765/geography-service").description("Behind API Gateway example")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token issued by Keycloak.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
