package com.piedrazul.frontend.util;

import com.piedrazul.frontend.dto.response.CitaResponse;

import java.util.List;

public final class ConsultaGeneralPolicy {

    private static final String ESTADO_ATENDIDA = "ATENDIDA";
    private static final String ESPECIALIDAD_GENERAL = "GENERAL";

    private ConsultaGeneralPolicy() {
    }

    public static boolean tieneConsultaGeneralAtendida(List<CitaResponse> citas) {
        if (citas == null || citas.isEmpty()) {
            return false;
        }
        return citas.stream().anyMatch(cita ->
                ESPECIALIDAD_GENERAL.equalsIgnoreCase(cita.getEspecialidad())
                        && ESTADO_ATENDIDA.equalsIgnoreCase(cita.getEstado())
        );
    }

    public static List<String> filtrarEspecialidadesDisponibles(
            List<String> candidatas,
            List<CitaResponse> historial
    ) {
        if (tieneConsultaGeneralAtendida(historial)) {
            return candidatas;
        }
        return candidatas.stream()
                .filter(ESPECIALIDAD_GENERAL::equals)
                .toList();
    }

    public static boolean especialidadPermitida(String especialidad, List<CitaResponse> historial) {
        if (especialidad == null || especialidad.isBlank()) {
            return false;
        }
        if (ESPECIALIDAD_GENERAL.equals(especialidad)) {
            return true;
        }
        return tieneConsultaGeneralAtendida(historial);
    }

    public static String mensajeRestriccion() {
        return "Los pacientes nuevos deben agendar primero una Consulta General. "
                + "Solo podrá solicitar Terapia Neural, Quiropraxia o Fisioterapia "
                + "después de atender esa consulta.";
    }
}
