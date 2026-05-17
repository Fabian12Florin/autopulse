package com.autopulse.userservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient keycloakRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
