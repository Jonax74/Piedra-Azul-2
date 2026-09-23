package com.piedrazul.citas.interfaces.rest.controller;

import com.piedrazul.citas.domain.model.EspecialidadMedica;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/citas/especialidades")
public class EspecialidadController {

    @GetMapping
    public List<EspecialidadMedica> listarCatalogo() {
        return List.of(EspecialidadMedica.values());
    }
}
