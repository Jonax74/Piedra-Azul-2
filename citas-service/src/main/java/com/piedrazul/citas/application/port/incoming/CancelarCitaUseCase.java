package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.application.dto.request.CancelarCitaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;

public interface CancelarCitaUseCase {
    CitaResponse cancelarCita(CancelarCitaRequest request);
}