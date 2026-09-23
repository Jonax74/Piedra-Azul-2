package com.piedrazul.citas.domain.policy;

import com.piedrazul.citas.domain.exception.PacienteNoDisponibleException;

public final class CitaProgramadaUnicaPolicy {

    private CitaProgramadaUnicaPolicy() {
    }

    public static void validar(boolean tieneCitaPendiente) {
        if (tieneCitaPendiente) {
            throw new PacienteNoDisponibleException(
                    "El paciente ya tiene una cita programada o reagendada pendiente. "
                            + "Debe cancelar o gestionar la cita existente antes de agendar otra."
            );
        }
    }
}
