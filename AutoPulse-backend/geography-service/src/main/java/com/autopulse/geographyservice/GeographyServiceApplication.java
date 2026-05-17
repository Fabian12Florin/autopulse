package com.autopulse.geographyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.autopulse")
public class GeographyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeographyServiceApplication.class, args);
    }

}
