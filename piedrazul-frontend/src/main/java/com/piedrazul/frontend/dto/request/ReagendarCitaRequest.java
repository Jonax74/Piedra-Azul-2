package com.piedrazul.frontend.dto.request;

import java.time.LocalDateTime;

public class ReagendarCitaRequest {

    private String citaId;
    private LocalDateTime nuevaFechaHora;

    public ReagendarCitaRequest() {
    }

    public ReagendarCitaRequest(String citaId, LocalDateTime nuevaFechaHora) {
        this.citaId = citaId;
        this.nuevaFechaHora = nuevaFechaHora;
    }

    public String getCitaId() {
        return citaId;
    }

    public void setCitaId(String citaId) {
        this.citaId = citaId;
    }

    public LocalDateTime getNuevaFechaHora() {
        return nuevaFechaHora;
    }

    public void setNuevaFechaHora(LocalDateTime nuevaFechaHora) {
        this.nuevaFechaHora = nuevaFechaHora;
    }
}
