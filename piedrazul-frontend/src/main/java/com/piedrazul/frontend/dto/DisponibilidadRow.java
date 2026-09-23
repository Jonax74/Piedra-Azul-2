package com.piedrazul.frontend.dto;

public class DisponibilidadRow {

    private final Long id;
    private final Long medicoId;
    private final String medico;
    private final String dia;
    private final String horaInicio;
    private final String horaFin;
    private final Integer intervalo;

    public DisponibilidadRow(Long id, Long medicoId, String medico, String dia,
                             String horaInicio, String horaFin, Integer intervalo) {
        this.id = id;
        this.medicoId = medicoId;
        this.medico = medico;
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.intervalo = intervalo;
    }

    public Long getId() { return id; }
    public Long getMedicoId() { return medicoId; }
    public String getMedico() { return medico; }
    public String getDia() { return dia; }
    public String getHoraInicio() { return horaInicio; }
    public String getHoraFin() { return horaFin; }
    public Integer getIntervalo() { return intervalo; }
}
