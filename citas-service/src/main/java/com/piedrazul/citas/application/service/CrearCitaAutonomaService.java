package com.piedrazul.citas.application.service;

import com.piedrazul.citas.application.dto.request.CrearCitaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;
import com.piedrazul.citas.application.mapper.CitaApplicationMapper;
import com.piedrazul.citas.application.port.incoming.CrearCitaAutonomaUseCase;
import com.piedrazul.citas.application.port.outgoing.*;
import com.piedrazul.citas.application.service.agendamiento.AbstractAgendamientoService;
import com.piedrazul.citas.application.service.singleton.ConfiguracionManager;
import com.piedrazul.citas.domain.factory.CitaAutonomaFactory;
import com.piedrazul.citas.domain.model.*;
import org.springframework.stereotype.Service;

@Service
public class CrearCitaAutonomaService
        extends AbstractAgendamientoService
        implements CrearCitaAutonomaUseCase {

    public CrearCitaAutonomaService(
            CitaRepositoryPort citaRepository,
            PacienteSnapshotRepositoryPort pacienteRepository,
            MedicoSnapshotRepositoryPort medicoRepository,
            DisponibilidadSnapshotRepositoryPort disponibilidadRepository,
            CitaEventPublisherPort eventPublisher,
            CitaApplicationMapper mapper,
            CitaAutonomaFactory citaAutonomaFactory,
            ConfiguracionManager configuracionManager
    ) {
        super(
                citaRepository,
                pacienteRepository,
                medicoRepository,
                disponibilidadRepository,
                eventPublisher,
                mapper,
                citaAutonomaFactory,
                configuracionManager
        );
    }

    @Override
    protected void validarTipoAgendamiento(
            CrearCitaRequest request,
            PacienteSnapshot paciente,
            MedicoSnapshot medico
    ) {

        // ejemplo:
        // validar que el usuario autenticado
        // sea el mismo paciente
    }

    @Override
    public CitaResponse crearCitaAutonoma(CrearCitaRequest request) {
        return super.crearCita(request);
    }
}