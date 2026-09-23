package com.piedrazul.frontend.util;

import com.piedrazul.frontend.dto.response.CitaResponse;

import java.util.List;

public final class CitaProgramadaPolicy {

    private CitaProgramadaPolicy() {
    }

    public static boolean tieneCitaProgramada(List<CitaResponse> citas) {
        if (citas == null || citas.isEmpty()) {
            return false;
        }
        return citas.stream().anyMatch(CitaProgramadaPolicy::esCitaPendiente);
    }

    public static boolean esCitaPendiente(CitaResponse cita) {
        if (cita == null || cita.getEstado() == null) {
            return false;
        }
        String estado = cita.getEstado().trim();
        return "Programada".equalsIgnoreCase(estado) || "Reagendada".equalsIgnoreCase(estado);
    }

    public static String mensajeBloqueo() {
        return "El paciente ya tiene una cita programada o reagendada pendiente. "
                + "Debe cancelar o gestionar la cita existente antes de agendar otra.";
    }
}
