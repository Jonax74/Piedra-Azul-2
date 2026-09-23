package com.piedrazul.usuarios.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio del Usuario tras la integracion con Keycloak.
 *
 * Diseno post-migracion (Sprint Keycloak Fase 3):
 *   - {@code id}: UUID generado por NUESTRO sistema antes de crear el usuario en
 *     Keycloak. Se persiste tambien como atributo 'usuario_id' del usuario en
 *     Keycloak para que aparezca como claim del JWT. Es el ID de dominio que
 *     usan los demas microservicios (no el sub de Keycloak), lo que nos da
 *     independencia del IdP.
 *   - {@code keycloakUserId}: UUID asignado por Keycloak (el claim 'sub' del JWT).
 *     Lo guardamos para poder consultar/modificar el usuario contra el Admin API
 *     sin tener que hacer lookup por username.
 *   - {@code personaId}: Long, FK al personas-service. Mantenido para preservar
 *     la relacion de dominio Usuario ↔ Persona.
 *
 * Lo que YA NO esta aqui (lo gestiona Keycloak):
 *   - password / passwordHash    → credentials API de Keycloak
 *   - roles                       → realm_access.roles del JWT
 *   - estado (ACTIVO/INACTIVO)    → "enabled" del usuario en Keycloak
 *   - intentosFallidos / lockout  → "Brute Force Detection" nativo de Keycloak
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    private UUID id;
    private UUID keycloakUserId;
    private String username;
    private Long personaId;
    private Instant fechaCreacion;
    private Instant fechaActualizacion;
}
