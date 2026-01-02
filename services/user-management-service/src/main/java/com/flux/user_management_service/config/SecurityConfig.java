package com.flux.user_management_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // allow health/actuator (optional)
                        .requestMatchers("/actuator/**").permitAll()
                        // all other endpoints require valid JWT
                        .anyRequest().authenticated()
                )
                // enable OAuth2 Resource Server with JWT (autoconfigured via issuer-uri)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                // disable CSRF for stateless REST APIs
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}