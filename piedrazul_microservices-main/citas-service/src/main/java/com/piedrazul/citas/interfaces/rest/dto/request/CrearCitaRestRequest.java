package com.piedrazul.citas.interfaces.rest.dto.request;

import com.piedrazul.citas.domain.model.EspecialidadMedica;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearCitaRestRequest {

    @NotNull(message = "El ID del paciente es obligatorio")
    @Positive(message = "El ID del paciente debe ser positivo")
    private Long pacienteId;

    @NotNull(message = "El ID del médico es obligatorio")
    @Positive(message = "El ID del médico debe ser positivo")
    private Long medicoId;

    @NotNull(message = "El ID del usuario creador es obligatorio")
    @Positive(message = "El ID del usuario creador debe ser positivo")
    private Long usuarioCreadorId;

    @NotNull(message = "La fecha y hora son obligatorias")
    private LocalDateTime fechaHora;

    @NotNull(message = "La especialidad es obligatoria")
    private EspecialidadMedica especialidad;

    @Size(max = 500, message = "El motivo de agendamiento no puede superar 500 caracteres")
    private String motivoAgendamiento;
}