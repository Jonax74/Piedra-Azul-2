package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.application.dto.request.CrearCitaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;

public interface CrearCitaAutonomaUseCase {
    CitaResponse crearCitaAutonoma(CrearCitaRequest request);
}
