package com.piedrazul.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.http.HttpStatus;


/**
 * Configuracion de seguridad reactiva del API Gateway.
 *
 * Modelo:
 *   - El Gateway actua como OAuth2 Resource Server.
 *   - Cada request debe traer Authorization: Bearer <JWT> emitido por el realm 'piedrazul' de Keycloak,
 *     EXCEPTO las rutas explicitamente publicas (actuator, /api/auth/** temporal).
 *   - Spring Security valida la firma del JWT contra el JWKS expuesto por Keycloak (ver application.yml).
 *   - El converter KeycloakRealmRolesConverter mapea realm_access.roles -> ROLE_*.
 *
 * Errores:
 *   - 401 si falta el token o es invalido (HttpStatusServerEntryPoint).
 *   - 403 si el token es valido pero el rol no es suficiente (HttpStatusServerAccessDeniedHandler).
 *
 * CSRF: deshabilitado porque el Gateway no maneja sesiones HTML; los clientes son nativos (JavaFX)
 * o futuros SPAs que usan tokens Bearer (no cookies).
 *
 * La matriz de autorizacion vive aqui (no en application.yml) porque tener reglas como codigo nos
 * permite refactorizar con seguridad de tipos y tests si llega el caso.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        // --- Infraestructura: actuator publico para health/diagnostico ---
                        .pathMatchers("/actuator/**").permitAll()

                        // --- CORS preflight ---
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- Usuarios ---
                        // DECISION DE NEGOCIO (paridad con el sistema actual sin auth):
                        //   - POST: registro autoservicio. Un PACIENTE crea su propia cuenta;
                        //     no hay distincion de quien registra (paciente, agendador, admin).
                        //   - PUT/DELETE: tambien libres por compatibilidad con el flujo actual.
                        //   - GET: requiere autenticacion (los datos del listado son sensibles:
                        //     emails, nombres, cedulas). Un usuario logueado ya tiene identidad.
                        // TODO(seguridad-fase-2): endurecer cuando se separe el flujo de roles:
                        //   - POST publico solo para auto-registro de PACIENTE (rol fijo).
                        //   - Registro de MEDICO_TERAPISTA/AGENDADOR via endpoint ADMINISTRADOR.
                        //   - PUT/DELETE con ownership: solo el propio usuario o ADMINISTRADOR.
                        .pathMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/usuarios/**").permitAll()
                        .pathMatchers(HttpMethod.DELETE, "/api/usuarios/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/usuarios/**").authenticated()

                        // --- Personas y Pacientes (datos personales sensibles) ---
                        // POST publico: el auto-registro de un PACIENTE crea su propia Persona y
                        // su propio Paciente antes de tener sesion (el JWT lo emitira Keycloak
                        // recien tras el login). Mismo TODO de endurecimiento que /api/usuarios:
                        // separar auto-registro de PACIENTE (publico) vs creacion por staff
                        // (ADMIN/AGENDADOR autenticado) cuando definamos el flujo final.
                        .pathMatchers(HttpMethod.POST, "/api/personas").permitAll()
                        .pathMatchers(HttpMethod.DELETE, "/api/personas/*/registro-fallido").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/personas/dni/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/pacientes").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/personas/**", "/api/pacientes/**").authenticated()
                        .pathMatchers("/api/personas/**", "/api/pacientes/**")
                            .hasAnyRole("ADMINISTRADOR", "AGENDADOR", "MEDICO_TERAPISTA")

                        // --- Medicos ---
                        // POST publico por la misma razon de auto-registro (un usuario que se
                        // registra como MEDICO_TERAPISTA necesita crear su Medico antes del login).
                        // TODO: en produccion, los medicos deberian ser dados de alta por un
                        // ADMINISTRADOR autenticado, no por auto-registro.
                        .pathMatchers(HttpMethod.POST, "/api/medicos").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/medicos/**").authenticated()
                        .pathMatchers("/api/medicos/**").hasRole("ADMINISTRADOR")

                        // --- Disponibilidad de medicos ---
                        // Lectura abierta a cualquier usuario autenticado (pacientes consultan slots).
                        // Escritura solo medico (su propia agenda) o administrador.
                        .pathMatchers(HttpMethod.GET, "/api/disponibilidad/**").authenticated()
                        .pathMatchers("/api/disponibilidad/**")
                            .hasAnyRole("ADMINISTRADOR", "MEDICO_TERAPISTA")

                        // --- Configuracion del sistema ---
                        .pathMatchers(HttpMethod.GET, "/api/configuracion/**").authenticated()
                        .pathMatchers("/api/configuracion/**").hasRole("ADMINISTRADOR")

                        // --- Citas ---
                        // Cualquier usuario autenticado puede operar sobre citas; la validacion fina
                        // de ownership (un paciente solo cancela sus propias citas) se hace en
                        // citas-service usando el claim usuario_id del JWT.
                        .pathMatchers("/api/citas/**").authenticated()

                        // --- Notificaciones ---
                        .pathMatchers("/api/notificaciones/**").authenticated()

                        // --- Cualquier otra cosa: denegado por defecto ---
                        .anyExchange().denyAll()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(
                                        new KeycloakRealmRolesConverter()
                                )
                        ))
                )

                // Respuestas explicitas en HTTP para clientes nativos (no redirige a /login).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )

                .build();
    }
}
