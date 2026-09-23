package com.piedrazul.usuarios.application.service;

import com.piedrazul.usuarios.application.exception.CredencialesInvalidasException;
import com.piedrazul.usuarios.application.security.PasswordHasher;
import com.piedrazul.usuarios.domain.model.EstadoUsuario;
import com.piedrazul.usuarios.domain.model.Rol;
import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.domain.repository.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de AutenticarUsuarioService")
class AutenticarUsuarioServiceTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private AutenticarUsuarioService autenticarUsuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Rol rol = Rol.builder().id(1).nombre("PACIENTE").build();
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
    @DisplayName("Debe autenticar usuario exitosamente")
    void testAutenticarExitoso() {
        when(usuarioRepository.buscarPorUsername("jperez")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("password123", "hash123")).thenReturn(true);
        when(usuarioRepository.guardar(any(Usuario.class))).thenReturn(usuario);

        Usuario resultado = autenticarUsuarioService.autenticar("jperez", "password123");

        assertNotNull(resultado);
        assertEquals("jperez", resultado.getUsername());
        // Cambiar de 2 a 1 vez
        verify(usuarioRepository, times(1)).guardar(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe")
    void testAutenticarUsuarioNoExiste() {
        when(usuarioRepository.buscarPorUsername("noexiste")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> {
            autenticarUsuarioService.autenticar("noexiste", "password");
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario está inactivo")
    void testAutenticarUsuarioInactivo() {
        usuario.setEstado(EstadoUsuario.INACTIVO);
        when(usuarioRepository.buscarPorUsername("jperez")).thenReturn(Optional.of(usuario));

        assertThrows(IllegalStateException.class, () -> {
            autenticarUsuarioService.autenticar("jperez", "password123");
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción si la contraseña es incorrecta")
    void testAutenticarPasswordIncorrecto() {
        when(usuarioRepository.buscarPorUsername("jperez")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("wrongpass", "hash123")).thenReturn(false);
        when(usuarioRepository.guardar(any(Usuario.class))).thenReturn(usuario);

        assertThrows(CredencialesInvalidasException.class, () -> {
            autenticarUsuarioService.autenticar("jperez", "wrongpass");
        });

        verify(usuarioRepository).guardar(usuario);
        assertEquals(1, usuario.getIntentosFallidos());
    }

    @Test
    @DisplayName("Debe lanzar excepción si username es nulo")
    void testAutenticarUsernameNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            autenticarUsuarioService.autenticar(null, "password");
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción si password es nulo")
    void testAutenticarPasswordNulo() {
        assertThrows(IllegalArgumentException.class, () -> {
            autenticarUsuarioService.autenticar("jperez", null);
        });
    }
}