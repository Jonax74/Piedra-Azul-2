package com.piedrazul.frontend.dto.response;

public class PacienteListItem {

    public static final Long NUEVO_PACIENTE_ID = -1L;

    private final Long personaId;
    private final String nombreCompleto;

    public static PacienteListItem nuevoPaciente() {
        return new PacienteListItem(NUEVO_PACIENTE_ID, "— Registrar nuevo paciente —");
    }

    public PacienteListItem(Long personaId, String nombreCompleto) {
        this.personaId = personaId;
        this.nombreCompleto = nombreCompleto;
    }

    public boolean esNuevoPaciente() {
        return NUEVO_PACIENTE_ID.equals(personaId);
    }

    public Long getPersonaId() {
        return personaId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    @Override
    public String toString() {
        return nombreCompleto + " - Paciente #" + personaId;
    }
}
