package com.piedrazul.frontend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.frontend.dto.response.MedicoResponse;
import com.piedrazul.frontend.http.AuthenticatedHttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicoClient {

    private static final String URL_MEDICO = AuthenticatedHttpClient.baseUrl() + "/api/medicos";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void crearMedico(Long personaId, String tipoProfesional, List<String> especialidades) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("personaId", personaId);
            payload.put("tipoProfesional", tipoProfesional);
            payload.put("especialidades", especialidades);
            String body = objectMapper.writeValueAsString(payload);
            AuthenticatedHttpClient.post(URL_MEDICO, body);
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error creando medico: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error en MedicoClient: " + e.getMessage(), e);
        }
    }

    public List<MedicoResponse> obtenerMedicos() {
        try {
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(URL_MEDICO);
            return objectMapper.readValue(
                    resp.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MedicoResponse.class)
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error obteniendo medicos: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error en MedicoClient: " + e.getMessage(), e);
        }
    }
}
