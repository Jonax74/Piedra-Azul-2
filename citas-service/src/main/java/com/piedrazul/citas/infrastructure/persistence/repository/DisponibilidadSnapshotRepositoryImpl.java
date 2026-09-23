// infrastructure/persistence/repository/DisponibilidadSnapshotRepositoryImpl.java
package com.piedrazul.citas.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.citas.application.port.outgoing.DisponibilidadSnapshotRepositoryPort;
import com.piedrazul.citas.domain.model.DisponibilidadSnapshot;
import com.piedrazul.citas.domain.model.TimeRange;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.infrastructure.persistence.entity.DisponibilidadSnapshotEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DisponibilidadSnapshotRepositoryImpl implements DisponibilidadSnapshotRepositoryPort {

    private final SpringDataDisponibilidadSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    // No necesitas constructor manual, @RequiredArgsConstructor lo crea automáticamente
    // Spring inyectará tanto repository como objectMapper

    @Override
    public Optional<DisponibilidadSnapshot> findByMedicoId(MedicoId medicoId) {
        return repository.findById(medicoId.value())
                .map(this::toDomain);
    }

    @Override
    public DisponibilidadSnapshot save(DisponibilidadSnapshot snapshot) {

        // Buscar si ya existe en BD
        Optional<DisponibilidadSnapshotEntity> existingOpt =
                repository.findById(snapshot.getMedicoId().value());

        DisponibilidadSnapshotEntity entity;

        if (existingOpt.isPresent()) {
            // UPDATE (merge)
            entity = existingOpt.get();

            try {
                // Convertir horarios
                Map<String, List<Map<String, String>>> horariosMap = new HashMap<>();
                snapshot.getHorariosSemanales().forEach((dia, rangos) -> {
                    List<Map<String, String>> lista = rangos.stream()
                            .map(r -> {
                                Map<String, String> m = new HashMap<>();
                                m.put("start", r.getStart().toString());
                                m.put("end", r.getEnd().toString());
                                return m;
                            })
                            .toList();
                    horariosMap.put(dia.name(), lista);
                });

                entity.setHorariosSemanales(objectMapper.writeValueAsString(horariosMap));

                // Convertir bloqueos
                entity.setBloqueosEspecificos(
                        objectMapper.writeValueAsString(snapshot.getBloqueosEspecificos())
                );

            } catch (JsonProcessingException e) {
                log.error("Error convirtiendo snapshot a JSON", e);
            }

            entity.setIntervaloMinutos(snapshot.getIntervaloMinutos());

        } else {
            // INSERT
            entity = toEntity(snapshot);
        }

        DisponibilidadSnapshotEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private DisponibilidadSnapshotEntity toEntity(DisponibilidadSnapshot snapshot) {
        DisponibilidadSnapshotEntity entity = new DisponibilidadSnapshotEntity();
        entity.setMedicoId(snapshot.getMedicoId().value());
        entity.setIntervaloMinutos(snapshot.getIntervaloMinutos());

        // Convertir horarios semanales a JSON string
        try {
            Map<String, List<Map<String, String>>> horariosMap = new HashMap<>();
            snapshot.getHorariosSemanales().forEach((dia, rangos) -> {
                List<Map<String, String>> rangosList = rangos.stream()
                        .map(rango -> {
                            Map<String, String> rangoMap = new HashMap<>();
                            rangoMap.put("start", rango.getStart().toString());
                            rangoMap.put("end", rango.getEnd().toString());
                            return rangoMap;
                        })
                        .collect(Collectors.toList());
                horariosMap.put(dia.name(), rangosList);
            });
            entity.setHorariosSemanales(objectMapper.writeValueAsString(horariosMap));

            // Convertir bloqueos a JSON string
            entity.setBloqueosEspecificos(objectMapper.writeValueAsString(snapshot.getBloqueosEspecificos()));

        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            entity.setHorariosSemanales("{}");
            entity.setBloqueosEspecificos("[]");
        }

        return entity;
    }

    private DisponibilidadSnapshot toDomain(DisponibilidadSnapshotEntity entity) {
        DisponibilidadSnapshot snapshot = new DisponibilidadSnapshot(MedicoId.of(entity.getMedicoId()), entity.getIntervaloMinutos());

        // Restaurar horarios semanales desde JSON
        try {
            if (entity.getHorariosSemanales() != null && !entity.getHorariosSemanales().isEmpty()) {
                Map<String, List<Map<String, String>>> horariosMap = objectMapper.readValue(
                        entity.getHorariosSemanales(),
                        new TypeReference<Map<String, List<Map<String, String>>>>() {}
                );

                horariosMap.forEach((diaStr, rangosList) -> {
                    DayOfWeek dia = DayOfWeek.valueOf(diaStr);
                    rangosList.forEach(rangoMap -> {
                        LocalTime start = LocalTime.parse(rangoMap.get("start"));
                        LocalTime end = LocalTime.parse(rangoMap.get("end"));
                        snapshot.agregarHorarioSemanal(dia, new TimeRange(start, end));
                    });
                });
            }

            // Restaurar bloqueos desde JSON
            if (entity.getBloqueosEspecificos() != null && !entity.getBloqueosEspecificos().isEmpty()) {
                Set<LocalDateTime> bloqueos = objectMapper.readValue(
                        entity.getBloqueosEspecificos(),
                        new TypeReference<Set<LocalDateTime>>() {}
                );
                bloqueos.forEach(snapshot::agregarBloqueo);
            }

        } catch (JsonProcessingException e) {
            log.error("Error converting from JSON", e);
        }

        return snapshot;
    }
}