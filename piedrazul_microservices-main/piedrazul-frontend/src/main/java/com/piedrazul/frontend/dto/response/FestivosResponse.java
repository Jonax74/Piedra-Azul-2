package com.piedrazul.frontend.dto.response;

import java.time.LocalDate;
import java.util.List;

public class FestivosResponse {

    private List<LocalDate> festivos;

    public List<LocalDate> getFestivos() {
        return festivos;
    }

    public void setFestivos(List<LocalDate> festivos) {
        this.festivos = festivos;
    }
}
