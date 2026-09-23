package com.piedrazul.citas.infrastructure.persistence.repository;

import com.piedrazul.citas.application.port.outgoing.CitaRepositoryPort;
import com.piedrazul.citas.domain.model.Cita;
import com.piedrazul.citas.domain.valueobjects.CitaId;
import com.piedrazul.citas.domain.valueobjects.MedicoId;
import com.piedrazul.citas.domain.valueobjects.PacienteId;
import com.piedrazul.citas.infrastructure.persistence.entity.CitaEntity;
import com.piedrazul.citas.infrastructure.persistence.mapper.CitaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CitaRepositoryImpl implements CitaRepositoryPort {

    private final SpringDataCitaRepository springDataCitaRepository;
    private final CitaPersistenceMapper mapper;

    @Override
    public Cita save(Cita cita) {
        CitaEntity entity = mapper.toEntity(cita);
        CitaEntity savedEntity = springDataCitaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Cita> findById(CitaId id) {
        return springDataCitaRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsCitaActivaByMedicoIdAndFechaHora(MedicoId medicoId, LocalDateTime fechaHora) {
        return springDataCitaRepository.existsCitaActivaByMedicoIdAndFechaHora(
                medicoId.value(), fechaHora);
    }

    @Override
    public boolean existsCitaActivaByMedicoIdAndFechaHoraExcluding(
            MedicoId medicoId,
            LocalDateTime fechaHora,
            CitaId citaId) {
        return springDataCitaRepository.existsCitaActivaByMedicoIdAndFechaHoraExcluding(
                medicoId.value(), fechaHora, toUuid(citaId));
    }

    @Override
    public boolean existsCitaActivaByPacienteIdAndFechaHora(PacienteId pacienteId, LocalDateTime fechaHora) {
        return springDataCitaRepository.existsCitaActivaByPacienteIdAndFechaHora(
                pacienteId.value(), fechaHora);
    }

    @Override
    public boolean existsCitaActivaByPacienteIdAndFechaHoraExcluding(
            PacienteId pacienteId,
            LocalDateTime fechaHora,
            CitaId citaId) {
        return springDataCitaRepository.existsCitaActivaByPacienteIdAndFechaHoraExcluding(
                pacienteId.value(), fechaHora, toUuid(citaId));
    }

    @Override
    public List<LocalDateTime> findFechasOcupadasPorMedico(
            MedicoId medicoId,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return springDataCitaRepository.findFechasOcupadasPorMedico(
                medicoId.value(),
                desde,
                hasta
        );
    }

    @Override
    public List<LocalDateTime> findFechasOcupadasPorPaciente(
            PacienteId pacienteId,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return springDataCitaRepository.findFechasOcupadasPorPaciente(
                pacienteId.value(),
                desde,
                hasta
        );
    }
    @Override
    public List<Cita> findByMedicoIdAndFecha(
            MedicoId medicoId,
            LocalDateTime inicio,
            LocalDateTime fin) {

        return springDataCitaRepository
                .findByMedicoIdAndFechaHoraBetween(
                        medicoId.value(),
                        inicio,
                        fin
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
    @Override
    public List<Cita> findByMedicoId(MedicoId medicoId) {
        return springDataCitaRepository
                .findByMedicoId(medicoId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Cita> findByFecha(LocalDateTime inicio, LocalDateTime fin) {
        return springDataCitaRepository
                .findByFechaHoraBetween(inicio, fin)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Cita> findAll() {
        return springDataCitaRepository
                .findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Cita> findByPacienteId(PacienteId pacienteId) {
        return springDataCitaRepository
                .findByPacienteId(pacienteId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Cita> findByPacienteIdAndFecha(PacienteId pacienteId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataCitaRepository
                .findByPacienteIdAndFechaHoraBetween(pacienteId.value(), inicio, fin)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existeConsultaGeneralAtendidaByPacienteId(PacienteId pacienteId) {
        return springDataCitaRepository.existeConsultaGeneralAtendidaByPacienteId(pacienteId.value());
    }

    @Override
    public boolean existeCitaProgramadaByPacienteId(PacienteId pacienteId) {
        return springDataCitaRepository.existeCitaProgramadaByPacienteId(pacienteId.value());
    }

    private static UUID toUuid(CitaId citaId) {
        return citaId.value();
    }
}