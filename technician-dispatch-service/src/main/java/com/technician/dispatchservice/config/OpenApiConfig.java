package com.technician.dispatchservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI technicianDispatchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HelpFlow - Technician Dispatch Service API")
                        .description("Microservicio simulado y determinista para la asignación y estimación de disponibilidad de técnicos.")
                        .version("1.0.0")
                        .contact(new Contact().name("HelpFlow Team").email("soporte@helpflow.com")));
    }
}
