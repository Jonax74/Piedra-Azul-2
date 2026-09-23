package com.piedrazul.citas.infrastructure.messaging.consumer;

import com.piedrazul.citas.application.port.outgoing.DisponibilidadSnapshotRepositoryPort;
import com.piedrazul.citas.application.util.DiaSemanaMapper;
import com.piedrazul.citas.domain.model.DisponibilidadSnapshot;
import com.piedrazul.citas.domain.model.TimeRange;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.infrastructure.messaging.event.DisponibilidadEliminadaEvent;
import com.piedrazul.citas.infrastructure.messaging.event.DisponibilidadModificadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisponibilidadCambioEventConsumer {

    private final DisponibilidadSnapshotRepositoryPort disponibilidadSnapshotRepository;

    @RabbitListener(queues = "${rabbitmq.queue.disponibilidad-eliminada}")
    public void consumeDisponibilidadEliminada(DisponibilidadEliminadaEvent event) {
        log.info("Recibido evento DisponibilidadEliminada: {}", event.getEventId());

        try {
            DisponibilidadEliminadaEvent.DisponibilidadEliminadaData data = event.getData();
            MedicoId medicoId = MedicoId.of(data.getMedicoId());
            DayOfWeek dia = DiaSemanaMapper.toDayOfWeek(data.getDiaSemana());
            TimeRange rango = new TimeRange(data.getHoraInicio(), data.getHoraFin());

            disponibilidadSnapshotRepository.findByMedicoId(medicoId).ifPresent(snapshot -> {
                snapshot.removerHorarioSemanal(dia, rango);
                disponibilidadSnapshotRepository.save(snapshot);
            });

            log.info("Horario eliminado del snapshot para médico: {}", data.getMedicoId());
        } catch (Exception e) {
            log.error("Error procesando evento DisponibilidadEliminada: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.disponibilidad-modificada}")
    public void consumeDisponibilidadModificada(DisponibilidadModificadaEvent event) {
        log.info("Recibido evento DisponibilidadModificada: {}", event.getEventId());

        try {
            DisponibilidadModificadaEvent.DisponibilidadModificadaData data = event.getData();
            MedicoId medicoAnterior = MedicoId.of(data.getMedicoId());
            MedicoId medicoNuevo = MedicoId.of(data.getMedicoIdNuevo());

            DayOfWeek diaAnterior = DiaSemanaMapper.toDayOfWeek(data.getDiaSemanaAnterior());
            DayOfWeek diaNuevo = DiaSemanaMapper.toDayOfWeek(data.getDiaSemanaNuevo());

            TimeRange rangoAnterior = new TimeRange(data.getHoraInicioAnterior(), data.getHoraFinAnterior());
            TimeRange rangoNuevo = new TimeRange(data.getHoraInicioNuevo(), data.getHoraFinNuevo());

            if (medicoAnterior.equals(medicoNuevo) && diaAnterior == diaNuevo) {
                DisponibilidadSnapshot snapshot = disponibilidadSnapshotRepository
                        .findByMedicoId(medicoAnterior)
                        .orElse(new DisponibilidadSnapshot(medicoAnterior, data.getIntervaloMinutos()));

                snapshot.setIntervaloMinutos(data.getIntervaloMinutos());
                snapshot.reemplazarHorarioSemanal(diaAnterior, rangoAnterior, rangoNuevo);
                disponibilidadSnapshotRepository.save(snapshot);
            } else {
                DisponibilidadSnapshot snapshotAnterior = disponibilidadSnapshotRepository
                        .findByMedicoId(medicoAnterior)
                        .orElse(new DisponibilidadSnapshot(medicoAnterior, data.getIntervaloMinutos()));

                snapshotAnterior.removerHorarioSemanal(diaAnterior, rangoAnterior);
                disponibilidadSnapshotRepository.save(snapshotAnterior);

                DisponibilidadSnapshot snapshotNuevo = disponibilidadSnapshotRepository
                        .findByMedicoId(medicoNuevo)
                        .orElse(new DisponibilidadSnapshot(medicoNuevo, data.getIntervaloMinutos()));

                snapshotNuevo.setIntervaloMinutos(data.getIntervaloMinutos());
                snapshotNuevo.agregarHorarioSemanalSinDuplicados(diaNuevo, rangoNuevo);
                disponibilidadSnapshotRepository.save(snapshotNuevo);
            }

            log.info("Horario modificado en snapshot para médico: {}", data.getMedicoId());
        } catch (Exception e) {
            log.error("Error procesando evento DisponibilidadModificada: {}", e.getMessage(), e);
        }
    }
}
