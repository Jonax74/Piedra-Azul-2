package com.piedrazul.frontend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Cliente para hablar con el endpoint OIDC /token del realm de Keycloak.
 *
 * Soporta dos flujos:
 *   - Resource Owner Password Credentials (login con usuario+password). Es lo que usa
 *     la pantalla de login de JavaFX hoy. No es lo mas moderno para apps web, pero es
 *     el flujo correcto para un cliente nativo de escritorio que ya implementa su
 *     propia pantalla de login. Migracion a Authorization Code + PKCE queda para futuro.
 *   - Refresh Token (renovar el access token sin volver a pedir password).
 *
 * El cliente Keycloak 'piedrazul-frontend' es PUBLIC (sin client_secret), apropiado
 * para apps nativas que no pueden guardar secretos con seguridad.
 *
 * Errores:
 *   - 401/403 de Keycloak: AuthException con mensaje "Credenciales invalidas" o similar.
 *   - errores de red / JSON: AuthException envolviendo la causa.
 *
 * No mantiene estado. Quien quiera guardar los tokens lo hace en {@code SessionManager}.
 */
public class KeycloakAuthClient {

    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenSet login(String username, String password) {
        String body = formUrlEncoded(
                "grant_type", "password",
                "client_id", KeycloakConfig.clientId(),
                "username", username,
                "password", password,
                "scope", "openid"
        );
        return postToken(body, "login");
    }

    public TokenSet refresh(String refreshToken) {
        String body = formUrlEncoded(
                "grant_type", "refresh_token",
                "client_id", KeycloakConfig.clientId(),
                "refresh_token", refreshToken
        );
        return postToken(body, "refresh");
    }

    // ----------------------------------------------------------------------

    private TokenSet postToken(String body, String operation) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(KeycloakConfig.tokenEndpoint()).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE_FORM);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                try (InputStream in = conn.getInputStream()) {
                    return parseTokenResponse(objectMapper.readTree(in));
                }
            }

            // Error: leer body de error para diagnostico.
            String errorBody = readErrorBody(conn);
            throw new AuthException(buildErrorMessage(operation, status, errorBody));

        } catch (IOException e) {
            throw new AuthException("No se pudo conectar con Keycloak (" + operation + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private TokenSet parseTokenResponse(JsonNode json) {
        String accessToken = textRequired(json, "access_token");
        String refreshToken = textRequired(json, "refresh_token");
        long expiresIn = json.path("expires_in").asLong(0);
        long refreshExpiresIn = json.path("refresh_expires_in").asLong(0);

        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(expiresIn);
        Instant refreshExpiresAt = refreshExpiresIn > 0 ? now.plusSeconds(refreshExpiresIn) : null;

        return new TokenSet(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }

    private String buildErrorMessage(String operation, int status, String errorBody) {
        // Keycloak responde con {"error":"invalid_grant","error_description":"Invalid user credentials"}
        try {
            JsonNode node = objectMapper.readTree(errorBody);
            String errorCode = nodeText(node, "error");
            String errorDesc = nodeText(node, "error_description");

            if ("invalid_grant".equals(errorCode)) {
                String lowerDesc = errorDesc.toLowerCase();
                if (lowerDesc.contains("not fully set up")) {
                    return "La cuenta aun no esta lista para iniciar sesion. "
                            + "Vuelva a registrarse o contacte al administrador.";
                }
                if ("login".equals(operation)) {
                    return "Usuario o contrasena incorrectos.";
                }
                return "Sesion expirada. Por favor inicia sesion nuevamente.";
            }
            if (!errorDesc.isBlank()) {
                return "Error de autenticacion (" + status + "): " + errorDesc;
            }
        } catch (Exception ignored) {
            // si no es JSON, caemos al mensaje generico
        }
        return "Error de autenticacion (" + status + "): " + errorBody;
    }

    private String readErrorBody(HttpURLConnection conn) {
        try {
            InputStream err = conn.getErrorStream();
            if (err == null) return "";
            return new String(err.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String nodeText(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? "" : n.asText();
    }

    private String textRequired(JsonNode json, String field) {
        JsonNode n = json.get(field);
        if (n == null || n.isNull()) {
            throw new AuthException("Respuesta de Keycloak sin campo requerido: " + field);
        }
        return n.asText();
    }

    private String formUrlEncoded(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Se esperan pares clave/valor");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) sb.append('&');
            sb.append(URLEncoder.encode(pairs[i], StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /**
     * Excepcion runtime para fallos de autenticacion. Permite al caller mostrar
     * el mensaje al usuario sin envolver en otra excepcion.
     */
    public static class AuthException extends RuntimeException {
        public AuthException(String message) {
            super(message);
        }
        public AuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
