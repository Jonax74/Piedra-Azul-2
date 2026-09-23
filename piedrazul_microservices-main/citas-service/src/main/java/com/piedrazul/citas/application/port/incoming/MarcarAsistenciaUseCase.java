package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.application.dto.request.MarcarAsistenciaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;

public interface MarcarAsistenciaUseCase {
    CitaResponse marcarAsistencia(MarcarAsistenciaRequest request);
}