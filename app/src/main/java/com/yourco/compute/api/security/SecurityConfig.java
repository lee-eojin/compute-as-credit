package com.yourco.compute.api.security;

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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${security.jwt.secret:#{null}}")
  private String jwtSecret;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
          .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
          .requestMatchers(HttpMethod.POST, "/v1/jobs/**").hasAnyAuthority("SCOPE_jobs:write")
          // HEAD is dispatched to the @GetMapping handler, so it needs the read scope too.
          .requestMatchers(HttpMethod.GET, "/v1/jobs/**").hasAnyAuthority("SCOPE_jobs:read")
          .requestMatchers(HttpMethod.HEAD, "/v1/jobs/**").hasAnyAuthority("SCOPE_jobs:read")
          .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(JwtSecret.toKey(jwtSecret)).build();
  }
}
