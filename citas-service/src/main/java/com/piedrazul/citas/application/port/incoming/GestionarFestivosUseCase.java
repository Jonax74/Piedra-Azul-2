package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.domain.model.ConfiguracionSistema;

import java.time.LocalDate;
import java.util.Set;

public interface GestionarFestivosUseCase {

    Set<LocalDate> listarFestivos();

    Set<LocalDate> actualizarFestivos(Set<LocalDate> festivos);
}
