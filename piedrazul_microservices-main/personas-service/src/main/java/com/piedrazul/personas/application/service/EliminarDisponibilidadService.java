package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.DisponibilidadNoEncontradaException;
import com.piedrazul.personas.domain.model.Disponibilidad;
import com.piedrazul.personas.domain.repository.IDisponibilidadRepository;
import com.piedrazul.personas.infrastructure.client.CitasServiceClient;
import com.piedrazul.personas.infrastructure.messaging.publisher.DisponibilidadEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EliminarDisponibilidadService {

    private final IDisponibilidadRepository disponibilidadRepository;
    private final CitasServiceClient citasServiceClient;
    private final DisponibilidadEventPublisher eventPublisher;

    public void eliminarDisponibilidad(Long id) {
        log.info("Eliminando disponibilidad id: {}", id);

        Disponibilidad disponibilidad = disponibilidadRepository.buscarPorId(id)
                .orElseThrow(() -> new DisponibilidadNoEncontradaException(
                        "Disponibilidad no encontrada con ID: " + id
                ));

        citasServiceClient.validarEliminacion(
                disponibilidad.getMedicoId(),
                disponibilidad.getDiaSemana(),
                disponibilidad.getHoraInicio(),
                disponibilidad.getHoraFin()
        );

        disponibilidadRepository.eliminar(id);

        eventPublisher.publicarDisponibilidadEliminada(
                disponibilidad.getMedicoId(),
                disponibilidad.getDiaSemana(),
                disponibilidad.getHoraInicio(),
                disponibilidad.getHoraFin()
        );

        log.info("Disponibilidad eliminada exitosamente id: {}", id);
    }
}
