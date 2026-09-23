package com.piedrazul.frontend.dto.request;

public class MarcarAsistenciaRequest {

    private String citaId;
    private boolean asistio;

    public MarcarAsistenciaRequest() {
    }

    public MarcarAsistenciaRequest(String citaId, boolean asistio) {
        this.citaId = citaId;
        this.asistio = asistio;
    }

    public String getCitaId() {
        return citaId;
    }

    public void setCitaId(String citaId) {
        this.citaId = citaId;
    }

    public boolean isAsistio() {
        return asistio;
    }

    public void setAsistio(boolean asistio) {
        this.asistio = asistio;
    }
}
