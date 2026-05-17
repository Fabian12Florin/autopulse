package com.autopulse.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutesConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Value("${local.server.port}")
    private int port;

    @Test
    void routeLocator_shouldContainExpectedRoutes() {
        List<String> routeIds = routeLocator.getRoutes().toStream()
                .map(Route::getId)
                .toList();
        assertTrue(routeIds.contains("user-service"));
        assertTrue(routeIds.contains("delivery-execution-service"));
        assertTrue(routeIds.contains("fleet-service"));
        assertTrue(routeIds.contains("geography-service"));
        assertTrue(routeIds.contains("parcel-service"));
        assertTrue(routeIds.contains("routing-service"));
    }

    @Test
    void preflightRequest_shouldReturnCorsHeaders() {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .options()
                .uri("/api/users/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
    }
}
