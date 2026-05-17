package com.autopulse.deliveryexecutionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DeliveryExecutionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryExecutionServiceApplication.class, args);
	}

}
