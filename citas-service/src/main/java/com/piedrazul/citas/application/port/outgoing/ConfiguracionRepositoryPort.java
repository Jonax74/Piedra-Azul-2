package com.piedrazul.citas.application.port.outgoing;

import com.piedrazul.citas.domain.model.ConfiguracionSistema;

public interface ConfiguracionRepositoryPort {

    ConfiguracionSistema obtener();

    ConfiguracionSistema guardar(ConfiguracionSistema configuracion);
}
