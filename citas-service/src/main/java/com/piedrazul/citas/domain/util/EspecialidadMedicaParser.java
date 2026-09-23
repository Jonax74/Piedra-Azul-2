package com.piedrazul.citas.domain.util;

import com.piedrazul.citas.domain.model.EspecialidadMedica;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class EspecialidadMedicaParser {

    private EspecialidadMedicaParser() {
    }

    public static Set<EspecialidadMedica> resolver(List<String> especialidades, String resumenLegacy) {
        if (especialidades != null && !especialidades.isEmpty()) {
            return especialidades.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(EspecialidadMedica::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return parsearResumen(resumenLegacy);
    }

    public static Set<EspecialidadMedica> parsearResumen(String resumen) {
        if (resumen == null || resumen.isBlank()) {
            return Set.of(EspecialidadMedica.GENERAL);
        }

        return Arrays.stream(resumen.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(EspecialidadMedica::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
