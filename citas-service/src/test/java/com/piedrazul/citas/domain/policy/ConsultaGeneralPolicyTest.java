package com.piedrazul.citas.domain.policy;

import com.piedrazul.citas.domain.exception.ConsultaGeneralRequeridaException;
import com.piedrazul.citas.domain.model.EspecialidadMedica;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultaGeneralPolicyTest {

    @Test
    void permiteConsultaGeneralSinHistorial() {
        assertDoesNotThrow(() ->
                ConsultaGeneralPolicy.validarAgendamiento(EspecialidadMedica.GENERAL, false)
        );
    }

    @Test
    void rechazaEspecialidadRestringidaSinConsultaGeneralAtendida() {
        assertThrows(
                ConsultaGeneralRequeridaException.class,
                () -> ConsultaGeneralPolicy.validarAgendamiento(EspecialidadMedica.TERAPEUTA_NEURAL, false)
        );
    }

    @Test
    void permiteEspecialidadRestringidaConConsultaGeneralAtendida() {
        assertDoesNotThrow(() ->
                ConsultaGeneralPolicy.validarAgendamiento(EspecialidadMedica.FISIOTERAPEUTA, true)
        );
    }
}
