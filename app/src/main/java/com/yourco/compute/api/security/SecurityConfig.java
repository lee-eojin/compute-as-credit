package com.yourco.compute.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  @Value("${security.jwt.secret:#{null}}")
  private String jwtSecret;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
          .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
          .requestMatchers(HttpMethod.POST, "/v1/jobs/**").hasAnyAuthority("SCOPE_jobs:write")
          .requestMatchers(HttpMethod.GET, "/v1/jobs/**").hasAnyAuthority("SCOPE_jobs:read")
          .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException(
        "JWT secret is not configured. Set environment variable JWT_SECRET or property security.jwt.secret. " +
        "For development, use: export JWT_SECRET=dev-secret"
      );
    }
    if ("dev-secret".equals(jwtSecret)) {
      log.warn("Using default JWT secret 'dev-secret'. DO NOT USE IN PRODUCTION!");
    }
    byte[] keyBytes = jwtSecret.getBytes();
    var key = new SecretKeySpec(keyBytes, "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }
}
