package com.autopulse.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    private static final String CORS_RESPONSE_HEADERS =
            "Access-Control-Allow-Origin Access-Control-Allow-Credentials";

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder) {

        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, "RETAIN_FIRST"))
                        .uri("lb://user-service"))
                .route("delivery-execution-service", r -> r
                        .path("/api/delivery-execution/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, "RETAIN_FIRST"))
                        .uri("lb://delivery-execution-service"))
                .route("fleet-service", r -> r
                        .path("/api/fleet/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, "RETAIN_FIRST"))
                        .uri("lb://fleet-service"))
                .route("geography-service", r -> r
                        .path("/api/geography/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, "RETAIN_FIRST"))
                        .uri("lb://geography-service"))
                .route("parcel-service", r -> r
                        .path("/api/parcels/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, "RETAIN_FIRST"))
                        .uri("lb://parcel-service"))
                .route("routing-service", r -> r
                        .path("/api/routing/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, "RETAIN_FIRST"))
                        .uri("lb://routing-service"))
                .build();
    }
}
