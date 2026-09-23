package com.piedrazul.notifications.controller;

import com.piedrazul.notifications.dto.NotificacionRequest;
import com.piedrazul.notifications.dto.NotificacionResponse;
import com.piedrazul.notifications.service.InAppNotificationService;
import com.piedrazul.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final InAppNotificationService inAppNotificationService;

    /**
     * ESCENARIO 2: Endpoint REST para notificaciones síncronas
     * citas-service llama a este endpoint después de crear una cita
     */
    @PostMapping("/cita-agendada")
    public ResponseEntity<Void> notificarCitaAgendada(@Valid @RequestBody NotificacionRequest request) {
        log.info("========================================");
        log.info("📨 SOLICITUD REST RECIBIDA (ESCENARIO 2 - SÍNCRONO)");
        log.info("   Cita ID: {}", request.getCitaId());
        log.info("   Destinatario: {}", request.getDestinatario());
        log.info("   Tipo: {}", request.getTipo());
        log.info("========================================");

        notificationService.enviarNotificacionSincrona(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/mias")
    public ResponseEntity<List<NotificacionResponse>> listarMisNotificaciones(
            @RequestParam Long personaId,
            @RequestParam(required = false) Boolean leida) {
        return ResponseEntity.ok(inAppNotificationService.listar(personaId, leida));
    }

    @GetMapping("/mias/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(@RequestParam Long personaId) {
        long count = inAppNotificationService.contarNoLeidas(personaId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id, @RequestParam Long personaId) {
        inAppNotificationService.marcarLeida(id, personaId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas(@RequestParam Long personaId) {
        inAppNotificationService.marcarTodasLeidas(personaId);
        return ResponseEntity.noContent().build();
    }
}