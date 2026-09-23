package com.piedrazul.citas.domain.policy;

import com.piedrazul.citas.domain.exception.ConsultaGeneralRequeridaException;
import com.piedrazul.citas.domain.model.EspecialidadMedica;

public final class ConsultaGeneralPolicy {

    private ConsultaGeneralPolicy() {
    }

    public static void validarAgendamiento(
            EspecialidadMedica especialidad,
            boolean tieneConsultaGeneralAtendida
    ) {
        if (especialidad == null || especialidad == EspecialidadMedica.GENERAL) {
            return;
        }

        if (!tieneConsultaGeneralAtendida) {
            throw new ConsultaGeneralRequeridaException(
                    "Debe atender primero una Consulta General antes de agendar citas de "
                            + etiqueta(especialidad)
                            + ". Los pacientes nuevos deben iniciar con Medicina General."
            );
        }
    }

    private static String etiqueta(EspecialidadMedica especialidad) {
        return switch (especialidad) {
            case TERAPEUTA_NEURAL -> "Terapia Neural";
            case QUIROPRACTICO -> "Quiropraxia";
            case FISIOTERAPEUTA -> "Fisioterapia";
            case GENERAL -> "Medicina General";
        };
    }
}
