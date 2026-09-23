package com.piedrazul.frontend.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Resolucion de los parametros para hablar directo con Keycloak desde JavaFX.
 *
 * Cada parametro sigue la misma cascada (igual que ApiConfig):
 *   1. Variable de entorno  (ideal para Docker/CI)
 *   2. Propiedad en classpath:application.properties
 *   3. Default hardcoded     (modo desarrollo sin configurar nada)
 *
 * El frontend habla DIRECTAMENTE con Keycloak (no pasa por el Gateway) para
 * /token y /token (refresh). Es el patron estandar OIDC: el IdP es publico
 * conceptualmente. El Gateway protege APIs de negocio, no el endpoint de tokens.
 *
 * Valores resueltos una sola vez al inicio del proceso (cacheados, thread-safe).
 */
public final class KeycloakConfig {

    // --- URL base de Keycloak ---
    private static final String ENV_URL = "PIEDRAZUL_KEYCLOAK_URL";
    private static final String PROP_URL = "piedrazul.keycloak.url";
    private static final String DEFAULT_URL = "http://localhost:8080";

    // --- Realm ---
    private static final String ENV_REALM = "PIEDRAZUL_KEYCLOAK_REALM";
    private static final String PROP_REALM = "piedrazul.keycloak.realm";
    private static final String DEFAULT_REALM = "piedrazul";

    // --- Client ID (cliente publico de OIDC para la app JavaFX) ---
    private static final String ENV_CLIENT = "PIEDRAZUL_KEYCLOAK_CLIENT_ID";
    private static final String PROP_CLIENT = "piedrazul.keycloak.client-id";
    private static final String DEFAULT_CLIENT = "piedrazul-frontend";

    private static final String PROPERTIES_FILE = "application.properties";

    private static volatile String cachedBaseUrl;
    private static volatile String cachedRealm;
    private static volatile String cachedClientId;
    private static volatile Properties cachedProperties;

    private KeycloakConfig() {
    }

    public static String baseUrl() {
        String v = cachedBaseUrl;
        if (v == null) {
            synchronized (KeycloakConfig.class) {
                v = cachedBaseUrl;
                if (v == null) {
                    v = stripTrailingSlash(resolve(ENV_URL, PROP_URL, DEFAULT_URL));
                    cachedBaseUrl = v;
                }
            }
        }
        return v;
    }

    public static String realm() {
        String v = cachedRealm;
        if (v == null) {
            synchronized (KeycloakConfig.class) {
                v = cachedRealm;
                if (v == null) {
                    v = resolve(ENV_REALM, PROP_REALM, DEFAULT_REALM);
                    cachedRealm = v;
                }
            }
        }
        return v;
    }

    public static String clientId() {
        String v = cachedClientId;
        if (v == null) {
            synchronized (KeycloakConfig.class) {
                v = cachedClientId;
                if (v == null) {
                    v = resolve(ENV_CLIENT, PROP_CLIENT, DEFAULT_CLIENT);
                    cachedClientId = v;
                }
            }
        }
        return v;
    }

    /**
     * URL del endpoint que emite/refresca tokens del realm configurado.
     * Ej: http://localhost:8080/realms/piedrazul/protocol/openid-connect/token
     */
    public static String tokenEndpoint() {
        return baseUrl() + "/realms/" + realm() + "/protocol/openid-connect/token";
    }

    // ----------------------------------------------------------------------

    private static String resolve(String envKey, String propKey, String defaultValue) {
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        Properties props = loadProperties();
        String fromProps = props.getProperty(propKey);
        if (fromProps != null && !fromProps.isBlank()) {
            return fromProps.trim();
        }
        return defaultValue;
    }

    private static Properties loadProperties() {
        Properties p = cachedProperties;
        if (p == null) {
            synchronized (KeycloakConfig.class) {
                p = cachedProperties;
                if (p == null) {
                    p = new Properties();
                    try (InputStream in = KeycloakConfig.class
                            .getClassLoader()
                            .getResourceAsStream(PROPERTIES_FILE)) {
                        if (in != null) {
                            p.load(in);
                        }
                    } catch (IOException ignored) {
                        // Si no se puede leer, devolvemos Properties vacio (caemos a default).
                    }
                    cachedProperties = p;
                }
            }
        }
        return p;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
