package com.piedrazul.citas.application.port.outgoing;

import com.piedrazul.citas.domain.model.DisponibilidadSnapshot;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import java.util.Optional;

public interface DisponibilidadSnapshotRepositoryPort {
    Optional<DisponibilidadSnapshot> findByMedicoId(MedicoId medicoId);
    DisponibilidadSnapshot save(DisponibilidadSnapshot snapshot);
}