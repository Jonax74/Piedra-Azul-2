package com.piedrazul.citas.interfaces.rest.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ActualizarFestivosRequest {

    private List<LocalDate> festivos;
}
