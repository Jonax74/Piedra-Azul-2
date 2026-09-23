package com.piedrazul.frontend.auth;

import java.time.Instant;

/**
 * Conjunto de tokens devueltos por Keycloak tras un login o un refresh.
 *
 * Inmutable. La instancia se reemplaza completa cuando ocurre un refresh.
 *
 * Campos derivados:
 *   - accessExpiresAt: instante absoluto en que vence el access token. Se calcula
 *     una vez al recibir la respuesta (en lugar de guardar expires_in y restar
 *     contra el reloj cada vez) para evitar drift acumulado.
 *
 * El refresh token tiene su propio expires_in (refresh_expires_in en Keycloak).
 * Lo guardamos por simetria aunque hoy no lo usamos para decidir cuando re-loguear
 * (eso queda como mejora: si el refresh esta vencido, forzar pantalla de login).
 */
public final class TokenSet {

    private final String accessToken;
    private final String refreshToken;
    private final Instant accessExpiresAt;
    private final Instant refreshExpiresAt;

    public TokenSet(
            String accessToken,
            String refreshToken,
            Instant accessExpiresAt,
            Instant refreshExpiresAt
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Instant getAccessExpiresAt() {
        return accessExpiresAt;
    }

    public Instant getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    /**
     * Indica si el access token vence en menos de {@code skewSeconds} segundos.
     * Usado para decidir refresh proactivo antes de llamar al gateway y evitar
     * carreras de "el token vencio durante el viaje de red".
     */
    public boolean isAccessExpiringSoon(long skewSeconds) {
        return Instant.now().plusSeconds(skewSeconds).isAfter(accessExpiresAt);
    }

    public boolean isRefreshExpired() {
        return refreshExpiresAt != null && Instant.now().isAfter(refreshExpiresAt);
    }
}
