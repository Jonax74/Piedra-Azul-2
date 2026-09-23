package com.piedrazul.citas.application.port.outgoing;

import com.piedrazul.citas.domain.model.Cita;
import com.piedrazul.citas.domain.model.MedicoSnapshot;
import com.piedrazul.citas.domain.model.PacienteSnapshot;

import java.time.LocalDateTime;

public interface CitaEventPublisherPort {

    void publicarCitaAgendada(Cita cita, PacienteSnapshot paciente, MedicoSnapshot medico);

    void publicarCitaCancelada(Cita cita, PacienteSnapshot paciente, MedicoSnapshot medico);

    void publicarCitaReagendada(Cita cita,
                                LocalDateTime fechaHoraOriginal,
                                PacienteSnapshot paciente,
                                MedicoSnapshot medico);
}