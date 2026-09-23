package com.piedrazul.frontend.http;

import com.piedrazul.frontend.config.ApiConfig;
import com.piedrazul.frontend.session.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Helper centralizado para hacer llamadas REST al API Gateway desde JavaFX.
 *
 * Razones de existir:
 *   1. Inyecta automaticamente Authorization: Bearer <access_token>.
 *   2. Hace refresh PROACTIVO: si el token vence en < 30s pide uno nuevo
 *      via SessionManager.getValidAccessToken() ANTES de la request.
 *   3. Hace retry REACTIVO: si igual recibe 401 (token revocado, reloj fuera de
 *      sincronia, race con un refresh ajeno), refresca una vez y reintenta UNA
 *      sola vez. Mas reintentos no tienen sentido y solo enmascararian errores.
 *   4. Centraliza timeouts y headers comunes para que cada *Client no los repita.
 *
 * Lo que NO hace:
 *   - No parsea JSON; eso queda en cada *Client (cada uno conoce su DTO).
 *   - No maneja URLs relativas con base url custom; el caller compone la URL completa
 *     a partir de ApiConfig.gatewayBaseUrl() como ya lo hace hoy.
 *   - No reabre la pantalla de login automaticamente; si la sesion se invalida,
 *     lanza HttpException con codigo 401 para que el caller decida (en general,
 *     el handler global puede capturarla y redirigir).
 *
 * Diseno: en lugar de exponer HttpURLConnection (que requiere recordar
 * connect/disconnect/timeouts), exponemos metodos por verbo HTTP que devuelven
 * un Response cerrado (status + body string). Eso elimina recursos pegados.
 *
 * Si en el futuro se cambia a java.net.http.HttpClient (nativo de Java 11+) o
 * a OkHttp, este es el unico archivo a tocar.
 */
public final class AuthenticatedHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public static String baseUrl() {
        return ApiConfig.gatewayBaseUrl();
    }

    // =====================================================================
    // API publica
    // =====================================================================

    public static Response get(String url) {
        return execute("GET", url, null, null);
    }

    public static Response post(String url, String jsonBody) {
        return execute("POST", url, jsonBody, "application/json");
    }

    public static Response put(String url, String jsonBody) {
        return execute("PUT", url, jsonBody, "application/json");
    }

    /** PUT sin cuerpo (p. ej. cancelar cita por query params). */
    public static Response putWithoutBody(String url) {
        return execute("PUT", url, null, null);
    }

    public static Response delete(String url) {
        return execute("DELETE", url, null, null);
    }

    public static Response patch(String url, String jsonBody) {
        // HttpURLConnection no soporta PATCH nativo en algunas JVMs; lo emulamos
        // con override de metodo. La mayoria de Spring lo respeta automaticamente.
        return execute("PATCH", url, jsonBody, "application/json");
    }

    // =====================================================================
    // Ejecucion con retry reactivo en 401
    // =====================================================================

    private static Response execute(String method, String url, String body, String contentType) {
        try {
            return executeOnce(method, url, body, contentType);
        } catch (HttpException e) {
            // Solo intentamos refresh + retry si hay sesion activa. Para llamadas
            // anonimas (registro publico) un 401 significa que el endpoint requiere
            // auth y no la dimos: no hay nada que refrescar.
            if (e.getStatusCode() == 401 && SessionManager.isLoggedIn()) {
                try {
                    SessionManager.refreshNow();
                } catch (IllegalStateException refreshFailed) {
                    throw e; // no se pudo refrescar; propagamos el 401 original
                }
                return executeOnce(method, url, body, contentType);
            }
            throw e;
        }
    }

    private static Response executeOnce(String method, String url, String body, String contentType) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod(emulatePatchIfNeeded(conn, method));
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");

            // Inyectamos Bearer solo si hay sesion activa. Si la pantalla actual es
            // publica (login / registro), simplemente no enviamos Authorization y
            // dejamos que el gateway decida si la ruta permite acceso anonimo.
            if (SessionManager.isLoggedIn()) {
                conn.setRequestProperty(AUTH_HEADER, BEARER_PREFIX + SessionManager.getValidAccessToken());
            }

            if (body != null) {
                conn.setDoOutput(true);
                if (contentType != null) {
                    conn.setRequestProperty("Content-Type", contentType);
                }
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = conn.getResponseCode();
            String responseBody = readBody(conn, status);

            if (status >= 200 && status < 300) {
                return new Response(status, responseBody);
            }
            throw new HttpException(status, responseBody);

        } catch (IOException e) {
            throw new HttpException(0, "Error de red llamando a " + url + ": " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Algunas implementaciones de HttpURLConnection (Sun) rechazan PATCH como
     * metodo. Si vemos PATCH, lo cambiamos a POST con header X-HTTP-Method-Override
     * que Spring respeta. Para los demas metodos, sin cambios.
     */
    private static String emulatePatchIfNeeded(HttpURLConnection conn, String method) {
        if ("PATCH".equalsIgnoreCase(method)) {
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            return "POST";
        }
        return method;
    }

    private static String readBody(HttpURLConnection conn, int status) throws IOException {
        InputStream stream = (status >= 200 && status < 400)
                ? conn.getInputStream()
                : conn.getErrorStream();
        if (stream == null) return "";
        try (InputStream s = stream) {
            return new String(s.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // =====================================================================
    // Tipos auxiliares
    // =====================================================================

    /** Respuesta HTTP ya consumida (sin recursos abiertos). */
    public static final class Response {
        private final int statusCode;
        private final String body;

        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
    }

    public static class HttpException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        public HttpException(int statusCode, String responseBody) {
            super("HTTP " + statusCode + ": " + responseBody);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public HttpException(int statusCode, String message, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
            this.responseBody = null;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
