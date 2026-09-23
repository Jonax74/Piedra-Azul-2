package com.piedrazul.frontend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piedrazul.frontend.dto.request.ActualizarFestivosRequest;
import com.piedrazul.frontend.dto.request.ConfiguracionRequest;
import com.piedrazul.frontend.dto.response.ConfiguracionResponse;
import com.piedrazul.frontend.dto.response.FestivosResponse;
import com.piedrazul.frontend.http.AuthenticatedHttpClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConfiguracionClient {

    private static final String URL_CONFIG = AuthenticatedHttpClient.baseUrl() + "/api/configuracion";
    private static final String URL_FESTIVOS = URL_CONFIG + "/festivos";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public ConfiguracionResponse obtenerConfiguracion() {
        try {
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(URL_CONFIG);
            return mapper.readValue(resp.getBody(), ConfiguracionResponse.class);
        } catch (AuthenticatedHttpClient.HttpException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void guardarConfiguracion(Integer semanas) {
        try {
            ConfiguracionRequest request = new ConfiguracionRequest();
            request.setSemanasDisponibles(semanas);

            String body = mapper.writeValueAsString(request);
            AuthenticatedHttpClient.put(URL_CONFIG, body);
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error guardando configuracion: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<LocalDate> obtenerFestivos() {
        try {
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(URL_FESTIVOS);
            FestivosResponse response = mapper.readValue(resp.getBody(), FestivosResponse.class);

            if (response == null || response.getFestivos() == null) {
                return List.of();
            }

            List<LocalDate> fechas = new ArrayList<>(response.getFestivos());
            fechas.sort(Comparator.naturalOrder());
            return fechas;
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error obteniendo festivos: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<LocalDate> guardarFestivos(List<LocalDate> festivos) {
        try {
            ActualizarFestivosRequest request = new ActualizarFestivosRequest();
            request.setFestivos(festivos);

            String body = mapper.writeValueAsString(request);
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.put(URL_FESTIVOS, body);
            FestivosResponse response = mapper.readValue(resp.getBody(), FestivosResponse.class);

            if (response == null || response.getFestivos() == null) {
                return List.of();
            }

            List<LocalDate> fechas = new ArrayList<>(response.getFestivos());
            fechas.sort(Comparator.naturalOrder());
            return fechas;
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw new RuntimeException("Error guardando festivos: " + e.getResponseBody(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
