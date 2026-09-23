package com.piedrazul.personas.application.service;

import com.piedrazul.personas.application.exception.MedicoNoEncontradoException;
import com.piedrazul.personas.application.exception.PersonaNoEncontradaException;
import com.piedrazul.personas.domain.model.EspecialidadMedica;
import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IMedicoEspecialidadRepository;
import com.piedrazul.personas.domain.repository.IMedicoRepository;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import com.piedrazul.personas.infrastructure.messaging.publisher.MedicoEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AsignarEspecialidadesMedicoService {

    private final IMedicoRepository medicoRepository;
    private final IMedicoEspecialidadRepository medicoEspecialidadRepository;
    private final IPersonaRepository personaRepository;
    private final MedicoEventPublisher medicoEventPublisher;

    public Medico ejecutar(Long personaId, Set<EspecialidadMedica> especialidades) {
        Medico medico = medicoRepository.buscarPorPersonaId(personaId)
                .orElseThrow(() -> new MedicoNoEncontradoException(
                        "No existe un médico para la personaId: " + personaId
                ));

        Persona persona = personaRepository.buscarPorId(personaId)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                        "Persona no encontrada con ID: " + personaId
                ));

        medico.asignarEspecialidades(especialidades);
        medicoEspecialidadRepository.reemplazarEspecialidades(personaId, medico.getEspecialidades());

        List<String> especialidadesEvento = medico.getEspecialidades().stream()
                .map(Enum::name)
                .toList();

        String nombreCompleto = persona.getPrimerNombre() + " " + persona.getPrimerApellido();
        medicoEventPublisher.publicarMedicoActualizado(
                personaId,
                nombreCompleto,
                persona.getCorreo(),
                especialidadesEvento,
                medico.getEstado().name()
        );

        log.info("Especialidades actualizadas y evento MedicoActualizado publicado para médico: {}", personaId);

        return medico;
    }
}
