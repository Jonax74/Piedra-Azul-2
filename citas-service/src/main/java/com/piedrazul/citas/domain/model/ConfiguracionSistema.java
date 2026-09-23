package com.piedrazul.citas.domain.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class ConfiguracionSistema {

    private final Integer semanasDisponibles;
    private final Set<LocalDate> festivos;

    public ConfiguracionSistema(Integer semanasDisponibles) {
        this(semanasDisponibles, Set.of());
    }

    public ConfiguracionSistema(Integer semanasDisponibles, Set<LocalDate> festivos) {
        if (semanasDisponibles == null || semanasDisponibles <= 0) {
            throw new IllegalArgumentException("Semanas inválidas");
        }
        this.semanasDisponibles = semanasDisponibles;
        this.festivos = festivos == null || festivos.isEmpty()
                ? new TreeSet<>()
                : new TreeSet<>(festivos);
    }

    public Integer getSemanasDisponibles() {
        return semanasDisponibles;
    }

    public Set<LocalDate> getFestivos() {
        return Collections.unmodifiableSet(festivos);
    }

    public boolean esFestivo(LocalDate fecha) {
        return festivos.contains(fecha);
    }

    public ConfiguracionSistema conSemanas(Integer semanas) {
        return new ConfiguracionSistema(semanas, festivos);
    }

    public ConfiguracionSistema conFestivos(Set<LocalDate> nuevosFestivos) {
        return new ConfiguracionSistema(semanasDisponibles, nuevosFestivos);
    }
}
