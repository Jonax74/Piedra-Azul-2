package com.piedrazul.personas.application.service;

import com.piedrazul.personas.domain.model.Disponibilidad;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.repository.IDisponibilidadRepository;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import com.piedrazul.personas.infrastructure.messaging.publisher.DisponibilidadEventPublisher;
import com.piedrazul.personas.interfaces.rest.dto.request.CrearDisponibilidadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CrearDisponibilidadService {

    private final IMedicoRepository medicoRepository;
    private final IDisponibilidadRepository disponibilidadRepository;
    private final DisponibilidadEventPublisher eventPublisher;

    public void crearDisponibilidad(CrearDisponibilidadRequest request) {
        log.info("Creando disponibilidad para médico: {}", request.getMedicoId());

        // Verificar que el médico existe
        Medico medico = medicoRepository.buscarPorPersonaId(request.getMedicoId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado con ID: " + request.getMedicoId()));

        // Crear disponibilidad
        Disponibilidad disponibilidad = new Disponibilidad();
        disponibilidad.setMedicoId(medico.getPersonaId());  // ← Usar getPersonaId(), no getId()
        disponibilidad.setDiaSemana(request.getDiaSemana());
        disponibilidad.setHoraInicio(request.getHoraInicio());
        disponibilidad.setHoraFin(request.getHoraFin());
        disponibilidad.setIntervaloMinutos(request.getIntervaloMinutos());

        // Guardar
        disponibilidadRepository.guardar(disponibilidad);
        log.info("Disponibilidad creada exitosamente");

        // Publicar evento para citas-service (ASÍNCRONO)
        eventPublisher.publicarDisponibilidadActualizada(
                medico.getPersonaId(),  // ← Usar getPersonaId()
                request.getDiaSemana(),
                request.getHoraInicio(),
                request.getHoraFin(),
                request.getIntervaloMinutos()

        );

        log.info("Evento DisponibilidadActualizada publicado para médico: {}", medico.getPersonaId());
    }
}