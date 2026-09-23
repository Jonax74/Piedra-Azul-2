package com.piedrazul.personas.infrastructure.client;

import com.piedrazul.personas.application.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CitasServiceClient {

    private final RestTemplate restTemplate;

    @Value("${citas.service.url}")
    private String citasServiceUrl;

    public void validarEliminacion(
            Long medicoId,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("medicoId", medicoId);
        body.put("diaSemana", diaSemana);
        body.put("horaInicio", horaInicio.toString());
        body.put("horaFin", horaFin.toString());

        postValidacion("/api/citas/disponibilidad/validar-eliminacion", body);
    }

    public void validarModificacion(
            Long medicoIdActual,
            String diaSemanaActual,
            LocalTime horaInicioActual,
            LocalTime horaFinActual,
            Long medicoIdNuevo,
            String diaSemanaNuevo,
            LocalTime horaInicioNuevo,
            LocalTime horaFinNuevo
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("medicoIdActual", medicoIdActual);
        body.put("diaSemanaActual", diaSemanaActual);
        body.put("horaInicioActual", horaInicioActual.toString());
        body.put("horaFinActual", horaFinActual.toString());
        body.put("medicoIdNuevo", medicoIdNuevo);
        body.put("diaSemanaNuevo", diaSemanaNuevo);
        body.put("horaInicioNuevo", horaInicioNuevo.toString());
        body.put("horaFinNuevo", horaFinNuevo.toString());

        postValidacion("/api/citas/disponibilidad/validar-modificacion", body);
    }

    private void postValidacion(String path, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(citasServiceUrl + path, request, Void.class);
        } catch (HttpClientErrorException.Conflict e) {
            throw new ReglaDeNegocioException(extractMessage(e.getResponseBodyAsString()));
        } catch (HttpClientErrorException e) {
            throw new ReglaDeNegocioException(extractMessage(e.getResponseBodyAsString()));
        } catch (Exception e) {
            throw new ReglaDeNegocioException("No fue posible validar las citas asociadas a esta disponibilidad.");
        }
    }

    private String extractMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Existen citas programadas dentro de esta disponibilidad. "
                    + "Debe atenderlas o reagendarlas antes de poder modificar o eliminar este horario.";
        }
        int messageIndex = responseBody.indexOf("\"message\"");
        if (messageIndex >= 0) {
            int start = responseBody.indexOf(':', messageIndex) + 1;
            int firstQuote = responseBody.indexOf('"', start);
            int secondQuote = responseBody.indexOf('"', firstQuote + 1);
            if (firstQuote >= 0 && secondQuote > firstQuote) {
                return responseBody.substring(firstQuote + 1, secondQuote);
            }
        }
        return responseBody;
    }
}
