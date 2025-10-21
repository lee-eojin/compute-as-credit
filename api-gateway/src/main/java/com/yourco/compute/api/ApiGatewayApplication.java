package com.yourco.compute.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.yourco.compute")
@EnableScheduling
public class ApiGatewayApplication {
  public static void main(String[] args) { SpringApplication.run(ApiGatewayApplication.class, args); }
}
