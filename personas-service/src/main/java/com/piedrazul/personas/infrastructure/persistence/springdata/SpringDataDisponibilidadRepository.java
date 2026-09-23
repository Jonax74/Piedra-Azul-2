package com.piedrazul.personas.infrastructure.persistence.springdata;

import com.piedrazul.personas.infrastructure.persistence.entity.DisponibilidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpringDataDisponibilidadRepository extends JpaRepository<DisponibilidadEntity, Long> {

    List<DisponibilidadEntity> findByMedicoId(Long medicoId);

    boolean existsByMedicoIdAndDiaSemana(Long medicoId, String diaSemana);

    boolean existsByMedicoIdAndDiaSemanaAndIdNot(Long medicoId, String diaSemana, Long id);
}