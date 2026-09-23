package com.piedrazul.usuarios.infrastructure.keycloak;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente del Admin REST API de Keycloak.
 *
 * Responsabilidades:
 *   1. Autenticarse contra Keycloak como service account (client_credentials)
 *      y mantener un access token cacheado, renovandolo proactivamente cuando
 *      esta proximo a vencer.
 *   2. Crear usuarios (POST /admin/realms/{realm}/users) con atributos custom
 *      (usuario_id, persona_id) que apareceran como claims en el JWT.
 *   3. Setear password permanente (PUT .../reset-password con temporary=false).
 *   4. Asignar realm roles al usuario recien creado.
 *   5. Eliminar usuario (DELETE) — usado como rollback si falla el guardado en
 *      la BD local del usuarios-service.
 *
 * Manejo de errores:
 *   - 409 Conflict al crear usuario (username/email ya existe) → traduce a
 *     UsuarioYaExisteException para que el caller responda 409 al cliente final.
 *   - Otros 4xx/5xx → KeycloakAdminException con el body de respuesta.
 *
 * Thread-safety:
 *   - Las llamadas son stateless excepto el cache de token, que se actualiza
 *     bajo synchronized.
 */
@Component
public class KeycloakAdminClient {

    /** Segundos antes del vencimiento para gatillar refresh del token de admin. */
    private static final long REFRESH_SKEW_SECONDS = 30;

    private final RestTemplate restTemplate;
    private final KeycloakAdminProperties props;

    private final Object tokenLock = new Object();
    private volatile String cachedAccessToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminClient(RestTemplate restTemplate, KeycloakAdminProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    // =====================================================================
    // API publica
    // =====================================================================

    /**
     * Crea un usuario en Keycloak y devuelve su UUID (el "sub" del JWT).
     *
     * @param attributes atributos custom; cada valor se serializa como un array
     *                   de un solo elemento (Keycloak siempre acepta arrays).
     */
    public UUID crearUsuario(
            String username,
            String email,
            String firstName,
            String lastName,
            Map<String, String> attributes,
            String password
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("email", emailNormalizado(username, email));
        body.put("firstName", nombreNormalizado(firstName, username));
        body.put("lastName", nombreNormalizado(lastName, username));
        body.put("enabled", true);
        body.put("emailVerified", true);
        body.put("requiredActions", List.of());
        body.put("attributes", toMultiValueAttributes(attributes));
        body.put("credentials", List.of(credencialPassword(password)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, bearerHeaders());

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    props.adminUsersEndpoint(),
                    HttpMethod.POST,
                    request,
                    Void.class
            );
            return extractUserIdFromLocation(response);

        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new UsuarioYaExisteException(
                        "El username o email ya estan registrados en Keycloak.");
            }
            throw new KeycloakAdminException(
                    "Error creando usuario en Keycloak (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Setea password permanente (no temporal) para el usuario.
     */
    public void setPasswordPermanente(UUID keycloakUserId, String password) {
        Map<String, Object> body = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        String url = props.adminUsersEndpoint() + "/" + keycloakUserId + "/reset-password";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, bearerHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
            limpiarAccionesRequeridas(keycloakUserId);
        } catch (HttpStatusCodeException e) {
            throw new KeycloakAdminException(
                    "Error seteando password (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Keycloak 26 puede dejar UPDATE_PASSWORD activo aun con temporary=false.
     * El grant type password del frontend falla hasta limpiar esas acciones.
     */
    public void limpiarAccionesRequeridas(UUID keycloakUserId) {
        String url = props.adminUsersEndpoint() + "/" + keycloakUserId;
        Map<String, Object> body = Map.of("requiredActions", List.of());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, bearerHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new KeycloakAdminException(
                    "Error limpiando acciones requeridas (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Asigna realm roles al usuario por nombre. Hace primero un GET para resolver
     * cada nombre a su ID interno, luego POST al endpoint role-mappings.
     */
    public void asignarRealmRoles(UUID keycloakUserId, List<String> rolNames) {
        if (rolNames == null || rolNames.isEmpty()) {
            return;
        }

        List<Map<String, Object>> rolesPayload = rolNames.stream()
                .map(this::obtenerRealmRole)
                .toList();

        String url = props.adminUsersEndpoint() + "/" + keycloakUserId + "/role-mappings/realm";
        HttpEntity<List<Map<String, Object>>> request =
                new HttpEntity<>(rolesPayload, bearerHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new KeycloakAdminException(
                    "Error asignando roles (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Elimina un usuario de Keycloak. Idempotente: si no existe, no lanza.
     * Usado como compensacion ante fallo de la transaccion local.
     */
    public void eliminarUsuario(UUID keycloakUserId) {
        String url = props.adminUsersEndpoint() + "/" + keycloakUserId;
        HttpEntity<Void> request = new HttpEntity<>(bearerHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return;
            }
            throw new KeycloakAdminException(
                    "Error eliminando usuario (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    // =====================================================================
    // Internals
    // =====================================================================

    private Map<String, Object> obtenerRealmRole(String roleName) {
        String url = props.adminRolesEndpoint() + "/" + roleName;
        HttpEntity<Void> request = new HttpEntity<>(bearerHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<?, ?> body = resp.getBody();
            if (body == null) {
                throw new KeycloakAdminException("Rol no encontrado en Keycloak: " + roleName);
            }
            // Devolvemos solo los campos que role-mappings/realm necesita: id y name.
            return Map.of(
                    "id", body.get("id"),
                    "name", body.get("name")
            );
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new KeycloakAdminException("Rol no encontrado en Keycloak: " + roleName);
            }
            throw new KeycloakAdminException(
                    "Error consultando rol '" + roleName + "' (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    private UUID extractUserIdFromLocation(ResponseEntity<Void> response) {
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new KeycloakAdminException(
                    "Keycloak no devolvio Location header tras crear el usuario.");
        }
        String path = location.getPath();
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            throw new KeycloakAdminException("Location header invalido: " + location, e);
        }
    }

    private Map<String, Object> credencialPassword(String password) {
        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);
        return credential;
    }

    private String emailNormalizado(String username, String email) {
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return username.trim().toLowerCase() + "@piedrazul.local";
    }

    private String nombreNormalizado(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback.trim();
    }

    private MultiValueMap<String, String> toMultiValueAttributes(Map<String, String> attributes) {
        MultiValueMap<String, String> mv = new LinkedMultiValueMap<>();
        if (attributes != null) {
            attributes.forEach((k, v) -> {
                if (v != null) {
                    mv.add(k, v);
                }
            });
        }
        return mv;
    }

    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getValidAdminToken());
        return headers;
    }

    private String getValidAdminToken() {
        String snapshot = cachedAccessToken;
        if (snapshot != null && !isTokenExpiringSoon()) {
            return snapshot;
        }
        synchronized (tokenLock) {
            if (cachedAccessToken == null || isTokenExpiringSoon()) {
                refreshAdminToken();
            }
            return cachedAccessToken;
        }
    }

    private boolean isTokenExpiringSoon() {
        return Instant.now().plusSeconds(REFRESH_SKEW_SECONDS).isAfter(cachedTokenExpiresAt);
    }

    private void refreshAdminToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", props.getClientId());
        body.add("client_secret", props.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    props.tokenEndpoint(), request, Map.class);

            HttpStatusCode code = resp.getStatusCode();
            Map<?, ?> respBody = resp.getBody();
            if (!code.is2xxSuccessful() || respBody == null) {
                throw new KeycloakAdminException(
                        "No se pudo obtener token admin de Keycloak: status " + code);
            }
            String token = (String) respBody.get("access_token");
            Number expiresIn = (Number) respBody.get("expires_in");
            if (token == null || expiresIn == null) {
                throw new KeycloakAdminException("Respuesta de Keycloak sin access_token/expires_in.");
            }
            this.cachedAccessToken = token;
            this.cachedTokenExpiresAt = Instant.now().plusSeconds(expiresIn.longValue());

        } catch (HttpStatusCodeException e) {
            throw new KeycloakAdminException(
                    "Error de credenciales con piedrazul-admin-cli (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    // =====================================================================
    // Excepciones de dominio
    // =====================================================================

    public static class KeycloakAdminException extends RuntimeException {
        public KeycloakAdminException(String message) { super(message); }
        public KeycloakAdminException(String message, Throwable cause) { super(message, cause); }
    }

    public static class UsuarioYaExisteException extends RuntimeException {
        public UsuarioYaExisteException(String message) { super(message); }
    }
}