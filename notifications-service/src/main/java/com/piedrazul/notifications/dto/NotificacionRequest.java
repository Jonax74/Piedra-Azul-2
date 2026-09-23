package com.piedrazul.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionRequest {

    @NotBlank
    private String destinatario;

    @NotBlank
    private String tipo;

    @NotBlank
    private String asunto;

    @NotBlank
    private String mensaje;

    @NotNull
    private String citaId;
}