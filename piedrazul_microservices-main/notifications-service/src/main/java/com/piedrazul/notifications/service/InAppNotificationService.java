package com.piedrazul.notifications.service;

import com.piedrazul.notifications.domain.model.TipoNotificacion;
import com.piedrazul.notifications.dto.CitaAgendadaDTO;
import com.piedrazul.notifications.dto.CitaCanceladaDTO;
import com.piedrazul.notifications.dto.CitaReagendadaDTO;
import com.piedrazul.notifications.dto.NotificacionResponse;
import com.piedrazul.notifications.infrastructure.persistence.entity.NotificacionEntity;
import com.piedrazul.notifications.infrastructure.persistence.repository.SpringDataNotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SpringDataNotificacionRepository repository;

    @Transactional
    public void registrarCitaAgendada(CitaAgendadaDTO evento) {
        if (evento == null || evento.getData() == null) {
            return;
        }
        CitaAgendadaDTO.CitaData data = evento.getData();
        String fecha = formatear(data.getFechaHora());

        guardar(
                data.getPacienteId(),
                TipoNotificacion.CITA_AGENDADA,
                "Cita programada",
                String.format(
                        "Su cita con el Dr(a). %s fue programada para el %s.",
                        data.getMedicoNombre(),
                        fecha
                ),
                data.getCitaId()
        );

        guardar(
                data.getMedicoId(),
                TipoNotificacion.CITA_AGENDADA,
                "Nueva cita asignada",
                String.format(
                        "Tiene una nueva cita con %s el %s.",
                        data.getPacienteNombre(),
                        fecha
                ),
                data.getCitaId()
        );

        log.info("Notificaciones in-app creadas para cita agendada {}", data.getCitaId());
    }

    @Transactional
    public void registrarCitaCancelada(CitaCanceladaDTO evento) {
        if (evento == null || evento.getData() == null) {
            return;
        }
        CitaCanceladaDTO.CitaCanceladaData data = evento.getData();
        String fecha = formatear(data.getFechaHoraOriginal());
        String motivo = data.getMotivo() == null || data.getMotivo().isBlank()
                ? "Sin motivo indicado"
                : data.getMotivo();

        guardar(
                data.getPacienteId(),
                TipoNotificacion.CITA_CANCELADA,
                "Cita cancelada",
                String.format("Su cita del %s fue cancelada. Motivo: %s.", fecha, motivo),
                data.getCitaId()
        );

        guardar(
                data.getMedicoId(),
                TipoNotificacion.CITA_CANCELADA,
                "Cita cancelada",
                String.format("La cita del %s fue cancelada. Motivo: %s.", fecha, motivo),
                data.getCitaId()
        );

        log.info("Notificaciones in-app creadas para cita cancelada {}", data.getCitaId());
    }

    @Transactional
    public void registrarCitaReagendada(CitaReagendadaDTO evento) {
        if (evento == null || evento.getData() == null) {
            return;
        }
        CitaReagendadaDTO.CitaReagendadaData data = evento.getData();
        String fechaAnterior = formatear(data.getFechaHoraOriginal());
        String fechaNueva = formatear(data.getNuevaFechaHora());

        guardar(
                data.getPacienteId(),
                TipoNotificacion.CITA_REAGENDADA,
                "Cita reagendada",
                String.format(
                        "Su cita fue reagendada del %s al %s.",
                        fechaAnterior,
                        fechaNueva
                ),
                data.getCitaId()
        );

        guardar(
                data.getMedicoId(),
                TipoNotificacion.CITA_REAGENDADA,
                "Cita reagendada",
                String.format(
                        "Una cita fue reagendada del %s al %s.",
                        fechaAnterior,
                        fechaNueva
                ),
                data.getCitaId()
        );

        log.info("Notificaciones in-app creadas para cita reagendada {}", data.getCitaId());
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(Long personaId, Boolean leida) {
        List<NotificacionEntity> entities = leida == null
                ? repository.findByPersonaIdOrderByFechaCreacionDesc(personaId)
                : repository.findByPersonaIdAndLeidaOrderByFechaCreacionDesc(personaId, leida);
        return entities.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long personaId) {
        return repository.countByPersonaIdAndLeida(personaId, false);
    }

    @Transactional
    public void marcarLeida(String id, Long personaId) {
        NotificacionEntity entity = repository.findByIdAndPersonaId(id, personaId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));
        entity.setLeida(true);
        repository.save(entity);
    }

    @Transactional
    public void marcarTodasLeidas(Long personaId) {
        List<NotificacionEntity> pendientes = repository.findByPersonaIdAndLeida(personaId, false);
        pendientes.forEach(n -> n.setLeida(true));
        repository.saveAll(pendientes);
    }

    private void guardar(Long personaId,
                         TipoNotificacion tipo,
                         String titulo,
                         String mensaje,
                         String citaId) {
        if (personaId == null) {
            log.warn("Se omitió notificación in-app: personaId nulo para cita {}", citaId);
            return;
        }
        repository.save(NotificacionEntity.crear(personaId, tipo, titulo, mensaje, citaId));
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "fecha por confirmar" : fecha.format(FECHA_FORMATO);
    }

    private NotificacionResponse toResponse(NotificacionEntity entity) {
        return NotificacionResponse.builder()
                .id(entity.getId())
                .personaId(entity.getPersonaId())
                .tipo(entity.getTipo())
                .titulo(entity.getTitulo())
                .mensaje(entity.getMensaje())
                .citaId(entity.getCitaId())
                .leida(entity.isLeida())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
