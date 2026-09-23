package com.piedrazul.citas.interfaces.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class FestivosResponse {

    private List<LocalDate> festivos;
}
