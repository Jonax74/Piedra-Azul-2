package com.piedrazul.personas.application.service;

import com.piedrazul.personas.domain.model.Genero;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de CrearPersonaService")
class CrearPersonaServiceTest {

    @Mock
    private IPersonaRepository personaRepository;

    @InjectMocks
    private CrearPersonaService crearPersonaService;

    private Persona persona;

    @BeforeEach
    void setUp() {
        persona = Persona.crear(
                "Juan", "Carlos", "Perez", "Lopez",
                Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                "3001234567", "12345678", "juan@email.com"
        );
        persona.setId(1L);
    }

    @Test
    @DisplayName("Debería crear una persona exitosamente")
    void testEjecutarExitoso() {
        when(personaRepository.existePorDni(anyString())).thenReturn(false);
        when(personaRepository.guardar(any(Persona.class))).thenReturn(persona);

        Persona resultado = crearPersonaService.ejecutar(
                "Juan", "Carlos", "Perez", "Lopez",
                Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                "3001234567", "12345678", "juan@email.com"
        );

        assertNotNull(resultado);
        assertEquals("Juan", resultado.getPrimerNombre());
        verify(personaRepository).guardar(any(Persona.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción si el DNI ya existe")
    void testEjecutarDniExistente() {
        when(personaRepository.existePorDni("12345678")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            crearPersonaService.ejecutar(
                    "Juan", "Carlos", "Perez", "Lopez",
                    Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                    "3001234567", "12345678", "juan@email.com"
            );
        });

        verify(personaRepository, never()).guardar(any(Persona.class));
    }
}