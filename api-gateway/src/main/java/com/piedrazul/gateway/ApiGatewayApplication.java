package com.piedrazul.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication implements CommandLineRunner {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:NOT_FOUND}")
    private String issuer;

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("ISSUER = " + issuer);
    }
}