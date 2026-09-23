package com.piedrazul.frontend.util;

import com.piedrazul.frontend.client.UsuarioClient;
import com.piedrazul.frontend.dto.response.UsuarioResponse;
import com.piedrazul.frontend.session.SessionManager;

/**
 * Resuelve el personaId del usuario logueado. Primero intenta el claim persona_id del JWT;
 * si no está (usuarios creados antes de mapear el claim), consulta usuarios-service.
 */
public final class SessionPersonaResolver {

    private static final UsuarioClient USUARIO_CLIENT = new UsuarioClient();

    private SessionPersonaResolver() {
    }

    public static Long resolverPersonaId() {
        Long personaId = SessionManager.getPersonaId();
        if (personaId != null) {
            return personaId;
        }

        String username = SessionManager.getUsername();
        if (username == null || username.isBlank()) {
            return null;
        }

        try {
            UsuarioResponse usuario = USUARIO_CLIENT.obtenerPorUsername(username);
            return usuario == null ? null : usuario.getPersonaId();
        } catch (Exception e) {
            return null;
        }
    }
}
