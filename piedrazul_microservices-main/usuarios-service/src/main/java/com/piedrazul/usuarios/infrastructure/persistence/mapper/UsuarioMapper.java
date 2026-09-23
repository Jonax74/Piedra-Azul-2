package com.piedrazul.usuarios.infrastructure.persistence.mapper;

import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.infrastructure.persistence.entity.UsuarioEntity;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static Usuario toDomain(UsuarioEntity entity) {
        return Usuario.builder()
                .id(entity.getId())
                .keycloakUserId(entity.getKeycloakUserId())
                .username(entity.getUsername())
                .personaId(entity.getPersonaId())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }

    public static UsuarioEntity toEntity(Usuario usuario) {
        return UsuarioEntity.builder()
                .id(usuario.getId())
                .keycloakUserId(usuario.getKeycloakUserId())
                .username(usuario.getUsername())
                .personaId(usuario.getPersonaId())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}
