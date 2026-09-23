package com.piedrazul.usuarios.service.client;

public interface PersonaServiceClient {
    boolean existePersona(Integer personaId, boolean requiereUsuario);
}