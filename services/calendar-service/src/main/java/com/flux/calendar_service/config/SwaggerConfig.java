package com.flux.calendar_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI calendarServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Calendar Service API")
                        .description("API documentation for the Calendar Service")
                        .version("1.0"));
    }
}
