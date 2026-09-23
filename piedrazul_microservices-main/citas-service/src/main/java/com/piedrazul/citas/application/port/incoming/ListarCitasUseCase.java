package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.application.dto.response.CitaResponse;

import java.time.LocalDate;
import java.util.List;

public interface ListarCitasUseCase {

    List<CitaResponse> listar(Long medicoId, Long pacienteId, LocalDate fechaInicio, LocalDate fechaFin);
}