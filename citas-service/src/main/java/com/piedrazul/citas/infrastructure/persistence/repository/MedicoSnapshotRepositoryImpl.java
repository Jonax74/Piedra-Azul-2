package com.piedrazul.citas.infrastructure.persistence.repository;

import com.piedrazul.citas.application.port.outgoing.MedicoSnapshotRepositoryPort;
import com.piedrazul.citas.domain.model.EspecialidadMedica;
import com.piedrazul.citas.domain.model.EstadoMedico;
import com.piedrazul.citas.domain.model.MedicoSnapshot;
import com.piedrazul.citas.domain.util.EspecialidadMedicaParser;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.infrastructure.persistence.entity.MedicoSnapshotEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class MedicoSnapshotRepositoryImpl implements MedicoSnapshotRepositoryPort {

    private final SpringDataMedicoSnapshotRepository repository;

    @Override
    public Optional<MedicoSnapshot> findById(MedicoId id) {
        return repository.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public MedicoSnapshot save(MedicoSnapshot snapshot) {
        Long medicoId = snapshot.getId().value();

        MedicoSnapshotEntity entity = repository.findById(medicoId)
                .orElseGet(() -> MedicoSnapshotEntity.builder()
                        .id(medicoId)
                        .especialidades(new LinkedHashSet<>())
                        .build());

        entity.setNombreCompleto(snapshot.getNombreCompleto());
        entity.setEmail(snapshot.getEmail());
        entity.setEspecialidad(snapshot.getEspecialidadResumen());
        entity.setEstado(snapshot.getEstado().name());

        entity.getEspecialidades().clear();
        entity.getEspecialidades().addAll(snapshot.getEspecialidades());

        MedicoSnapshotEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    private MedicoSnapshot toDomain(MedicoSnapshotEntity entity) {
        Set<EspecialidadMedica> especialidades = entity.getEspecialidades();
        if (especialidades == null || especialidades.isEmpty()) {
            especialidades = EspecialidadMedicaParser.parsearResumen(entity.getEspecialidad());
        }

        return new MedicoSnapshot(
                MedicoId.of(entity.getId()),
                entity.getNombreCompleto(),
                entity.getEmail(),
                especialidades,
                EstadoMedico.valueOf(entity.getEstado())
        );
    }
}
