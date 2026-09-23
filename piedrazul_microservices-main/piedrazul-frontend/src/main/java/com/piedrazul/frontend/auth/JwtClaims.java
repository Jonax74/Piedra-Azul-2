package com.piedrazul.frontend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Base64;

/**
 * Extrae claims del payload de un JWT SIN validar firma.
 *
 * Por que no validar firma aqui:
 *   - El api-gateway ya valida la firma con la clave publica del realm (JWKS) en
 *     cada request entrante. Si el token llega manipulado, el gateway lo rechaza
 *     con 401 antes de hacer forward.
 *   - El frontend solo lee claims para fines de UI/sesion (mostrar username,
 *     redirigir por rol, obtener usuario_id). Confiar en estos claims sin firma
 *     desde el frontend es aceptable porque NO se usan para decisiones de
 *     autorizacion del servidor; esas las toma el gateway con el token completo.
 *   - Implementar validacion RS256 desde JavaFX requeriria descargar JWKS,
 *     parsear claves PEM y agregar nimbus-jose-jwt. Innecesario para el caso.
 *
 * Si en el futuro se requiere validacion local (por ej. para offline-mode), este
 * componente queda como punto de extension natural sin tocar el resto del codigo.
 */
public final class JwtClaims {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode payload;

    private JwtClaims(JsonNode payload) {
        this.payload = payload;
    }

    public static JwtClaims fromAccessToken(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("JWT vacio");
        }
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("JWT mal formado (no tiene payload)");
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
            return new JwtClaims(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo decodificar el JWT: " + e.getMessage(), e);
        }
    }

    public String getPreferredUsername() {
        return text("preferred_username");
    }

    public String getEmail() {
        return text("email");
    }

    public String getSubject() {
        return text("sub");
    }

    /**
     * UUID del usuario en el dominio del sistema (claim custom mapeado en Keycloak).
     * Es el principal name que usaremos del lado del backend.
     * Puede ser null para tokens emitidos antes de la migracion.
     */
    public String getUsuarioId() {
        return text("usuario_id");
    }

    /**
     * ID legacy de la persona en personas-service (Long). Claim custom OPCIONAL.
     * Necesario para AgendarCitaAutonomaController hasta que se complete la
     * migracion a UUIDs (Fase 3). Retorna null si no esta seteado en Keycloak.
     */
    public Long getPersonaId() {
        JsonNode n = payload.get("persona_id");
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.asLong();
        try {
            return Long.parseLong(n.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Roles del realm extraidos de realm_access.roles.
     * Filtra los roles internos de Keycloak para que el frontend solo vea los del negocio.
     */
    public List<String> getRealmRoles() {
        JsonNode realmAccess = payload.get("realm_access");
        if (realmAccess == null) return Collections.emptyList();
        JsonNode roles = realmAccess.get("roles");
        if (roles == null || !roles.isArray()) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        roles.forEach(r -> {
            String role = r.asText();
            if (!isKeycloakInternalRole(role)) {
                result.add(role);
            }
        });
        return Collections.unmodifiableList(result);
    }

    private static boolean isKeycloakInternalRole(String role) {
        return role.startsWith("default-roles-")
                || role.equals("offline_access")
                || role.equals("uma_authorization");
    }

    private String text(String claim) {
        JsonNode n = payload.get(claim);
        return (n == null || n.isNull()) ? null : n.asText();
    }
}
