package com.piedrazul.notifications.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationsServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Piedrazul - Notifications Service API")
                        .description("Documentacion REST del microservicio de notificaciones.")
                        .version("v1")
                        .contact(new Contact().name("Equipo Piedrazul"))
                        .license(new License().name("Uso academico")));
    }
}
