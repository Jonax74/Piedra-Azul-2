package com.piedrazul.citas.domain.factory;

import com.piedrazul.citas.domain.builder.CitaBuilder;
import com.piedrazul.citas.domain.builder.CitaManualBuilder;
import org.springframework.stereotype.Component;

@Component
public class CitaManualFactory implements CitaBuilderFactory {

    @Override
    public CitaBuilder crearBuilder() {
        return new CitaManualBuilder();
    }
}
