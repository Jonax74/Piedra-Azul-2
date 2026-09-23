package com.piedrazul.frontend.session;

import com.piedrazul.frontend.auth.JwtClaims;
import com.piedrazul.frontend.auth.KeycloakAuthClient;
import com.piedrazul.frontend.auth.TokenSet;

import java.util.Collections;
import java.util.List;

/**
 * Sesion del usuario logueado. Singleton estatico (igual que la version anterior)
 * para que cualquier controller/cliente pueda acceder a los datos sin DI.
 *
 * Responsabilidades:
 *   1. Guardar los tokens emitidos por Keycloak (access + refresh + vencimientos).
 *   2. Exponer los claims utiles (username, roles, usuario_id, persona_id) parseados
 *      del access token una sola vez al iniciar sesion.
 *   3. Proveer un access token VIGENTE bajo demanda: si esta proximo a vencer,
 *      lo refresca transparentemente contra Keycloak.
 *
 * Thread-safety:
 *   - Las escrituras (start/clear/refresh) estan sincronizadas.
 *   - Las lecturas usan campos volatiles para que JavaFX vea cambios desde otros hilos.
 *
 * Nota de diseno: este componente NO conoce el HTTP de los microservicios; solo
 * sabe de Keycloak y de la sesion. El AuthenticatedHttpClient lo usa para inyectar
 * el header Authorization.
 */
public final class SessionManager {

    /** Segundos antes del vencimiento para gatillar refresh proactivo. */
    private static final long REFRESH_SKEW_SECONDS = 30;

    private static final KeycloakAuthClient AUTH_CLIENT = new KeycloakAuthClient();

    private static final Object LOCK = new Object();

    private static volatile TokenSet tokens;
    private static volatile JwtClaims claims;

    /** true cuando el registro se abrio desde el panel del administrador (no desde login). */
    private static volatile boolean registerFromAdminPanel;

    /** true cuando el agendamiento manual se abrio desde el dashboard del medico. */
    private static volatile boolean agendarManualComoMedico;

    private SessionManager() {
    }

    // =====================================================================
    // Ciclo de vida
    // =====================================================================

    /**
     * Inicia sesion con username/password contra Keycloak.
     * Lanza KeycloakAuthClient.AuthException con mensaje listo para mostrar al usuario.
     */
    public static void login(String username, String password) {
        TokenSet newTokens = AUTH_CLIENT.login(username, password);
        synchronized (LOCK) {
            tokens = newTokens;
            claims = JwtClaims.fromAccessToken(newTokens.getAccessToken());
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            tokens = null;
            claims = null;
        }
        registerFromAdminPanel = false;
        agendarManualComoMedico = false;
    }

    public static void beginRegisterFromAdminPanel() {
        registerFromAdminPanel = true;
    }

    public static boolean isRegisterFromAdminPanel() {
        return registerFromAdminPanel;
    }

    public static void endRegisterFromAdminPanel() {
        registerFromAdminPanel = false;
    }

    public static void beginAgendarManualComoMedico() {
        agendarManualComoMedico = true;
    }

    public static boolean isAgendarManualComoMedico() {
        return agendarManualComoMedico;
    }

    public static void endAgendarManualComoMedico() {
        agendarManualComoMedico = false;
    }

    public static boolean hasRole(String role) {
        return role != null && getRoles().contains(role);
    }

    public static boolean isLoggedIn() {
        return tokens != null && claims != null;
    }

    // =====================================================================
    // Acceso a datos de sesion (lectura)
    // =====================================================================

    public static String getUsername() {
        JwtClaims c = claims;
        return c == null ? null : c.getPreferredUsername();
    }

    public static String getEmail() {
        JwtClaims c = claims;
        return c == null ? null : c.getEmail();
    }

    public static List<String> getRoles() {
        JwtClaims c = claims;
        return c == null ? Collections.emptyList() : c.getRealmRoles();
    }

    /** Primer rol (usado por LoginController para redirigir a dashboard). */
    public static String getPrimaryRole() {
        List<String> r = getRoles();
        return r.isEmpty() ? null : r.get(0);
    }

    public static String getUsuarioId() {
        JwtClaims c = claims;
        return c == null ? null : c.getUsuarioId();
    }

    /**
     * ID legacy de la persona (Long). Puede ser null si el JWT no trae el claim
     * persona_id (los usuarios de prueba creados a mano en Keycloak no lo tienen
     * por defecto; se setea como atributo en el panel). Cuando se complete la
     * Fase 3 (KeycloakAdminClient + RegistrarUsuarioService refactorizado), este
     * claim se va a setear automaticamente en cada usuario creado.
     */
    public static Long getPersonaId() {
        JwtClaims c = claims;
        return c == null ? null : c.getPersonaId();
    }

    // =====================================================================
    // Token vigente (usado por AuthenticatedHttpClient en cada request)
    // =====================================================================

    /**
     * Devuelve un access token vigente. Si el actual esta proximo a vencer
     * (< REFRESH_SKEW_SECONDS), refresca contra Keycloak antes de devolverlo.
     *
     * Si no hay sesion activa, lanza IllegalStateException; quien llama deberia
     * forzar logout y mostrar la pantalla de login. Igual si el refresh token
     * tambien expiro o el refresh contra Keycloak falla.
     */
    public static String getValidAccessToken() {
        TokenSet snapshot = tokens;
        if (snapshot == null) {
            throw new IllegalStateException("No hay sesion activa.");
        }
        if (!snapshot.isAccessExpiringSoon(REFRESH_SKEW_SECONDS)) {
            return snapshot.getAccessToken();
        }
        return refreshNow().getAccessToken();
    }

    /**
     * Fuerza un refresh inmediato (usado por AuthenticatedHttpClient cuando una
     * llamada al gateway responde 401 a pesar del refresh proactivo, ej. token
     * revocado o reloj fuera de sincronia).
     */
    public static TokenSet refreshNow() {
        synchronized (LOCK) {
            TokenSet snapshot = tokens;
            if (snapshot == null) {
                throw new IllegalStateException("No hay sesion activa.");
            }
            if (snapshot.isRefreshExpired()) {
                clearInternal();
                throw new IllegalStateException("La sesion expiro. Inicia sesion nuevamente.");
            }
            try {
                TokenSet refreshed = AUTH_CLIENT.refresh(snapshot.getRefreshToken());
                tokens = refreshed;
                claims = JwtClaims.fromAccessToken(refreshed.getAccessToken());
                return refreshed;
            } catch (KeycloakAuthClient.AuthException e) {
                clearInternal();
                throw new IllegalStateException("La sesion expiro. Inicia sesion nuevamente.", e);
            }
        }
    }

    private static void clearInternal() {
        tokens = null;
        claims = null;
    }
}
