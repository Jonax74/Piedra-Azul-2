package com.piedrazul.citas.application.service;

import com.piedrazul.citas.application.port.incoming.ConsultarSlotsDisponiblesUseCase;
import com.piedrazul.citas.application.port.outgoing.CitaRepositoryPort;
import com.piedrazul.citas.application.port.outgoing.ConfiguracionRepositoryPort;
import com.piedrazul.citas.application.port.outgoing.DisponibilidadSnapshotRepositoryPort;
import com.piedrazul.citas.domain.model.DisponibilidadSnapshot;
import com.piedrazul.citas.domain.model.TimeRange;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.domain.valueobjects.PacienteId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConsultarSlotsDisponiblesService implements ConsultarSlotsDisponiblesUseCase {

    private final DisponibilidadSnapshotRepositoryPort disponibilidadRepo;
    private final CitaRepositoryPort citaRepo;
    private final ConfiguracionRepositoryPort configRepo;

    @Override
    public List<LocalDateTime> consultar(MedicoId medicoId, PacienteId pacienteId) {

        DisponibilidadSnapshot snapshot = disponibilidadRepo.findByMedicoId(medicoId)
                .orElseThrow(() -> new RuntimeException("No hay disponibilidad"));

        var configuracion = configRepo.obtener();
        int semanas = configuracion.getSemanasDisponibles();

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fin = ahora.plusWeeks(semanas);

        Set<LocalDateTime> ocupados = new HashSet<>(
                citaRepo.findFechasOcupadasPorMedico(medicoId, ahora, fin)
        );

        if (pacienteId != null) {
            ocupados.addAll(citaRepo.findFechasOcupadasPorPaciente(pacienteId, ahora, fin));
        }

        List<LocalDateTime> disponibles = new ArrayList<>();

        LocalDate fecha = ahora.toLocalDate();

        while (!fecha.isAfter(fin.toLocalDate())) {

            if (configuracion.esFestivo(fecha)) {
                fecha = fecha.plusDays(1);
                continue;
            }

            DayOfWeek dia = fecha.getDayOfWeek();

            List<TimeRange> rangos = snapshot.getHorariosSemanales().get(dia);

            if (rangos != null) {

                for (TimeRange rango : rangos) {

                    LocalTime hora = rango.getStart();

                    while (hora.isBefore(rango.getEnd())) {

                        LocalDateTime slot = LocalDateTime.of(fecha, hora);

                        if (slot.isAfter(ahora)
                                && !ocupados.contains(slot)
                                && !snapshot.getBloqueosEspecificos().contains(slot)) {

                            disponibles.add(slot);
                        }

                        hora = hora.plusMinutes(snapshot.getIntervaloMinutos());
                    }
                }
            }

            fecha = fecha.plusDays(1);
        }

        return disponibles;
    }
}