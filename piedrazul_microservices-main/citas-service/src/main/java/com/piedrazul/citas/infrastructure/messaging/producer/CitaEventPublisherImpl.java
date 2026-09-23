package com.piedrazul.citas.infrastructure.messaging.producer;

import com.piedrazul.citas.application.port.outgoing.CitaEventPublisherPort;
import com.piedrazul.citas.domain.model.Cita;
import com.piedrazul.citas.domain.model.MedicoSnapshot;
import com.piedrazul.citas.domain.model.PacienteSnapshot;
import com.piedrazul.citas.infrastructure.messaging.event.CitaAgendadaEvent;
import com.piedrazul.citas.infrastructure.messaging.event.CitaCanceladaEvent;
import com.piedrazul.citas.infrastructure.messaging.event.CitaReagendadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CitaEventPublisherImpl implements CitaEventPublisherPort {

    private static final String VALOR_DESCONOCIDO = "N/D";

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.citas}")
    private String citasExchange;

    @Value("${rabbitmq.routing.cita-agendada}")
    private String routingKeyAgendada;

    @Value("${rabbitmq.routing.cita-cancelada}")
    private String routingKeyCancelada;

    @Value("${rabbitmq.routing.cita-reagendada}")
    private String routingKeyReagendada;

    @Override
    public void publicarCitaAgendada(Cita cita, PacienteSnapshot paciente, MedicoSnapshot medico) {
        log.info("Publicando evento CITA_AGENDADA para cita: {}", cita.getId());

        CitaAgendadaEvent event = CitaAgendadaEvent.create(
                cita.getId().toString(),
                cita.getPacienteId().value(),
                nombrePaciente(paciente),
                emailPaciente(paciente),
                cita.getMedicoId().value(),
                nombreMedico(medico),
                emailMedico(medico),
                cita.getFechaHora(),
                cita.getEstado().getDescripcion()
        );

        publicarEvento("CITA_AGENDADA", () ->
                rabbitTemplate.convertAndSend(citasExchange, routingKeyAgendada, event)
        );
    }

    @Override
    public void publicarCitaCancelada(Cita cita, PacienteSnapshot paciente, MedicoSnapshot medico) {
        log.info("Publicando evento CITA_CANCELADA para cita: {}", cita.getId());

        CitaCanceladaEvent event = CitaCanceladaEvent.create(
                cita.getId().toString(),
                cita.getPacienteId().value(),
                emailPaciente(paciente),
                cita.getMedicoId().value(),
                emailMedico(medico),
                cita.getFechaHora(),
                cita.getMotivoCancelacion()
        );

        publicarEvento("CITA_CANCELADA", () ->
                rabbitTemplate.convertAndSend(citasExchange, routingKeyCancelada, event)
        );
    }

    @Override
    public void publicarCitaReagendada(Cita cita,
                                       LocalDateTime fechaHoraOriginal,
                                       PacienteSnapshot paciente,
                                       MedicoSnapshot medico) {
        log.info("Publicando evento CITA_REAGENDADA para cita: {}", cita.getId());

        CitaReagendadaEvent event = CitaReagendadaEvent.create(
                cita.getId().toString(),
                cita.getPacienteId().value(),
                emailPaciente(paciente),
                cita.getMedicoId().value(),
                emailMedico(medico),
                fechaHoraOriginal,
                cita.getFechaHora()
        );

        publicarEvento("CITA_REAGENDADA", () ->
                rabbitTemplate.convertAndSend(citasExchange, routingKeyReagendada, event)
        );
    }

    private void publicarEvento(String tipoEvento, Runnable accion) {
        try {
            accion.run();
            log.info("Evento {} publicado exitosamente", tipoEvento);
        } catch (Exception e) {
            log.warn(
                    "No se pudo publicar el evento {}. La operación de la cita se completó, "
                            + "pero no se envió notificación: {}",
                    tipoEvento,
                    e.getMessage()
            );
        }
    }

    private String nombrePaciente(PacienteSnapshot paciente) {
        return paciente != null && paciente.getNombreCompleto() != null
                ? paciente.getNombreCompleto()
                : VALOR_DESCONOCIDO;
    }

    private String emailPaciente(PacienteSnapshot paciente) {
        return paciente != null && paciente.getEmail() != null
                ? paciente.getEmail()
                : VALOR_DESCONOCIDO;
    }

    private String nombreMedico(MedicoSnapshot medico) {
        return medico != null && medico.getNombreCompleto() != null
                ? medico.getNombreCompleto()
                : VALOR_DESCONOCIDO;
    }

    private String emailMedico(MedicoSnapshot medico) {
        return medico != null && medico.getEmail() != null
                ? medico.getEmail()
                : VALOR_DESCONOCIDO;
    }
}
