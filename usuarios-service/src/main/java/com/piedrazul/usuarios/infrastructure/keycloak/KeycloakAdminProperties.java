package com.piedrazul.usuarios.infrastructure.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros para que usuarios-service hable con el Admin REST API de Keycloak.
 *
 * Se inyecta como bean (ver AppConfig). Las propiedades viven en
 * application.properties bajo el prefijo 'piedrazul.keycloak.admin', y como
 * siempre, cada una se puede sobreescribir con variable de entorno (Spring Boot
 * traduce 'piedrazul.keycloak.admin.client-secret' a
 * PIEDRAZUL_KEYCLOAK_ADMIN_CLIENT_SECRET automaticamente).
 *
 * EN PRODUCCION, el client_secret DEBE venir por variable de entorno o un
 * vault, NUNCA hardcodeado en application.properties commiteado al repo.
 * En este sprint, lo dejamos en properties como placeholder y dependemos del
 * env var en cualquier entorno serio.
 */
@ConfigurationProperties(prefix = "piedrazul.keycloak.admin")
public class KeycloakAdminProperties {

    /** URL base de Keycloak. Ej: http://localhost:8080 */
    private String url;

    /** Realm donde viven los usuarios. Ej: piedrazul */
    private String realm;

    /** Client ID del cliente confidential con service account. */
    private String clientId;

    /** Client secret del cliente anterior. */
    private String clientSecret;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String tokenEndpoint() {
        return stripSlash(url) + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String adminUsersEndpoint() {
        return stripSlash(url) + "/admin/realms/" + realm + "/users";
    }

    public String adminRolesEndpoint() {
        return stripSlash(url) + "/admin/realms/" + realm + "/roles";
    }

    private String stripSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }
}
