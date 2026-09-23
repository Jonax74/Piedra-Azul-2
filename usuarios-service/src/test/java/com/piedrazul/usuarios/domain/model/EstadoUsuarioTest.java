package com.piedrazul.usuarios.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de EstadoUsuario")
class EstadoUsuarioTest {

    @Test
    @DisplayName("Debe contener los valores esperados")
    void testValoresEnum() {
        EstadoUsuario[] valores = EstadoUsuario.values();
        assertEquals(2, valores.length);

        assertTrue(containsValue(valores, EstadoUsuario.ACTIVO));
        assertTrue(containsValue(valores, EstadoUsuario.INACTIVO));
    }

    private boolean containsValue(EstadoUsuario[] valores, EstadoUsuario valor) {
        for (EstadoUsuario v : valores) {
            if (v == valor) return true;
        }
        return false;
    }

    @Test
    @DisplayName("ACTIVO debe tener nombre correcto")
    void testActivoNombre() {
        assertEquals("ACTIVO", EstadoUsuario.ACTIVO.name());
    }

    @Test
    @DisplayName("INACTIVO debe tener nombre correcto")
    void testInactivoNombre() {
        assertEquals("INACTIVO", EstadoUsuario.INACTIVO.name());
    }
}