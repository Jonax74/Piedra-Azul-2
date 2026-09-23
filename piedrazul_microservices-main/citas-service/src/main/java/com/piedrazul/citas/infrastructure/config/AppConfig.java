package com.piedrazul.citas.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "com.piedrazul.citas")
public class AppConfig {
    // Configuración adicional de la aplicación
}