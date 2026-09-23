package com.piedrazul.usuarios.application.service;

import com.piedrazul.usuarios.application.security.PasswordHasher;
import com.piedrazul.usuarios.application.security.PasswordPolicyValidator;
import com.piedrazul.usuarios.domain.model.EstadoUsuario;
import com.piedrazul.usuarios.domain.model.Rol;
import com.piedrazul.usuarios.domain.model.Usuario;
import com.piedrazul.usuarios.domain.repository.IRolRepository;
import com.piedrazul.usuarios.domain.repository.IUsuarioRepository;
import com.piedrazul.usuarios.service.client.PersonaServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de RegistrarUsuarioService")
class RegistrarUsuarioServiceTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IRolRepository rolRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @Mock
    private PersonaServiceClient personaServiceClient;

    @InjectMocks
    private RegistrarUsuarioService registrarUsuarioService;

    private Rol rolPaciente;
    private List<String> roles;

    @BeforeEach
    void setUp() {
        rolPaciente = Rol.builder().id(1).nombre("PACIENTE").build();
        roles = List.of("PACIENTE");
    }

    @Test
    @DisplayName("Debe registrar un usuario exitosamente")
    void testRegistrarExitoso() throws Exception {
        when(personaServiceClient.existePersona(1, true)).thenReturn(true);
        when(usuarioRepository.existePorUsername("jperez")).thenReturn(false);
        when(usuarioRepository.existePorPersonaId(1)).thenReturn(false);
        when(rolRepository.buscarPorNombre("PACIENTE")).thenReturn(Optional.of(rolPaciente));

        Usuario usuarioEsperado = Usuario.builder()
                .id(1)
                .username("jperez")
                .passwordHash("hash123")
                .estado(EstadoUsuario.ACTIVO)
                .personaId(1)
                .intentosFallidos(0)
                .build();

        when(usuarioRepository.guardar(any(Usuario.class))).thenReturn(usuarioEsperado);

        Usuario resultado = registrarUsuarioService.ejecutar(1, "jperez", "Admin123!", roles);

        assertNotNull(resultado);
        assertEquals("jperez", resultado.getUsername());
        verify(passwordPolicyValidator).validar("Admin123!");
        verify(passwordHasher).hash("Admin123!");
        verify(usuarioRepository).guardar(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la persona no existe")
    void testRegistrarPersonaNoExiste() {
        when(personaServiceClient.existePersona(1, true)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            registrarUsuarioService.ejecutar(1, "jperez", "Admin123!", roles);
        });

        verify(usuarioRepository, never()).guardar(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el username ya existe")
    void testRegistrarUsernameExistente() {
        when(personaServiceClient.existePersona(1, true)).thenReturn(true);
        when(usuarioRepository.existePorUsername("jperez")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            registrarUsuarioService.ejecutar(1, "jperez", "Admin123!", roles);
        });
    }
}