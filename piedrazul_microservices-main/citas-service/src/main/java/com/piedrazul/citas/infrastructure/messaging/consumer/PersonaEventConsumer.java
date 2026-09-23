package com.piedrazul.citas.infrastructure.messaging.consumer;

import com.piedrazul.citas.application.port.outgoing.*;
import com.piedrazul.citas.domain.model.*;
import com.piedrazul.citas.domain.util.EspecialidadMedicaParser;
import com.piedrazul.citas.domain.valueobjects.*;
import com.piedrazul.citas.infrastructure.messaging.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaEventConsumer {

    private final PacienteSnapshotRepositoryPort pacienteSnapshotRepository;
    private final MedicoSnapshotRepositoryPort medicoSnapshotRepository;

    @RabbitListener(queues = "${rabbitmq.queue.paciente-creado}")
    public void consumePacienteCreado(PacienteCreadoEvent event) {
        log.info("Recibido evento PacienteCreado: {}", event.getEventId());
        guardarPacienteSnapshot(event.getData(), true);
    }

    @RabbitListener(queues = "${rabbitmq.queue.paciente-actualizado}")
    public void consumePacienteActualizado(PacienteCreadoEvent event) {
        log.info("Recibido evento PacienteActualizado: {}", event.getEventId());
        guardarPacienteSnapshot(event.getData(), false);
    }

    private void guardarPacienteSnapshot(PacienteCreadoEvent.PacienteData data, boolean usarActivoDelEvento) {
        try {
            PacienteId pacienteId = PacienteId.of(data.getPacienteId());
            boolean activo = data.isActivo();

            if (!usarActivoDelEvento) {
                activo = pacienteSnapshotRepository.findById(pacienteId)
                        .map(PacienteSnapshot::isActivo)
                        .orElse(data.isActivo());
            }

            PacienteSnapshot snapshot = new PacienteSnapshot(
                    pacienteId,
                    data.getNombreCompleto(),
                    data.getEmail(),
                    data.getTelefono(),
                    activo
            );

            pacienteSnapshotRepository.save(snapshot);
            log.info("Snapshot de paciente actualizado: {}", data.getPacienteId());

        } catch (Exception e) {
            log.error("Error procesando snapshot de paciente: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.medico-creado}")
    public void consumeMedicoCreado(MedicoCreadoEvent event) {
        log.info("Recibido evento MedicoCreado: {}", event.getEventId());

        try {
            MedicoCreadoEvent.MedicoData data = event.getData();
            guardarMedicoSnapshot(data);

        } catch (Exception e) {
            log.error("Error procesando evento MedicoCreado: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.medico-actualizado}")
    public void consumeMedicoActualizado(MedicoActualizadoEvent event) {
        log.info("Recibido evento MedicoActualizado: {}", event.getEventId());

        try {
            MedicoActualizadoEvent.MedicoActualizadoData data = event.getData();
            guardarMedicoSnapshot(data);

        } catch (Exception e) {
            log.error("Error procesando evento MedicoActualizado: {}", e.getMessage(), e);
        }
    }

    private void guardarMedicoSnapshot(MedicoCreadoEvent.MedicoData data) {
        MedicoSnapshot snapshot = construirSnapshot(
                data.getMedicoId(),
                data.getNombreCompleto(),
                data.getEmail(),
                data.getEspecialidades(),
                data.getEspecialidad(),
                data.getEstado()
        );
        medicoSnapshotRepository.save(snapshot);
        log.info("Snapshot de médico actualizado: {}", data.getMedicoId());
    }

    private void guardarMedicoSnapshot(MedicoActualizadoEvent.MedicoActualizadoData data) {
        MedicoSnapshot snapshot = construirSnapshot(
                data.getMedicoId(),
                data.getNombreCompleto(),
                data.getEmail(),
                data.getEspecialidades(),
                data.getEspecialidad(),
                data.getEstado()
        );
        medicoSnapshotRepository.save(snapshot);
        log.info("Snapshot de médico actualizado: {}", data.getMedicoId());
    }

    private MedicoSnapshot construirSnapshot(Long medicoId,
                                             String nombreCompleto,
                                             String email,
                                             java.util.List<String> especialidadesLista,
                                             String especialidadResumen,
                                             String estado) {
        Set<EspecialidadMedica> especialidades = EspecialidadMedicaParser.resolver(
                especialidadesLista,
                especialidadResumen
        );

        return new MedicoSnapshot(
                MedicoId.of(medicoId),
                nombreCompleto,
                email,
                especialidades,
                EstadoMedico.valueOf(estado)
        );
    }
}
