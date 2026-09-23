package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import com.piedrazul.personas.domain.repository.IPacienteRepository;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import com.piedrazul.personas.infrastructure.messaging.publisher.MedicoEventPublisher;
import com.piedrazul.personas.infrastructure.messaging.publisher.PacienteEventPublisher;
import com.piedrazul.personas.interfaces.rest.dto.request.ActualizarPersonaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActualizarPersonaService {

    private final IPersonaRepository personaRepository;
    private final IPacienteRepository pacienteRepository;
    private final IMedicoRepository medicoRepository;
    private final PacienteEventPublisher pacienteEventPublisher;
    private final MedicoEventPublisher medicoEventPublisher;

    public Persona ejecutar(Long personaId, ActualizarPersonaRequest request) {
        Persona persona = personaRepository.buscarPorId(personaId)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "No existe una persona con id: " + personaId
                ));

        persona.actualizarDatosAdministrables(
                request.getPrimerNombre(),
                request.getSegundoNombre(),
                request.getPrimerApellido(),
                request.getSegundoApellido(),
                request.getGenero(),
                request.getFechaNacimiento(),
                request.getTelefono()
        );

        Persona guardada = personaRepository.guardar(persona);
        publicarActualizacionSnapshots(guardada);
        return guardada;
    }

    private void publicarActualizacionSnapshots(Persona persona) {
        Long personaId = persona.getId();
        String nombreCompleto = persona.getNombreCompleto();

        pacienteRepository.buscarPorPersonaId(personaId).ifPresent(paciente -> {
            pacienteEventPublisher.publicarPacienteActualizado(
                    personaId,
                    nombreCompleto,
                    persona.getCorreo(),
                    persona.getTelefono()
            );
            log.info("Evento de snapshot de paciente publicado para personaId: {}", personaId);
        });

        medicoRepository.buscarPorPersonaId(personaId).ifPresent(medico ->
                publicarMedicoActualizado(persona, medico)
        );
    }

    private void publicarMedicoActualizado(Persona persona, Medico medico) {
        List<String> especialidades = medico.getEspecialidades().stream()
                .map(Enum::name)
                .toList();

        medicoEventPublisher.publicarMedicoActualizado(
                persona.getId(),
                persona.getNombreCompleto(),
                persona.getCorreo(),
                especialidades,
                medico.getEstado().name()
        );
        log.info("Evento MedicoActualizado publicado tras actualizar persona: {}", persona.getId());
    }
}
