package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.DisponibilidadNoEncontradaException;
import com.piedrazul.personas.application.exception.ReglaDeNegocioException;
import com.piedrazul.personas.domain.model.Disponibilidad;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.repository.IDisponibilidadRepository;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import com.piedrazul.personas.infrastructure.client.CitasServiceClient;
import com.piedrazul.personas.infrastructure.messaging.publisher.DisponibilidadEventPublisher;
import com.piedrazul.personas.interfaces.rest.dto.request.ActualizarDisponibilidadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActualizarDisponibilidadService {

    private final IMedicoRepository medicoRepository;
    private final IDisponibilidadRepository disponibilidadRepository;
    private final CitasServiceClient citasServiceClient;
    private final DisponibilidadEventPublisher eventPublisher;

    public void actualizarDisponibilidad(Long id, ActualizarDisponibilidadRequest request) {
        log.info("Actualizando disponibilidad id: {}", id);

        Disponibilidad actual = disponibilidadRepository.buscarPorId(id)
                .orElseThrow(() -> new DisponibilidadNoEncontradaException(
                        "Disponibilidad no encontrada con ID: " + id
                ));

        Medico medico = medicoRepository.buscarPorPersonaId(request.getMedicoId())
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "Médico no encontrado con ID: " + request.getMedicoId()
                ));

        citasServiceClient.validarModificacion(
                actual.getMedicoId(),
                actual.getDiaSemana(),
                actual.getHoraInicio(),
                actual.getHoraFin(),
                medico.getPersonaId(),
                request.getDiaSemana(),
                request.getHoraInicio(),
                request.getHoraFin()
        );

        Long medicoIdAnterior = actual.getMedicoId();
        String diaAnterior = actual.getDiaSemana();
        var horaInicioAnterior = actual.getHoraInicio();
        var horaFinAnterior = actual.getHoraFin();

        actual.setMedicoId(medico.getPersonaId());
        actual.setDiaSemana(request.getDiaSemana());
        actual.setHoraInicio(request.getHoraInicio());
        actual.setHoraFin(request.getHoraFin());
        actual.setIntervaloMinutos(request.getIntervaloMinutos());

        disponibilidadRepository.guardar(actual);

        eventPublisher.publicarDisponibilidadModificada(
                medicoIdAnterior,
                diaAnterior,
                horaInicioAnterior,
                horaFinAnterior,
                medico.getPersonaId(),
                request.getDiaSemana(),
                request.getHoraInicio(),
                request.getHoraFin(),
                request.getIntervaloMinutos()
        );

        log.info("Disponibilidad actualizada exitosamente id: {}", id);
    }
}
