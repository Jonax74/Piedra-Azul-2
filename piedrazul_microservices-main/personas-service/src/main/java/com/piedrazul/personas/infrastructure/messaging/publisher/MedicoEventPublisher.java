package com.piedrazul.personas.infrastructure.messaging.publisher;

import com.piedrazul.personas.infrastructure.messaging.event.MedicoActualizadoEvent;
import com.piedrazul.personas.infrastructure.messaging.event.MedicoCreadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicoEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.personas}")
    private String personasExchange;

    public void publicarMedicoCreado(Long medicoId,
                                     String nombreCompleto,
                                     String email,
                                     List<String> especialidades) {
        log.info("Publicando evento MedicoCreado para médico: {}", medicoId);

        MedicoCreadoEvent event = MedicoCreadoEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MEDICO_CREADO")
                .timestamp(LocalDateTime.now())
                .data(MedicoCreadoEvent.MedicoData.builder()
                        .medicoId(medicoId)
                        .nombreCompleto(nombreCompleto)
                        .email(email)
                        .especialidades(especialidades)
                        .especialidad(resumir(especialidades))
                        .estado("ACTIVO")
                        .build())
                .build();

        rabbitTemplate.convertAndSend(personasExchange, "medico.creado", event);
        log.info("Evento MedicoCreado publicado exitosamente");
    }

    public void publicarMedicoActualizado(Long medicoId,
                                          String nombreCompleto,
                                          String email,
                                          List<String> especialidades,
                                          String estado) {
        log.info("Publicando evento MedicoActualizado para médico: {}", medicoId);

        MedicoActualizadoEvent event = MedicoActualizadoEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MEDICO_ACTUALIZADO")
                .timestamp(LocalDateTime.now())
                .data(MedicoActualizadoEvent.MedicoActualizadoData.builder()
                        .medicoId(medicoId)
                        .nombreCompleto(nombreCompleto)
                        .email(email)
                        .especialidades(especialidades)
                        .especialidad(resumir(especialidades))
                        .estado(estado)
                        .build())
                .build();

        rabbitTemplate.convertAndSend(personasExchange, "medico.actualizado", event);
        log.info("Evento MedicoActualizado publicado exitosamente");
    }

    private String resumir(List<String> especialidades) {
        if (especialidades == null || especialidades.isEmpty()) {
            return "";
        }
        return especialidades.stream().collect(Collectors.joining(", "));
    }
}
