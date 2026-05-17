package com.autopulse.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Test
    public void testMain() {
        System.setProperty("spring.profiles.active", "test");
        System.setProperty("spring.config.location", "classpath:application-test.yaml");
        ApiGatewayApplication.main(new String[]{});
    }

}
