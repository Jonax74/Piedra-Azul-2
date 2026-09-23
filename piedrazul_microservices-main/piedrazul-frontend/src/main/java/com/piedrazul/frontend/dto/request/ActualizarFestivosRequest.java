package com.piedrazul.frontend.dto.request;

import java.time.LocalDate;
import java.util.List;

public class ActualizarFestivosRequest {

    private List<LocalDate> festivos;

    public List<LocalDate> getFestivos() {
        return festivos;
    }

    public void setFestivos(List<LocalDate> festivos) {
        this.festivos = festivos;
    }
}
