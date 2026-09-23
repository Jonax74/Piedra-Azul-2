package com.piedrazul.usuarios.service.client;

import com.piedrazul.usuarios.application.exception.DependenciaExternaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class PersonaServiceClientImpl implements PersonaServiceClient {

    private final RestTemplate restTemplate;

    @Value("${personas.service.url}")
    private String personasServiceUrl;

    public PersonaServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean existePersona(Integer personaId, boolean requiereUsuario) {
        try {
            // Consultar si la persona existe en personas-service
            restTemplate.getForEntity(personasServiceUrl + "/api/personas/" + personaId, String.class);

            // Si es necesario verificar si la persona es también un usuario
            if (requiereUsuario) {
                // Lógica adicional para verificar si la persona tiene un Usuario asociado.
                // Esto puede implicar una consulta al endpoint correspondiente en usuarios-service.
            }

            return true; // Si no hay excepciones, la persona existe.
        } catch (HttpClientErrorException.NotFound e) {
            return false; // Persona no encontrada.
        } catch (Exception e) {
            throw new DependenciaExternaException("Error al comunicarse con personas-service.");
        }
    }
}