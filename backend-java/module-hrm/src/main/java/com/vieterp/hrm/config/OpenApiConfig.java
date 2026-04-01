package com.vieterp.hrm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI vietErpOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("VietERP HRM API").version("1.0.0")
                .description("HRM module REST API for VietERP")
                .contact(new Contact().name("VietERP Team")));
    }
}
