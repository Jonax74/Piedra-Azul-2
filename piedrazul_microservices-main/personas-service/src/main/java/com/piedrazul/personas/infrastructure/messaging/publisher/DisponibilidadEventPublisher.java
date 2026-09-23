// personas-service/src/main/java/com/piedrazul/personas/infrastructure/messaging/publisher/DisponibilidadEventPublisher.java
package com.piedrazul.personas.infrastructure.messaging.publisher;

import com.piedrazul.personas.infrastructure.messaging.event.DisponibilidadActualizadaEvent;
import com.piedrazul.personas.infrastructure.messaging.event.DisponibilidadEliminadaEvent;
import com.piedrazul.personas.infrastructure.messaging.event.DisponibilidadModificadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisponibilidadEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.personas}")
    private String personasExchange;

    public void publicarDisponibilidadActualizada(Long medicoId, String diaSemana, LocalTime horaInicio, LocalTime horaFin, Integer intervaloMinutos) {
        log.info("Publicando evento DisponibilidadActualizada para médico: {}", medicoId);

        // Crear el mapa de horarios semanales
        Map<String, List<Map<String, String>>> horariosSemanales = new HashMap<>();
        List<Map<String, String>> horarios = new ArrayList<>();
        Map<String, String> horario = new HashMap<>();
        horario.put("start", horaInicio.toString());
        horario.put("end", horaFin.toString());
        horarios.add(horario);
        horariosSemanales.put(diaSemana, horarios);

        if (intervaloMinutos == null || intervaloMinutos <= 0) {
            throw new IllegalArgumentException("El intervalo del médico es inválido");
        }

        DisponibilidadActualizadaEvent event = DisponibilidadActualizadaEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DISPONIBILIDAD_ACTUALIZADA")
                .timestamp(LocalDateTime.now())
                .data(DisponibilidadActualizadaEvent.DisponibilidadData.builder()
                        .medicoId(medicoId)
                        .horariosSemanales(horariosSemanales)
                        .bloqueosEspecificos(new ArrayList<>())
                        .intervaloMinutos(intervaloMinutos)
                        .build())
                .build();

        rabbitTemplate.convertAndSend(personasExchange, "disponibilidad.actualizada", event);
        log.info("Evento DisponibilidadActualizada publicado exitosamente");
    }

    public void publicarDisponibilidadEliminada(
            Long medicoId,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        log.info("Publicando evento DisponibilidadEliminada para médico: {}", medicoId);

        DisponibilidadEliminadaEvent event = DisponibilidadEliminadaEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DISPONIBILIDAD_ELIMINADA")
                .timestamp(LocalDateTime.now())
                .data(DisponibilidadEliminadaEvent.DisponibilidadEliminadaData.builder()
                        .medicoId(medicoId)
                        .diaSemana(diaSemana)
                        .horaInicio(horaInicio)
                        .horaFin(horaFin)
                        .build())
                .build();

        rabbitTemplate.convertAndSend(personasExchange, "disponibilidad.eliminada", event);
        log.info("Evento DisponibilidadEliminada publicado exitosamente");
    }

    public void publicarDisponibilidadModificada(
            Long medicoIdAnterior,
            String diaSemanaAnterior,
            LocalTime horaInicioAnterior,
            LocalTime horaFinAnterior,
            Long medicoIdNuevo,
            String diaSemanaNuevo,
            LocalTime horaInicioNuevo,
            LocalTime horaFinNuevo,
            Integer intervaloMinutos
    ) {
        log.info("Publicando evento DisponibilidadModificada para médico: {}", medicoIdAnterior);

        DisponibilidadModificadaEvent event = DisponibilidadModificadaEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DISPONIBILIDAD_MODIFICADA")
                .timestamp(LocalDateTime.now())
                .data(DisponibilidadModificadaEvent.DisponibilidadModificadaData.builder()
                        .medicoId(medicoIdAnterior)
                        .diaSemanaAnterior(diaSemanaAnterior)
                        .horaInicioAnterior(horaInicioAnterior)
                        .horaFinAnterior(horaFinAnterior)
                        .medicoIdNuevo(medicoIdNuevo)
                        .diaSemanaNuevo(diaSemanaNuevo)
                        .horaInicioNuevo(horaInicioNuevo)
                        .horaFinNuevo(horaFinNuevo)
                        .intervaloMinutos(intervaloMinutos)
                        .build())
                .build();

        rabbitTemplate.convertAndSend(personasExchange, "disponibilidad.modificada", event);
        log.info("Evento DisponibilidadModificada publicado exitosamente");
    }
}