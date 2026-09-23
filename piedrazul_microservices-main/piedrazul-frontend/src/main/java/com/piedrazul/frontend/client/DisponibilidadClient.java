package com.piedrazul.frontend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.frontend.dto.response.DisponibilidadResponse;
import com.piedrazul.frontend.http.AuthenticatedHttpClient;

import java.util.List;
import java.util.Map;

public class DisponibilidadClient {

    private static final String URL = AuthenticatedHttpClient.baseUrl() + "/api/disponibilidad";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void crearDisponibilidad(Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            AuthenticatedHttpClient.post(URL, json);
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error creando disponibilidad: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void crearDisponibilidad(Long medicoId,
                                    String dia,
                                    String horaInicio,
                                    String horaFin,
                                    Integer intervalo) {
        Map<String, Object> body = Map.of(
                "medicoId", medicoId,
                "diaSemana", dia,
                "horaInicio", horaInicio,
                "horaFin", horaFin,
                "intervaloMinutos", intervalo
        );
        crearDisponibilidad(body);
    }

    public void actualizarDisponibilidad(Long id,
                                         Long medicoId,
                                         String dia,
                                         String horaInicio,
                                         String horaFin,
                                         Integer intervalo) {
        try {
            Map<String, Object> body = Map.of(
                    "medicoId", medicoId,
                    "diaSemana", dia,
                    "horaInicio", horaInicio,
                    "horaFin", horaFin,
                    "intervaloMinutos", intervalo
            );
            String json = objectMapper.writeValueAsString(body);
            AuthenticatedHttpClient.put(URL + "/" + id, json);
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error actualizando disponibilidad: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void eliminarDisponibilidad(Long id) {
        try {
            AuthenticatedHttpClient.delete(URL + "/" + id);
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error eliminando disponibilidad: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<DisponibilidadResponse> obtenerDisponibilidades() {
        try {
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(URL);
            return objectMapper.readValue(
                    resp.getBody(),
                    new TypeReference<List<DisponibilidadResponse>>() {}
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error obteniendo disponibilidades: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
