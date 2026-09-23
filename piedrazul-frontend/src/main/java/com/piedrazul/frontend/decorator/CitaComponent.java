package com.piedrazul.frontend.decorator;

import com.piedrazul.frontend.dto.response.CitaResponse;

public interface CitaComponent {
    String getDescripcion();
    String getColor();
    CitaResponse getCita();
}

