package com.flux.calendar_service.zoom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ZoomConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
