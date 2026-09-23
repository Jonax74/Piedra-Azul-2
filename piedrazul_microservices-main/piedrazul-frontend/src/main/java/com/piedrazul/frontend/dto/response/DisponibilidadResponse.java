package com.piedrazul.frontend.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisponibilidadResponse {

    private Long id;
    private Long medicoId;
    private String diaSemana;
    private String horaInicio;
    private String horaFin;
    private Integer intervaloMinutos;
}