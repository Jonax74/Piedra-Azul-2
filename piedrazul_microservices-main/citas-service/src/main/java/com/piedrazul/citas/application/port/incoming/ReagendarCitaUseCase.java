package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.application.dto.request.ReagendarCitaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;

public interface ReagendarCitaUseCase {
    CitaResponse reagendarCita(ReagendarCitaRequest request);
}