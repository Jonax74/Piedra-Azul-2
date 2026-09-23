package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PacienteYaRegistradoException;
import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.Paciente;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IPacienteRepository;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import com.piedrazul.personas.infrastructure.messaging.publisher.PacienteEventPublisher;
import com.piedrazul.personas.interfaces.rest.dto.request.RegistrarPacienteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistrarPacienteService {

    private final IPersonaRepository personaRepository;
    private final IPacienteRepository pacienteRepository;
    private final PacienteEventPublisher pacienteEventPublisher;

    public Paciente registrarPaciente(RegistrarPacienteRequest request) {
        log.info("Registrando nuevo paciente con personaId: {}", request.getPersonaId());

        // Validar que la persona existe
        Persona persona = personaRepository.buscarPorId(request.getPersonaId())
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "Persona no encontrada con ID: " + request.getPersonaId()
                ));

        if (pacienteRepository.existsByPersonaId(request.getPersonaId())) {
            throw new PacienteYaRegistradoException("La persona ya está registrada como paciente");
        }

        // Crear paciente
        Paciente paciente = new Paciente(request.getPersonaId());
        Paciente pacienteGuardado = pacienteRepository.guardar(paciente);

        log.info("Paciente registrado exitosamente con ID: {}", request.getPersonaId());

        // Publicar evento para citas-service (ASÍNCRONO)
        String nombreCompleto = persona.getPrimerNombre() + " " + persona.getPrimerApellido();
        pacienteEventPublisher.publicarPacienteCreado(
                request.getPersonaId(),
                nombreCompleto,
                persona.getCorreo(),
                persona.getTelefono()
        );

        log.info("Evento PacienteCreado publicado para ID: {}", request.getPersonaId());

        return pacienteGuardado;  // ← Devuelve el paciente creado
    }
}