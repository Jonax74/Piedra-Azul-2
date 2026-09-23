package com.piedrazul.citas.application.util;

import java.time.DayOfWeek;
import java.util.Map;

public final class DiaSemanaMapper {

    private static final Map<String, DayOfWeek> DIAS = Map.ofEntries(
            Map.entry("LUNES", DayOfWeek.MONDAY),
            Map.entry("MARTES", DayOfWeek.TUESDAY),
            Map.entry("MIERCOLES", DayOfWeek.WEDNESDAY),
            Map.entry("MIÉRCOLES", DayOfWeek.WEDNESDAY),
            Map.entry("JUEVES", DayOfWeek.THURSDAY),
            Map.entry("VIERNES", DayOfWeek.FRIDAY),
            Map.entry("SABADO", DayOfWeek.SATURDAY),
            Map.entry("SÁBADO", DayOfWeek.SATURDAY),
            Map.entry("DOMINGO", DayOfWeek.SUNDAY)
    );

    private DiaSemanaMapper() {
    }

    public static DayOfWeek toDayOfWeek(String diaSemana) {
        DayOfWeek dia = DIAS.get(diaSemana.toUpperCase());
        if (dia == null) {
            throw new IllegalArgumentException("Día de la semana no válido: " + diaSemana);
        }
        return dia;
    }
}
