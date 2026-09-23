package com.piedrazul.citas.application.service.singleton;

import com.piedrazul.citas.application.port.outgoing.ConfiguracionRepositoryPort;
import com.piedrazul.citas.domain.model.ConfiguracionSistema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ConfiguracionManager {

    private final ConfiguracionRepositoryPort configuracionRepository;

    private ConfiguracionSistema configuracionCache;

    @PostConstruct
    public void inicializar() {
        configuracionCache = configuracionRepository.obtener();

        if (configuracionCache == null) {
            configuracionCache = configuracionRepository.guardar(new ConfiguracionSistema(4));
        }
    }

    public ConfiguracionSistema obtenerConfiguracion() {
        return configuracionCache;
    }

    public ConfiguracionSistema actualizarConfiguracion(Integer semanasDisponibles) {
        ConfiguracionSistema nuevaConfiguracion = configuracionCache.conSemanas(semanasDisponibles);
        configuracionCache = configuracionRepository.guardar(nuevaConfiguracion);
        return configuracionCache;
    }

    public ConfiguracionSistema actualizarFestivos(Set<LocalDate> festivos) {
        ConfiguracionSistema nuevaConfiguracion = configuracionCache.conFestivos(festivos);
        configuracionCache = configuracionRepository.guardar(nuevaConfiguracion);
        return configuracionCache;
    }
}
