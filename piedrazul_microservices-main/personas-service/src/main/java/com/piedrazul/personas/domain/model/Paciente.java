package com.piedrazul.personas.domain.model;

import java.util.Objects;

public class Paciente {

    private Long personaId;

    public Paciente() {
    }

    public Paciente(Long personaId) {
        validarPersonaId(personaId);
        this.personaId = personaId;
    }

    private void validarPersonaId(Long personaId) {
        if (personaId == null || personaId <= 0) {
            throw new IllegalArgumentException("El personaId del paciente es obligatorio");
        }
    }

    public Long getPersonaId() {
        return personaId;
    }

    public void setPersonaId(Long personaId) {
        validarPersonaId(personaId);
        this.personaId = personaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paciente paciente)) return false;
        return Objects.equals(personaId, paciente.personaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personaId);
    }
}