package com.piedrazul.citas.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.citas.application.port.outgoing.ConfiguracionRepositoryPort;
import com.piedrazul.citas.domain.model.ConfiguracionSistema;
import com.piedrazul.citas.infrastructure.persistence.entity.ConfiguracionSistemaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ConfiguracionRepositoryImpl implements ConfiguracionRepositoryPort {

    private final SpringDataConfiguracionRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public ConfiguracionSistema obtener() {
        return repository.findById(1L)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public ConfiguracionSistema guardar(ConfiguracionSistema configuracion) {
        ConfiguracionSistemaEntity entity = new ConfiguracionSistemaEntity();
        entity.setId(1L);
        entity.setSemanasDisponibles(configuracion.getSemanasDisponibles());
        entity.setFestivos(serializeFestivos(configuracion.getFestivos()));

        ConfiguracionSistemaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private ConfiguracionSistema toDomain(ConfiguracionSistemaEntity entity) {
        return new ConfiguracionSistema(
                entity.getSemanasDisponibles(),
                deserializeFestivos(entity.getFestivos())
        );
    }

    private String serializeFestivos(Set<LocalDate> festivos) {
        try {
            return objectMapper.writeValueAsString(festivos);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando festivos", e);
        }
    }

    private Set<LocalDate> deserializeFestivos(String json) {
        if (json == null || json.isBlank()) {
            return new HashSet<>();
        }

        try {
            List<LocalDate> fechas = objectMapper.readValue(json, new TypeReference<>() {});
            return new HashSet<>(fechas);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializando festivos", e);
        }
    }
}
