package com.piedrazul.frontend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.frontend.config.ApiConfig;
import com.piedrazul.frontend.dto.response.PacienteResponse;
import com.piedrazul.frontend.http.AuthenticatedHttpClient;
import com.piedrazul.frontend.util.ApiClientException;
import com.piedrazul.frontend.util.ApiErrorParser;

import java.util.List;
import java.util.Map;

public class PacienteClient {

    private static String pacientesBaseUrl() {
        return ApiConfig.gatewayBaseUrl() + "/api/pacientes";
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void crearPaciente(Long personaId) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("personaId", personaId));
            AuthenticatedHttpClient.post(pacientesBaseUrl(), body);
        } catch (AuthenticatedHttpClient.HttpException e) {
            ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
            throw new ApiClientException(parsed, e);
        } catch (Exception e) {
            throw new RuntimeException("Error en PacienteClient: " + e.getMessage(), e);
        }
    }

    public List<PacienteResponse> obtenerPacientes() {
        try {
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(pacientesBaseUrl());
            return objectMapper.readValue(
                    resp.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PacienteResponse.class)
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error obteniendo pacientes: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error en PacienteClient: " + e.getMessage(), e);
        }
    }
}
