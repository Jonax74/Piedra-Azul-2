package com.piedrazul.frontend.util;

import com.piedrazul.frontend.dto.response.CitaResponse;

public final class CitaMotivoUtil {

    private static final String PREFIJO_CANCELACION = "Motivo de cancelación: ";

    private CitaMotivoUtil() {
    }

    public static String resolverMotivo(CitaResponse cita) {
        if (cita == null) {
            return "-";
        }

        if (esCancelada(cita) && tieneTexto(cita.getMotivoCancelacion())) {
            return PREFIJO_CANCELACION + cita.getMotivoCancelacion().trim();
        }

        if (tieneTexto(cita.getMotivoAgendamiento())) {
            return cita.getMotivoAgendamiento().trim();
        }

        if (tieneTexto(cita.getMotivoCancelacion())) {
            return PREFIJO_CANCELACION + cita.getMotivoCancelacion().trim();
        }

        return "-";
    }

    private static boolean esCancelada(CitaResponse cita) {
        return cita.getEstado() != null && "Cancelada".equalsIgnoreCase(cita.getEstado().trim());
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
