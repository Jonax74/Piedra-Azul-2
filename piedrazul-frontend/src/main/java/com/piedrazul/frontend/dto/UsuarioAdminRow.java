package com.piedrazul.frontend.dto;

public class UsuarioAdminRow {

    private final String username;
    private final Long personaId;
    private final String nombreCompleto;
    private final String dni;

    public UsuarioAdminRow(String username, Long personaId, String nombreCompleto, String dni) {
        this.username = username;
        this.personaId = personaId;
        this.nombreCompleto = nombreCompleto;
        this.dni = dni;
    }

    public String getUsername() {
        return username;
    }

    public Long getPersonaId() {
        return personaId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getDni() {
        return dni;
    }
}
