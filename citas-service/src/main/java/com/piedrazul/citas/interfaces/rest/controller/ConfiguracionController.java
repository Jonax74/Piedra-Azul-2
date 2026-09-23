package com.piedrazul.citas.interfaces.rest.controller;

import com.piedrazul.citas.application.service.ConfiguracionService;
import com.piedrazul.citas.domain.model.ConfiguracionSistema;
import com.piedrazul.citas.interfaces.rest.dto.request.ActualizarConfiguracionRequest;
import com.piedrazul.citas.interfaces.rest.dto.request.ActualizarFestivosRequest;
import com.piedrazul.citas.interfaces.rest.dto.response.ConfiguracionResponse;
import com.piedrazul.citas.interfaces.rest.dto.response.FestivosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService service;

    @GetMapping
    public ConfiguracionResponse obtener() {
        ConfiguracionSistema config = service.obtener();

        if (config == null) {
            return null;
        }

        return new ConfiguracionResponse(
                config.getSemanasDisponibles()
        );
    }

    @PutMapping
    public ConfiguracionResponse actualizar(
            @RequestBody ActualizarConfiguracionRequest request
    ) {
        ConfiguracionSistema actualizada =
                service.actualizar(request.getSemanasDisponibles());

        return new ConfiguracionResponse(
                actualizada.getSemanasDisponibles()
        );
    }

    @GetMapping("/festivos")
    public FestivosResponse listarFestivos() {
        Set<LocalDate> festivos = service.listarFestivos();
        return new FestivosResponse(new ArrayList<>(festivos));
    }

    @PutMapping("/festivos")
    public FestivosResponse actualizarFestivos(
            @RequestBody ActualizarFestivosRequest request
    ) {
        Set<LocalDate> actualizados = service.actualizarFestivos(
                request.getFestivos() == null ? Set.of() : Set.copyOf(request.getFestivos())
        );

        return new FestivosResponse(new ArrayList<>(actualizados));
    }
}
