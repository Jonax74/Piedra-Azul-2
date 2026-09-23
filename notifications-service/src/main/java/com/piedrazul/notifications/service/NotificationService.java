package com.piedrazul.notifications.service;

import com.piedrazul.notifications.dto.CitaAgendadaDTO;
import com.piedrazul.notifications.dto.CitaCanceladaDTO;
import com.piedrazul.notifications.dto.NotificacionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final InAppNotificationService inAppNotificationService;

    /**
     * ESCENARIO 1: Notificación asíncrona (vía RabbitMQ)
     */
    public void enviarNotificacionCitaAgendada(CitaAgendadaDTO citaAgendada) {
        log.info("========================================");
        log.info("📨 ESCENARIO 1: NOTIFICACIÓN ASÍNCRONA (RabbitMQ)");
        log.info("========================================");

        // Notificación para el PACIENTE
        String mensajePaciente = String.format(
                "Estimado(a) %s,\n\n" +
                        "Su cita médica ha sido PROGRAMADA exitosamente.\n\n" +
                        "📋 Detalles de la cita:\n" +
                        "   • Cita ID: %s\n" +
                        "   • Médico: %s\n" +
                        "   • Fecha y hora: %s\n" +
                        "   • Estado: %s\n\n" +
                        "Por favor, llegue 15 minutos antes de su cita.\n\n" +
                        "¡Gracias por confiar en Piedrazul!",
                citaAgendada.getData().getPacienteNombre(),
                citaAgendada.getData().getCitaId(),
                citaAgendada.getData().getMedicoNombre(),
                citaAgendada.getData().getFechaHora(),
                citaAgendada.getData().getEstado()
        );

        log.info("📧 NOTIFICACIÓN AL PACIENTE:");
        log.info("   De: notificaciones@piedrazul.com");
        log.info("   Para: {} <{}>",
                citaAgendada.getData().getPacienteNombre(),
                citaAgendada.getData().getPacienteEmail());
        log.info("   Asunto: Confirmación de cita médica - ID: {}",
                citaAgendada.getData().getCitaId());
        log.info("   Mensaje: \n{}", mensajePaciente);

        // Notificación para el MÉDICO
        String mensajeMedico = String.format(
                "Estimado(a) Dr(a). %s,\n\n" +
                        "Tiene una NUEVA cita programada.\n\n" +
                        "📋 Detalles de la cita:\n" +
                        "   • Cita ID: %s\n" +
                        "   • Paciente: %s\n" +
                        "   • Fecha y hora: %s\n" +
                        "   • Estado: %s\n\n" +
                        "Por favor, revise su agenda.",
                citaAgendada.getData().getMedicoNombre(),
                citaAgendada.getData().getCitaId(),
                citaAgendada.getData().getPacienteNombre(),
                citaAgendada.getData().getFechaHora(),
                citaAgendada.getData().getEstado()
        );

        log.info("📧 NOTIFICACIÓN AL MÉDICO:");
        log.info("   De: notificaciones@piedrazul.com");
        log.info("   Para: {} <{}>",
                citaAgendada.getData().getMedicoNombre(),
                citaAgendada.getData().getMedicoEmail());
        log.info("   Asunto: Nueva cita asignada - ID: {}",
                citaAgendada.getData().getCitaId());
        log.info("   Mensaje: \n{}", mensajeMedico);

        inAppNotificationService.registrarCitaAgendada(citaAgendada);

        log.info("✅ NOTIFICACIONES ENVIADAS EXITOSAMENTE (ASÍNCRONO - ESCENARIO 1)");
        log.info("========================================");
    }

    /**
     * ESCENARIO 2: Notificación síncrona (vía REST)
     */
    public void enviarNotificacionSincrona(NotificacionRequest request) {
        log.info("========================================");
        log.info("📨 ESCENARIO 2: NOTIFICACIÓN SÍNCRONA (REST)");
        log.info("========================================");

        String tipoDestinatario = "PACIENTE".equals(request.getTipo()) ? "paciente" : "médico";

        log.info("📧 NOTIFICACIÓN:");
        log.info("   De: notificaciones@piedrazul.com");
        log.info("   Para: {} - {}", tipoDestinatario, request.getDestinatario());
        log.info("   Asunto: {}", request.getAsunto());
        log.info("   Cita ID: {}", request.getCitaId());
        log.info("   Mensaje: \n{}", request.getMensaje());

        log.info("✅ NOTIFICACIÓN ENVIADA EXITOSAMENTE (SÍNCRONO - ESCENARIO 2)");
        log.info("========================================");
    }

    /**
     * Notificación de cancelación de cita
     */
    public void enviarNotificacionCitaCancelada(CitaCanceladaDTO citaCancelada) {
        log.info("========================================");
        log.info("📨 NOTIFICACIÓN DE CANCELACIÓN");
        log.info("========================================");

        String mensaje = String.format(
                "Estimado(a),\n\n" +
                        "La cita médica ha sido CANCELADA.\n\n" +
                        "📋 Detalles:\n" +
                        "   • Cita ID: %s\n" +
                        "   • Motivo: %s\n" +
                        "   • Fecha de cancelación: %s\n\n" +
                        "Para reagendar, por favor comuníquese al 123-456-7890.",
                citaCancelada.getData().getCitaId(),
                citaCancelada.getData().getMotivo(),
                citaCancelada.getData().getFechaCancelacion()
        );

        log.info("📧 NOTIFICACIÓN ENVIADA A PACIENTE Y MÉDICO:");
        log.info("   Asunto: Cancelación de cita médica");
        log.info("   Mensaje: \n{}", mensaje);
        log.info("========================================");

        inAppNotificationService.registrarCitaCancelada(citaCancelada);
    }

    public void enviarNotificacionCitaReagendada(com.piedrazul.notifications.dto.CitaReagendadaDTO citaReagendada) {
        log.info("========================================");
        log.info("📨 NOTIFICACIÓN DE REAGENDAMIENTO");
        log.info("========================================");

        if (citaReagendada.getData() != null) {
            log.info("   Cita ID: {}", citaReagendada.getData().getCitaId());
            log.info("   Nueva fecha: {}", citaReagendada.getData().getNuevaFechaHora());
        }
        log.info("========================================");

        inAppNotificationService.registrarCitaReagendada(citaReagendada);
    }
}