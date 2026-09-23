package com.piedrazul.personas.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Objects;

@Setter
@Getter
public class Disponibilidad {

    private Long id;
    private Long medicoId;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer intervaloMinutos;

    public Disponibilidad() {
    }

    public Disponibilidad(Long medicoId, String diaSemana, LocalTime horaInicio, LocalTime horaFin, Integer intervaloMinutos) {
        this.medicoId = medicoId;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.intervaloMinutos = intervaloMinutos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Disponibilidad that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}