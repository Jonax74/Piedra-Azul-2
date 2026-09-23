package com.piedrazul.personas.infrastructure.messaging.publisher;

import com.piedrazul.personas.infrastructure.messaging.event.PacienteCreadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PacienteEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.personas}")
    private String personasExchange;

    public void publicarPacienteCreado(Long pacienteId, String nombreCompleto, String email, String telefono) {
        log.info("Publicando evento PacienteCreado para paciente: {}", pacienteId);

        PacienteCreadoEvent event = PacienteCreadoEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PACIENTE_CREADO")
                .timestamp(LocalDateTime.now())
                .data(PacienteCreadoEvent.PacienteData.builder()
                        .pacienteId(pacienteId)
                        .nombreCompleto(nombreCompleto)
                        .email(email)
                        .telefono(telefono)
                        .activo(true)
                        .build())
                .build();

        rabbitTemplate.convertAndSend(personasExchange, "paciente.creado", event);
        log.info("Evento PacienteCreado publicado exitosamente");
    }

    public void publicarPacienteActualizado(Long pacienteId,
                                            String nombreCompleto,
                                            String email,
                                            String telefono) {
        log.info("Publicando evento PacienteActualizado para paciente: {}", pacienteId);

        PacienteCreadoEvent event = PacienteCreadoEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PACIENTE_ACTUALIZADO")
                .timestamp(LocalDateTime.now())
                .data(PacienteCreadoEvent.PacienteData.builder()
                        .pacienteId(pacienteId)
                        .nombreCompleto(nombreCompleto)
                        .email(email)
                        .telefono(telefono)
                        .activo(true)
                        .build())
                .build();

        rabbitTemplate.convertAndSend(personasExchange, "paciente.actualizado", event);
        log.info("Evento PacienteActualizado publicado exitosamente");
    }
}