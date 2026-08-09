package com.yourco.compute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lives in the root package on purpose: entity and repository scanning follow the
 * auto-configuration package, which is this class's own, and the other modules sit underneath it.
 */
@SpringBootApplication
@EnableScheduling
public class ApiGatewayApplication {
  public static void main(String[] args) { SpringApplication.run(ApiGatewayApplication.class, args); }
}
