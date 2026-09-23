package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.domain.valueobjects.PacienteId;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultarSlotsDisponiblesUseCase {
    List<LocalDateTime> consultar(MedicoId medicoId, PacienteId pacienteId);
}
