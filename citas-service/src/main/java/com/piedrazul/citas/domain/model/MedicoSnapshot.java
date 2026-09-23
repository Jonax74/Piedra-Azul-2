package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.valueobjects.MedicoId;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MedicoSnapshot {
    private final MedicoId id;
    private final String nombreCompleto;
    private final String email;
    private final Set<EspecialidadMedica> especialidades;
    private final EstadoMedico estado;
    private final LocalDateTime actualizadoEn;

    public MedicoSnapshot(MedicoId id, String nombreCompleto,
                          String email, Set<EspecialidadMedica> especialidades, EstadoMedico estado) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.especialidades = especialidades == null || especialidades.isEmpty()
                ? Set.of(EspecialidadMedica.GENERAL)
                : Collections.unmodifiableSet(new LinkedHashSet<>(especialidades));
        this.estado = estado;
        this.actualizadoEn = LocalDateTime.now();
    }

    public boolean estaActivo() {
        return estado == EstadoMedico.ACTIVO;
    }

    public boolean tieneEspecialidad(EspecialidadMedica especialidad) {
        return especialidades.contains(especialidad);
    }

    public String getEspecialidadResumen() {
        return especialidades.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    public MedicoId getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getEmail() {
        return email;
    }

    public Set<EspecialidadMedica> getEspecialidades() {
        return especialidades;
    }

    public EstadoMedico getEstado() {
        return estado;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
