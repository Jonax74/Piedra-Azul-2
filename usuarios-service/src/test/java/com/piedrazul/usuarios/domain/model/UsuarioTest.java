package com.piedrazul.usuarios.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Usuario")
class UsuarioTest {

    private Usuario usuario;
    private Rol rol;

    @BeforeEach
    void setUp() {
        rol = Rol.builder().id(1).nombre("PACIENTE").build();
        usuario = Usuario.builder()
                .id(1)
                .username("jperez")
                .passwordHash("hash123")
                .estado(EstadoUsuario.ACTIVO)
                .personaId(100)
                .intentosFallidos(0)
                .build();
        usuario.asignarRol(rol);
    }

    @Test
    @DisplayName("Debe crear un usuario correctamente")
    void testCrearUsuario() {
        assertNotNull(usuario);
        assertEquals(1, usuario.getId());
        assertEquals("jperez", usuario.getUsername());
        assertEquals("hash123", usuario.getPasswordHash());
        assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
        assertEquals(100, usuario.getPersonaId());
        assertEquals(0, usuario.getIntentosFallidos());
        assertTrue(usuario.estaActivo());
    }

    @Test
    @DisplayName("Debe incrementar los intentos fallidos")
    void testIncrementarIntentosFallidos() {
        usuario.incrementarIntentosFallidos();
        assertEquals(1, usuario.getIntentosFallidos());
        assertTrue(usuario.estaActivo());
    }

    @Test
    @DisplayName("Debe desactivar después de 3 intentos fallidos")
    void testDesactivarPorIntentosFallidos() {
        usuario.incrementarIntentosFallidos();
        usuario.incrementarIntentosFallidos();
        usuario.incrementarIntentosFallidos();

        assertEquals(3, usuario.getIntentosFallidos());
        assertEquals(EstadoUsuario.INACTIVO, usuario.getEstado());
        assertFalse(usuario.estaActivo());
    }

    @Test
    @DisplayName("Debe resetear los intentos fallidos")
    void testResetearIntentosFallidos() {
        usuario.incrementarIntentosFallidos();
        usuario.incrementarIntentosFallidos();
        usuario.resetearIntentosFallidos();

        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    @DisplayName("Debe tener un rol asignado")
    void testTieneRol() {
        assertTrue(usuario.tieneRol("PACIENTE"));
        assertFalse(usuario.tieneRol("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Debe poder asignar múltiples roles")
    void testAsignarMultiplesRoles() {
        Rol rolAdmin = Rol.builder().id(2).nombre("ADMINISTRADOR").build();
        usuario.asignarRol(rolAdmin);

        assertTrue(usuario.tieneRol("PACIENTE"));
        assertTrue(usuario.tieneRol("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Debe poder cambiar la contraseña")
    void testCambiarPasswordHash() {
        usuario.cambiarPasswordHash("newHash456");

        assertEquals("newHash456", usuario.getPasswordHash());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la nueva contraseña es nula")
    void testCambiarPasswordHashNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            usuario.cambiarPasswordHash(null);
        });
    }

    @Test
    @DisplayName("Debe poder desactivar manualmente")
    void testDesactivar() {
        usuario.desactivar();

        assertEquals(EstadoUsuario.INACTIVO, usuario.getEstado());
        assertFalse(usuario.estaActivo());
    }
}