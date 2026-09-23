package com.piedrazul.frontend.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piedrazul.frontend.dto.request.CrearUsuarioRequest;
import com.piedrazul.frontend.dto.response.UsuarioResponse;
import com.piedrazul.frontend.http.AuthenticatedHttpClient;
import com.piedrazul.frontend.util.ApiClientException;
import com.piedrazul.frontend.util.ApiErrorParser;

import java.util.List;

public class UsuarioClient {

    private static final String BASE_URL = AuthenticatedHttpClient.baseUrl() + "/api/usuarios";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public void crearUsuario(CrearUsuarioRequest request) {
        try {
            String body = mapper.writeValueAsString(request);
            AuthenticatedHttpClient.post(BASE_URL, body);
        } catch (AuthenticatedHttpClient.HttpException e) {
            ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
            throw new ApiClientException(parsed, e);
        } catch (Exception e) {
            throw new RuntimeException("Error creando usuario: " + e.getMessage(), e);
        }
    }

    public UsuarioResponse obtenerPorUsername(String username) {
        try {
            AuthenticatedHttpClient.Response resp =
                    AuthenticatedHttpClient.get(BASE_URL + "/by-username/" + username);
            return mapper.readValue(resp.getBody(), UsuarioResponse.class);
        } catch (AuthenticatedHttpClient.HttpException e) {
            ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
            throw new ApiClientException(parsed, e);
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo usuario: " + e.getMessage(), e);
        }
    }

    public List<UsuarioResponse> listarUsuarios() {
        try {
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(BASE_URL);
            return mapper.readValue(
                    resp.getBody(),
                    mapper.getTypeFactory().constructCollectionType(List.class, UsuarioResponse.class)
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
            throw new ApiClientException(parsed, e);
        } catch (Exception e) {
            throw new RuntimeException("Error listando usuarios: " + e.getMessage(), e);
        }
    }
}
