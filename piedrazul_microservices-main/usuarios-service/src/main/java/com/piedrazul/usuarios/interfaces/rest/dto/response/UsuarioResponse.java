package com.piedrazul.usuarios.interfaces.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {

    private UUID id;
    private UUID keycloakUserId;
    private String username;
    private Long personaId;
    private Instant fechaCreacion;
    private Instant fechaActualizacion;
}
