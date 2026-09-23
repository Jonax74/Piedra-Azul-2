package com.piedrazul.citas.application.service;

import com.piedrazul.citas.application.port.incoming.ActualizarConfiguracionUseCase;
import com.piedrazul.citas.application.port.incoming.ConsultarConfiguracionUseCase;
import com.piedrazul.citas.application.port.incoming.GestionarFestivosUseCase;
import com.piedrazul.citas.application.service.singleton.ConfiguracionManager;
import com.piedrazul.citas.domain.model.ConfiguracionSistema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class ConfiguracionService
        implements ConsultarConfiguracionUseCase,
        ActualizarConfiguracionUseCase,
        GestionarFestivosUseCase {

    private final ConfiguracionManager configuracionManager;

    @Override
    public ConfiguracionSistema obtener() {
        return configuracionManager.obtenerConfiguracion();
    }

    @Override
    public ConfiguracionSistema actualizar(Integer semanasDisponibles) {
        validarSemanas(semanasDisponibles);
        return configuracionManager.actualizarConfiguracion(semanasDisponibles);
    }

    @Override
    public Set<LocalDate> listarFestivos() {
        return configuracionManager.obtenerConfiguracion().getFestivos();
    }

    @Override
    public Set<LocalDate> actualizarFestivos(Set<LocalDate> festivos) {
        Set<LocalDate> normalizados = festivos == null
                ? new TreeSet<>()
                : new TreeSet<>(festivos);

        if (normalizados.stream().anyMatch(f -> f == null)) {
            throw new IllegalArgumentException("Las fechas festivas no pueden ser nulas");
        }

        return configuracionManager
                .actualizarFestivos(new HashSet<>(normalizados))
                .getFestivos();
    }

    private void validarSemanas(Integer semanasDisponibles) {
        if (semanasDisponibles == null || semanasDisponibles <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de semanas debe ser mayor a cero"
            );
        }

        if (semanasDisponibles > 52) {
            throw new IllegalArgumentException(
                    "La cantidad máxima permitida es 52 semanas"
            );
        }
    }
}