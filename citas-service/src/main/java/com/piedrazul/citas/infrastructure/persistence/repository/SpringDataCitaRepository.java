// infrastructure/persistence/repository/SpringDataCitaRepository.java
package com.piedrazul.citas.infrastructure.persistence.repository;

import com.piedrazul.citas.infrastructure.persistence.entity.CitaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataCitaRepository extends JpaRepository<CitaEntity, UUID> {

    @Query("""
    SELECT COUNT(c) > 0
    FROM CitaEntity c
    WHERE c.medicoId = :medicoId
    AND c.fechaHora = :fechaHora
    AND c.estado IN ('PROGRAMADA', 'CONFIRMADA', 'REAGENDADA')
    """)
    boolean existsCitaActivaByMedicoIdAndFechaHora(
            @Param("medicoId") Long medicoId,
            @Param("fechaHora") LocalDateTime fechaHora
    );

    @Query("""
    SELECT COUNT(c) > 0
    FROM CitaEntity c
    WHERE c.medicoId = :medicoId
    AND c.fechaHora = :fechaHora
    AND c.id <> :citaId
    AND c.estado IN ('PROGRAMADA', 'CONFIRMADA', 'REAGENDADA')
    """)
    boolean existsCitaActivaByMedicoIdAndFechaHoraExcluding(
            @Param("medicoId") Long medicoId,
            @Param("fechaHora") LocalDateTime fechaHora,
            @Param("citaId") UUID citaId
    );

    @Query("""
    SELECT COUNT(c) > 0
    FROM CitaEntity c
    WHERE c.pacienteId = :pacienteId
    AND c.fechaHora = :fechaHora
    AND c.estado IN ('PROGRAMADA', 'CONFIRMADA', 'REAGENDADA')
    """)
    boolean existsCitaActivaByPacienteIdAndFechaHora(
            @Param("pacienteId") Long pacienteId,
            @Param("fechaHora") LocalDateTime fechaHora
    );

    @Query("""
    SELECT COUNT(c) > 0
    FROM CitaEntity c
    WHERE c.pacienteId = :pacienteId
    AND c.fechaHora = :fechaHora
    AND c.id <> :citaId
    AND c.estado IN ('PROGRAMADA', 'CONFIRMADA', 'REAGENDADA')
    """)
    boolean existsCitaActivaByPacienteIdAndFechaHoraExcluding(
            @Param("pacienteId") Long pacienteId,
            @Param("fechaHora") LocalDateTime fechaHora,
            @Param("citaId") UUID citaId
    );

    @Query("""
    SELECT c.fechaHora
    FROM CitaEntity c
    WHERE c.medicoId = :medicoId
    AND c.fechaHora BETWEEN :desde AND :hasta
    AND c.estado IN ('PROGRAMADA', 'CONFIRMADA', 'REAGENDADA')
    """)
    List<LocalDateTime> findFechasOcupadasPorMedico(
            @Param("medicoId") Long medicoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT c.fechaHora
    FROM CitaEntity c
    WHERE c.pacienteId = :pacienteId
    AND c.fechaHora BETWEEN :desde AND :hasta
    AND c.estado IN ('PROGRAMADA', 'CONFIRMADA', 'REAGENDADA')
    """)
    List<LocalDateTime> findFechasOcupadasPorPaciente(
            @Param("pacienteId") Long pacienteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
    List<CitaEntity> findByMedicoIdAndFechaHoraBetween(
            Long medicoId,
            LocalDateTime inicio,
            LocalDateTime fin
    );
    List<CitaEntity> findByMedicoId(Long medicoId);

    List<CitaEntity> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    List<CitaEntity> findByPacienteId(Long pacienteId);

    List<CitaEntity> findByPacienteIdAndFechaHoraBetween(
            Long pacienteId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    @Query("""
    SELECT COUNT(c) > 0
    FROM CitaEntity c
    WHERE c.pacienteId = :pacienteId
    AND c.especialidad = 'GENERAL'
    AND c.estado = 'ATENDIDA'
    """)
    boolean existeConsultaGeneralAtendidaByPacienteId(@Param("pacienteId") Long pacienteId);

    @Query("""
    SELECT COUNT(c) > 0
    FROM CitaEntity c
    WHERE c.pacienteId = :pacienteId
    AND c.estado IN ('PROGRAMADA', 'REAGENDADA')
    """)
    boolean existeCitaProgramadaByPacienteId(@Param("pacienteId") Long pacienteId);
}