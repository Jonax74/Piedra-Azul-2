package com.piedrazul.citas.infrastructure.messaging.consumer;

import com.piedrazul.citas.application.port.outgoing.DisponibilidadSnapshotRepositoryPort;
import com.piedrazul.citas.domain.model.DisponibilidadSnapshot;
import com.piedrazul.citas.domain.model.TimeRange;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.infrastructure.messaging.event.DisponibilidadActualizadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisponibilidadEventConsumer {

    private final DisponibilidadSnapshotRepositoryPort disponibilidadSnapshotRepository;

    // Mapa de conversión de español a inglés
    private static final Map<String, String> DIAS_ESPAÑOL_INGLES = new HashMap<>();

    static {
        DIAS_ESPAÑOL_INGLES.put("LUNES", "MONDAY");
        DIAS_ESPAÑOL_INGLES.put("MARTES", "TUESDAY");
        DIAS_ESPAÑOL_INGLES.put("MIERCOLES", "WEDNESDAY");
        DIAS_ESPAÑOL_INGLES.put("JUEVES", "THURSDAY");
        DIAS_ESPAÑOL_INGLES.put("VIERNES", "FRIDAY");
        DIAS_ESPAÑOL_INGLES.put("SABADO", "SATURDAY");
        DIAS_ESPAÑOL_INGLES.put("DOMINGO", "SUNDAY");
        // Con tilde
        DIAS_ESPAÑOL_INGLES.put("MIÉRCOLES", "WEDNESDAY");
    }

    @RabbitListener(queues = "${rabbitmq.queue.disponibilidad-actualizada}")
    public void consumeDisponibilidadActualizada(DisponibilidadActualizadaEvent event) {
        log.info("Recibido evento DisponibilidadActualizada: {}", event.getEventId());

        try {
            DisponibilidadActualizadaEvent.DisponibilidadData data = event.getData();
            MedicoId medicoId = MedicoId.of(data.getMedicoId());
            Integer intervalo = data.getIntervaloMinutos();

            if (intervalo == null || intervalo <= 0) {
                log.error("Intervalo inválido para médico {}. Evento ignorado.", data.getMedicoId());
                return;
            }

            DisponibilidadSnapshot snapshot = disponibilidadSnapshotRepository
                    .findByMedicoId(medicoId)
                    .orElse(new DisponibilidadSnapshot(medicoId, intervalo));

            // actualizar intervalo siempre
            snapshot.setIntervaloMinutos(intervalo);

            // Procesar horarios semanales
            if (data.getHorariosSemanales() != null) {
                data.getHorariosSemanales().forEach((diaEspanol, rangos) -> {

                    String diaIngles = DIAS_ESPAÑOL_INGLES.getOrDefault(diaEspanol.toUpperCase(), diaEspanol.toUpperCase());
                    DayOfWeek dia = DayOfWeek.valueOf(diaIngles);

                    rangos.forEach(r -> {
                        TimeRange nuevo = new TimeRange(
                                LocalTime.parse(r.getStart()),
                                LocalTime.parse(r.getEnd())
                        );

                        snapshot.agregarHorarioSemanalSinDuplicados(dia, nuevo);
                    });
                });
            }

            // Procesar bloqueos específicos
            if (data.getBloqueosEspecificos() != null) {
                data.getBloqueosEspecificos().forEach(snapshot::agregarBloqueo);
            }

            disponibilidadSnapshotRepository.save(snapshot);
            log.info("Snapshot de disponibilidad actualizado para médico: {}", data.getMedicoId());

        } catch (Exception e) {
            log.error("Error procesando evento DisponibilidadActualizada: {}", e.getMessage(), e);
        }
    }
}