package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.MedicoYaRegistradoException;
import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IMedicoEspecialidadRepository;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import com.piedrazul.personas.infrastructure.messaging.publisher.MedicoEventPublisher;
import com.piedrazul.personas.interfaces.rest.dto.request.RegistrarMedicoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistrarMedicoService {

    private final IPersonaRepository personaRepository;
    private final IMedicoRepository medicoRepository;
    private final IMedicoEspecialidadRepository medicoEspecialidadRepository;
    private final MedicoEventPublisher medicoEventPublisher;

    public Medico registrarMedico(RegistrarMedicoRequest request) {
        log.info("Registrando nuevo médico con personaId: {}", request.getPersonaId());

        Persona persona = personaRepository.buscarPorId(request.getPersonaId())
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "Persona no encontrada con ID: " + request.getPersonaId()
                ));

        if (medicoRepository.existePorPersonaId(request.getPersonaId())) {
            throw new MedicoYaRegistradoException(
                    "La persona ya está registrada como médico: " + request.getPersonaId()
            );
        }

        Medico medico = Medico.crear(
                request.getPersonaId(),
                request.getTipoProfesional(),
                request.getEspecialidades()
        );
        Medico medicoGuardado = medicoRepository.guardar(medico);
        medicoEspecialidadRepository.reemplazarEspecialidades(
                request.getPersonaId(),
                medico.getEspecialidades()
        );
        medicoGuardado.setEspecialidades(medico.getEspecialidades());

        log.info("Médico registrado exitosamente con ID: {}", request.getPersonaId());

        List<String> especialidadesEvento = medico.getEspecialidades().stream()
                .map(Enum::name)
                .toList();

        String nombreCompleto = persona.getPrimerNombre() + " " + persona.getPrimerApellido();
        medicoEventPublisher.publicarMedicoCreado(
                request.getPersonaId(),
                nombreCompleto,
                persona.getCorreo(),
                especialidadesEvento
        );

        log.info("Evento MedicoCreado publicado para ID: {}", request.getPersonaId());

        return medicoGuardado;
    }
}
