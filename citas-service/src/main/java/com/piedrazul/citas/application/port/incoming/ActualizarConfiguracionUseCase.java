package com.piedrazul.citas.application.port.incoming;

import com.piedrazul.citas.domain.model.ConfiguracionSistema;

public interface ActualizarConfiguracionUseCase {

    ConfiguracionSistema actualizar(Integer semanasDisponibles);
}