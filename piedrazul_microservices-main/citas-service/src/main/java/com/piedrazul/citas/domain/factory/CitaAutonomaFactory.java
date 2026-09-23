package com.piedrazul.citas.domain.factory;

import com.piedrazul.citas.domain.builder.CitaAutonomaBuilder;
import com.piedrazul.citas.domain.builder.CitaBuilder;
import org.springframework.stereotype.Component;

@Component
public class CitaAutonomaFactory implements CitaBuilderFactory {

    @Override
    public CitaBuilder crearBuilder() {
        return new CitaAutonomaBuilder();
    }
}
