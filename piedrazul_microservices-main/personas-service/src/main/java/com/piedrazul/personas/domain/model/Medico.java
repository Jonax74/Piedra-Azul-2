package com.piedrazul.personas.domain.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Medico {

    private Long personaId;
    private TipoProfesional tipoProfesional;
    private EstadoMedico estado;
    private Integer intervaloMinutos;
    private Set<EspecialidadMedica> especialidades = new LinkedHashSet<>();

    public Medico() {
    }

    public Medico(Long personaId, TipoProfesional tipoProfesional, EstadoMedico estado) {
        validarPersonaId(personaId);
        validarTipoProfesional(tipoProfesional);
        validarEstado(estado);

        this.personaId = personaId;
        this.tipoProfesional = tipoProfesional;
        this.estado = estado;
    }

    public static Medico crear(Long personaId, TipoProfesional tipoProfesional) {
        return new Medico(personaId, tipoProfesional, EstadoMedico.ACTIVO);
    }

    public static Medico crear(Long personaId, TipoProfesional tipoProfesional, Set<EspecialidadMedica> especialidades) {
        Medico medico = crear(personaId, tipoProfesional);
        medico.asignarEspecialidades(especialidades);
        return medico;
    }

    public void asignarEspecialidades(Set<EspecialidadMedica> nuevasEspecialidades) {
        validarEspecialidades(nuevasEspecialidades);
        this.especialidades = new LinkedHashSet<>(nuevasEspecialidades);
    }

    public void cambiarEstado(EstadoMedico nuevoEstado) {
        validarEstado(nuevoEstado);
        this.estado = nuevoEstado;
    }

    private void validarPersonaId(Long personaId) {
        if (personaId == null || personaId <= 0) {
            throw new IllegalArgumentException("El personaId del médico es obligatorio");
        }
    }

    private void validarTipoProfesional(TipoProfesional tipoProfesional) {
        if (tipoProfesional == null) {
            throw new IllegalArgumentException("El tipo profesional es obligatorio");
        }
    }

    private void validarEstado(EstadoMedico estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del médico es obligatorio");
        }
    }

    private void validarEspecialidades(Set<EspecialidadMedica> especialidades) {
        if (especialidades == null || especialidades.isEmpty()) {
            throw new IllegalArgumentException("El médico debe tener al menos una especialidad");
        }
        if (especialidades.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Las especialidades no pueden contener valores nulos");
        }
    }

    public Set<EspecialidadMedica> getEspecialidades() {
        return Collections.unmodifiableSet(especialidades);
    }

    public void setEspecialidades(Set<EspecialidadMedica> especialidades) {
        if (especialidades == null || especialidades.isEmpty()) {
            this.especialidades = new LinkedHashSet<>();
            return;
        }
        validarEspecialidades(especialidades);
        this.especialidades = new LinkedHashSet<>(especialidades);
    }

    public boolean tieneEspecialidad(EspecialidadMedica especialidad) {
        return especialidades.contains(especialidad);
    }

    public Long getPersonaId() {
        return personaId;
    }

    public void setPersonaId(Long personaId) {
        validarPersonaId(personaId);
        this.personaId = personaId;
    }

    public TipoProfesional getTipoProfesional() {
        return tipoProfesional;
    }

    public void setTipoProfesional(TipoProfesional tipoProfesional) {
        validarTipoProfesional(tipoProfesional);
        this.tipoProfesional = tipoProfesional;
    }

    public EstadoMedico getEstado() {
        return estado;
    }

    public void setEstado(EstadoMedico estado) {
        validarEstado(estado);
        this.estado = estado;
    }

    public boolean estaActivo() {
        return EstadoMedico.ACTIVO.equals(this.estado);
    }

    public void actualizarIntervalo(Integer minutos) {
        if (minutos == null || minutos <= 0) {
            throw new IllegalArgumentException("Intervalo inválido");
        }
        this.intervaloMinutos = minutos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medico medico)) return false;
        return Objects.equals(personaId, medico.personaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personaId);
    }
}