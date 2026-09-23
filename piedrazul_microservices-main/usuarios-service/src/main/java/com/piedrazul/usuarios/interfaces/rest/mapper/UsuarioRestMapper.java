package com.piedrazul.usuarios.interfaces.rest.mapper;

import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.interfaces.rest.dto.response.UsuarioResponse;

public final class UsuarioRestMapper {

    private UsuarioRestMapper() {
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .keycloakUserId(usuario.getKeycloakUserId())
                .username(usuario.getUsername())
                .personaId(usuario.getPersonaId())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}
