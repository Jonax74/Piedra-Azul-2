package com.piedrazul.notifications.consumer;

import com.piedrazul.notifications.dto.CitaAgendadaDTO;
import com.piedrazul.notifications.dto.CitaCanceladaDTO;
import com.piedrazul.notifications.dto.CitaReagendadaDTO;
import com.piedrazul.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CitaEventConsumer {

    private final NotificationService notificationService;

    @Value("${rabbitmq.queue.cita-agendada}")
    private String citaAgendadaQueue;

    @Value("${rabbitmq.queue.cita-cancelada}")
    private String citaCanceladaQueue;

    @Value("${rabbitmq.queue.cita-reagendada}")
    private String citaReagendadaQueue;

    /**
     * ESCENARIO 1: Consumir eventos de citas agendadas desde RabbitMQ
     * Esto es COMPLETAMENTE ASÍNCRONO
     */
    @RabbitListener(queues = "${rabbitmq.queue.cita-agendada}")
    public void consumirCitaAgendada(CitaAgendadaDTO citaAgendada) {
        log.info("========================================");
        log.info("📨 MENSAJE RECIBIDO DESDE RABBITMQ");
        log.info("   Queue: {}", citaAgendadaQueue);
        log.info("   Event ID: {}", citaAgendada.getEventId());
        log.info("   Event Type: {}", citaAgendada.getEventType());
        log.info("   Timestamp: {}", citaAgendada.getTimestamp());
        log.info("========================================");

        notificationService.enviarNotificacionCitaAgendada(citaAgendada);
    }

    /**
     * Consumir eventos de citas canceladas
     */
    @RabbitListener(queues = "${rabbitmq.queue.cita-cancelada}")
    public void consumirCitaCancelada(CitaCanceladaDTO citaCancelada) {
        log.info("========================================");
        log.info("📨 MENSAJE RECIBIDO DESDE RABBITMQ (CANCELACIÓN)");
        log.info("   Event ID: {}", citaCancelada.getEventId());
        log.info("========================================");

        notificationService.enviarNotificacionCitaCancelada(citaCancelada);
    }

    @RabbitListener(queues = "${rabbitmq.queue.cita-reagendada}")
    public void consumirCitaReagendada(CitaReagendadaDTO citaReagendada) {
        log.info("========================================");
        log.info("📨 MENSAJE RECIBIDO DESDE RABBITMQ (REAGENDAMIENTO)");
        log.info("   Queue: {}", citaReagendadaQueue);
        log.info("   Event ID: {}", citaReagendada.getEventId());
        log.info("========================================");

        notificationService.enviarNotificacionCitaReagendada(citaReagendada);
    }
}