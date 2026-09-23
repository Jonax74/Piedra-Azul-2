package com.piedrazul.frontend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piedrazul.frontend.config.ApiConfig;
import com.piedrazul.frontend.dto.response.NotificacionResponse;
import com.piedrazul.frontend.http.AuthenticatedHttpClient;
import com.piedrazul.frontend.util.ApiErrorParser;

import java.util.List;
import java.util.Map;

public class NotificationClient {

    private final ObjectMapper objectMapper;

    private static String notificacionesBaseUrl() {
        return ApiConfig.gatewayBaseUrl() + "/api/notificaciones";
    }

    public NotificationClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<NotificacionResponse> listarMisNotificaciones(Long personaId, Boolean leida) {
        try {
            StringBuilder url = new StringBuilder(notificacionesBaseUrl() + "/mias?personaId=" + personaId);
            if (leida != null) {
                url.append("&leida=").append(leida);
            }
            AuthenticatedHttpClient.Response resp = AuthenticatedHttpClient.get(url.toString());
            return objectMapper.readValue(
                    resp.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificacionResponse.class)
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw apiException("Error listando notificaciones", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar notificaciones: " + e.getMessage(), e);
        }
    }

    public long contarNoLeidas(Long personaId) {
        try {
            AuthenticatedHttpClient.Response resp =
                    AuthenticatedHttpClient.get(notificacionesBaseUrl() + "/mias/count?personaId=" + personaId);
            Map<?, ?> body = objectMapper.readValue(resp.getBody(), Map.class);
            Object count = body.get("count");
            if (count instanceof Number number) {
                return number.longValue();
            }
            return 0L;
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw apiException("Error contando notificaciones", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al contar notificaciones: " + e.getMessage(), e);
        }
    }

    public void marcarLeida(String notificacionId, Long personaId) {
        try {
            AuthenticatedHttpClient.put(
                    notificacionesBaseUrl() + "/" + notificacionId + "/leida?personaId=" + personaId,
                    null
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw apiException("Error marcando notificación", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al marcar notificación como leída: " + e.getMessage(), e);
        }
    }

    public void marcarTodasLeidas(Long personaId) {
        try {
            AuthenticatedHttpClient.put(
                    notificacionesBaseUrl() + "/leer-todas?personaId=" + personaId,
                    null
            );
        } catch (AuthenticatedHttpClient.HttpException e) {
            throw apiException("Error marcando notificaciones", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al marcar todas las notificaciones: " + e.getMessage(), e);
        }
    }

    private static RuntimeException apiException(String context, AuthenticatedHttpClient.HttpException e) {
        ApiErrorParser.ParsedApiError parsed = ApiErrorParser.parse(e.getResponseBody());
        return new RuntimeException(parsed.message(), e);
    }
}
