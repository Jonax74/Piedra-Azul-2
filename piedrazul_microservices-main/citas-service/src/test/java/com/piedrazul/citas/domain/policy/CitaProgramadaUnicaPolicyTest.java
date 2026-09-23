package com.piedrazul.citas.domain.policy;

import com.piedrazul.citas.domain.exception.PacienteNoDisponibleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CitaProgramadaUnicaPolicyTest {

    @Test
    void permiteAgendarSiNoTieneCitaProgramada() {
        assertDoesNotThrow(() -> CitaProgramadaUnicaPolicy.validar(false));
    }

    @Test
    void rechazaSiYaTieneCitaPendiente() {
        assertThrows(
                PacienteNoDisponibleException.class,
                () -> CitaProgramadaUnicaPolicy.validar(true)
        );
    }
}
