package com.piedrazul.citas.application.service;

import com.piedrazul.citas.application.dto.request.CrearCitaRequest;
import com.piedrazul.citas.application.dto.response.CitaResponse;
import com.piedrazul.citas.application.mapper.CitaApplicationMapper;
import com.piedrazul.citas.application.port.incoming.CrearCitaManualUseCase;
import com.piedrazul.citas.application.port.outgoing.*;
import com.piedrazul.citas.application.service.agendamiento.AbstractAgendamientoService;
import com.piedrazul.citas.application.service.singleton.ConfiguracionManager;
import com.piedrazul.citas.domain.factory.CitaManualFactory;
import com.piedrazul.citas.domain.model.*;
import org.springframework.stereotype.Service;

@Service
public class CrearCitaManualService
        extends AbstractAgendamientoService
        implements CrearCitaManualUseCase {

    public CrearCitaManualService(
            CitaRepositoryPort citaRepository,
            PacienteSnapshotRepositoryPort pacienteRepository,
            MedicoSnapshotRepositoryPort medicoRepository,
            DisponibilidadSnapshotRepositoryPort disponibilidadRepository,
            CitaEventPublisherPort eventPublisher,
            CitaApplicationMapper mapper,
            CitaManualFactory citaManualFactory,
            ConfiguracionManager configuracionManager
    ) {
        super(
                citaRepository,
                pacienteRepository,
                medicoRepository,
                disponibilidadRepository,
                eventPublisher,
                mapper,
                citaManualFactory,
                configuracionManager
        );
    }

    @Override
    protected void validarTipoAgendamiento(
            CrearCitaRequest request,
            PacienteSnapshot paciente,
            MedicoSnapshot medico
    ) {

        // validaciones específicas manuales
        // ejemplo:
        // validar permisos del recepcionista
    }

    @Override
    public CitaResponse crearCitaManual(CrearCitaRequest request) {
        return super.crearCita(request);
    }
}