package com.piedrazul.usuarios.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Rol")
class RolTest {

    @Test
    @DisplayName("Debe crear un rol correctamente")
    void testCrearRol() {
        Rol rol = Rol.builder()
                .id(1)
                .nombre("ADMINISTRADOR")
                .build();

        assertNotNull(rol);
        assertEquals(1, rol.getId());
        assertEquals("ADMINISTRADOR", rol.getNombre());
    }

    @Test
    @DisplayName("Debe permitir modificar el nombre")
    void testSetNombre() {
        Rol rol = new Rol();
        rol.setNombre("PACIENTE");

        assertEquals("PACIENTE", rol.getNombre());
    }

    @Test
    @DisplayName("Dos roles con mismo id deben ser iguales")
    void testEquals() {
        Rol rol1 = Rol.builder().id(1).nombre("PACIENTE").build();
        Rol rol2 = Rol.builder().id(1).nombre("PACIENTE").build();

        assertEquals(rol1, rol2);
        assertEquals(rol1.hashCode(), rol2.hashCode());
    }

    @Test
    @DisplayName("Roles con diferente id no deben ser iguales")
    void testNotEquals() {
        Rol rol1 = Rol.builder().id(1).nombre("PACIENTE").build();
        Rol rol2 = Rol.builder().id(2).nombre("PACIENTE").build();

        assertNotEquals(rol1, rol2);
    }
}