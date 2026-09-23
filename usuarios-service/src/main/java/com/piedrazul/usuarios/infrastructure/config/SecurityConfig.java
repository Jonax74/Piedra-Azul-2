package com.piedrazul.usuarios.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Modelo de seguridad: GATEWAY-PERIMETRICO.
 *
 * A partir de la integracion con Keycloak (Sprint API Gateway + JWT), la autenticacion
 * y autorizacion son responsabilidad EXCLUSIVA del api-gateway. Este microservicio confia
 * en el gateway y atiende cualquier request que le llegue.
 *
 * Razones:
 *   1. El gateway es el unico punto de entrada publico. Los microservicios escuchan en
 *      127.0.0.1:8081 (solo accesibles desde la misma red local), no se exponen al exterior.
 *   2. Mantener httpBasic aqui choca con el modelo: el gateway forwardea Authorization: Bearer
 *      y este servicio no lo entiende, devolviendo 401 espurios incluso cuando el gateway
 *      ya autorizo correctamente.
 *   3. Spring Security esta presente (lo trae spring-boot-starter-security como dependencia
 *      transitiva); si no definimos un SecurityFilterChain explicito, Spring Boot activa el
 *      default lockdown. Por eso este bean existe: para DESACTIVAR el bloqueo, no para reforzarlo.
 *
 * TODO(seguridad-fase-4): activar defensa en profundidad cuando se haga el siguiente sprint:
 *   - Agregar spring-boot-starter-oauth2-resource-server.
 *   - Configurar spring.security.oauth2.resourceserver.jwt.issuer-uri al realm de Keycloak.
 *   - Restaurar reglas por rol aqui, replicando la matriz del gateway como red de seguridad.
 *   - Anotar controllers/servicios con @PreAuthorize donde aplique ownership.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}
