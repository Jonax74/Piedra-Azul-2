package com.piedrazul.frontend.util;

import java.time.LocalDate;

public final class RangoFechasUtil {

    public static final String MSG_RANGO_INVALIDO =
            "El rango de fechas no es válido: fecha final debe ser mayor a la fecha inicial";

    private RangoFechasUtil() {
    }

    public static boolean esRangoInvalido(LocalDate fechaInicio, LocalDate fechaFin) {
        return fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin);
    }
}
