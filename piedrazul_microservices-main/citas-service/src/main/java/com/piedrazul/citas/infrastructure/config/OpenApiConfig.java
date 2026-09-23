package com.piedrazul.citas.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI citasServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Piedrazul - Citas Service API")
                        .description("Documentacion REST del microservicio de agendamiento y configuracion de citas.")
                        .version("v1")
                        .contact(new Contact().name("Equipo Piedrazul"))
                        .license(new License().name("Uso academico")));
    }
}
