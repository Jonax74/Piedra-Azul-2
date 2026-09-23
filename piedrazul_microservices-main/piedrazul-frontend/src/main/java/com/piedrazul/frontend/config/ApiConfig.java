package com.piedrazul.frontend.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Resolucion de la URL base del API Gateway en cascada:
 *
 *   1. Variable de entorno PIEDRAZUL_GATEWAY_URL (ideal para Docker/CI)
 *   2. Propiedad piedrazul.gateway.url en classpath:application.properties
 *   3. Fallback a http://localhost:8085 (modo desarrollo sin configurar nada)
 *
 * El valor se resuelve una sola vez al inicio del proceso y se cachea.
 */
public final class ApiConfig {

    private static final String ENV_VAR = "PIEDRAZUL_GATEWAY_URL";
    private static final String PROPERTIES_FILE = "application.properties";
    private static final String PROPERTY_KEY = "piedrazul.gateway.url";
    private static final String DEFAULT_URL = "http://localhost:8085";

    private static volatile String cachedGatewayUrl;

    private ApiConfig() {
    }

    public static String gatewayBaseUrl() {
        String url = cachedGatewayUrl;
        if (url == null) {
            synchronized (ApiConfig.class) {
                url = cachedGatewayUrl;
                if (url == null) {
                    url = stripTrailingSlash(resolveUrl());
                    cachedGatewayUrl = url;
                }
            }
        }
        return url;
    }

    private static String resolveUrl() {
        String fromEnv = System.getenv(ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        try (InputStream in = ApiConfig.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String fromProps = props.getProperty(PROPERTY_KEY);
                if (fromProps != null && !fromProps.isBlank()) {
                    return fromProps.trim();
                }
            }
        } catch (IOException ignored) {
            // Si el archivo no existe o no se puede leer, caemos al default.
        }

        return DEFAULT_URL;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
