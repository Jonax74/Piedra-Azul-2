package com.piedrazul.personas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Persona")
class PersonaTest {

    @Test
    @DisplayName("Debería crear una persona correctamente")
    void testCrearPersona() {
        Persona persona = Persona.crear(
                "Juan", "Carlos", "Perez", "Lopez",
                Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                "3001234567", "12345678", "juan@email.com"
        );

        assertNotNull(persona);
        assertEquals("Juan", persona.getPrimerNombre());
        assertEquals("Carlos", persona.getSegundoNombre());
        assertEquals("Perez", persona.getPrimerApellido());
        assertEquals("Lopez", persona.getSegundoApellido());
        assertEquals(Genero.HOMBRE, persona.getGenero());
        assertEquals(LocalDate.of(1990, 5, 15), persona.getFechaNacimiento());
        assertEquals("3001234567", persona.getTelefono());
        assertEquals("12345678", persona.getDni());
        assertEquals("juan@email.com", persona.getCorreo());
        assertNull(persona.getId());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el primer nombre es nulo")
    void testPrimerNombreNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            Persona.crear(null, "Carlos", "Perez", "Lopez",
                    Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                    "3001234567", "12345678", "juan@email.com");
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si el primer apellido es nulo")
    void testPrimerApellidoNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            Persona.crear("Juan", "Carlos", null, "Lopez",
                    Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                    "3001234567", "12345678", "juan@email.com");
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si el género es nulo")
    void testGeneroNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            Persona.crear("Juan", "Carlos", "Perez", "Lopez",
                    null, LocalDate.of(1990, 5, 15),
                    "3001234567", "12345678", "juan@email.com");
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si el DNI es nulo")
    void testDniNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            Persona.crear("Juan", "Carlos", "Perez", "Lopez",
                    Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                    "3001234567", null, "juan@email.com");
        });
    }

    @Test
    @DisplayName("Debería obtener el nombre completo correctamente")
    void testGetNombreCompleto() {
        Persona persona = Persona.crear("Juan", "Carlos", "Perez", "Lopez",
                Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                "3001234567", "12345678", "juan@email.com");

        assertEquals("Juan Carlos Perez Lopez", persona.getNombreCompleto());
    }

    @Test
    @DisplayName("Debería obtener nombre completo sin segundos nombres")
    void testGetNombreCompletoSinSegundos() {
        Persona persona = Persona.crear("Juan", null, "Perez", null,
                Genero.HOMBRE, LocalDate.of(1990, 5, 15),
                "3001234567", "12345678", "juan@email.com");

        assertEquals("Juan Perez", persona.getNombreCompleto());
    }
}