package com.piedrazul.frontend.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piedrazul.frontend.config.ApiConfig;
import com.piedrazul.frontend.dto.request.ActualizarPersonaRequest;
import com.piedrazul.frontend.dto.request.CrearPersonaRequest;
import com.piedrazul.frontend.dto.response.PersonaResponse;
import com.piedrazul.frontend.http.AuthenticatedHttpClient;
import com.piedrazul.frontend.util.ApiClientException;
import com.piedrazul.frontend.util.ApiErrorParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class PersonaClient {

    private static String personasBaseUrl() {
        return ApiConfig.gatewayBaseUrl() + "/api/personas";
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public Long crearPersona(CrearPersonaRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);

            System.out.println("================================");
            System.out.println("URL: " + personasBaseUrl());
            System.out.println("BODY: " + body);
            System.out.println("================================");

            AuthenticatedHttpClient.Response resp =
                    AuthenticatedHttpClient.post(personasBaseUrl(), body);

            System.out.println("STATUS: " + resp.getStatusCode());
            System.out.println("RESPONSE: " + resp.getBody());

            JsonNode json = objectMapper.readTree(resp.getBody());

            if (!json.has("id")) {
                throw new RuntimeException("Respuesta sin 'id': " + resp.getBody());
            }

            return json.get("id").asLong();

        } catch (AuthenticatedHttpClient.HttpException e) {

            System.out.println("================================");
            System.out.println("ERROR HTTP");
            System.out.println("URL: " + personasBaseUrl());
            System.out.println("BODY ENVIADO: ");

            try {
                System.out.println(objectMapper.writeValueAsString(request));
            } catch (Exception ignored) {
            }

            System.out.println("STATUS: " + e.getStatusCode());
            System.out.println("RESPONSE: " + e.getResponseBody());
            System.out.println("================================");

            ApiErrorParser.ParsedApiError parsed =
                    ApiErrorParser.parse(e.getResponseBody());

            throw new ApiClientException(parsed, e);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error en PersonaClient: " + e.getMessage(), e);
        }
    }
    public void compensarRegistroFallido(Long personaId) {
        try {
            AuthenticatedHttpClient.delete(personasBaseUrl() + "/" + personaId + "/registro-fallido");
        } catch (Exception e) {
            System.err.println("No se pudo revertir persona " + personaId + ": " + e.getMessage());
        }
    }

    public Optional<PersonaResponse> buscarPorDni(String dni) {
        try {
            String encoded = URLEncoder.encode(dni, StandardCharsets.UTF_8);
            AuthenticatedHttpClient.Response resp =
                    AuthenticatedHttpClient.get(personasBaseUrl() + "/dni/" + encoded);
            return Optional.of(objectMapper.readValue(resp.getBody(), PersonaResponse.class));
        } catch (AuthenticatedHttpClient.HttpException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
            throw new ApiClientException(parsed, e);
        } catch (Exception e) {
            throw new RuntimeException("Error buscando persona por DNI: " + e.getMessage(), e);
        }
    }

    public List<PersonaResponse> listarPersonas() {
        try {
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(personasBaseUrl());
            return objectMapper.readValue(
                    resp.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PersonaResponse.class)
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error listando personas: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error en PersonaClient: " + e.getMessage(), e);
        }
    }

    public PersonaResponse obtenerPorId(Long personaId) {
        try {
            AuthenticatedHttpClient.Response resp =
                    AuthenticatedHttpClient.get(personasBaseUrl() + "/" + personaId);
            return objectMapper.readValue(resp.getBody(), PersonaResponse.class);
        } catch (AuthenticatedHttpClient.HttpException e) {
            ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
            throw new ApiClientException(parsed, e);
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo persona: " + e.getMessage(), e);
        }
    }

    public PersonaResponse actualizarPersona(Long personaId, ActualizarPersonaRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            AuthenticatedHttpClient.Response resp =
                    AuthenticatedHttpClient.put(personasBaseUrl() + "/" + personaId, body);
            return objectMapper.readValue(resp.getBody(), PersonaResponse.class);
        } catch (AuthenticatedHttpClient.HttpException e) {
            ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
            throw new ApiClientException(parsed, e);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando persona: " + e.getMessage(), e);
        }
    }
}
