package com.piedrazul.personas.interfaces.rest.mapper;

import com.piedrazul.personas.domain.model.Medico;
import com.piedrazul.personas.domain.model.Persona;
import com.piedrazul.personas.domain.repository.IPersonaRepository;
import com.piedrazul.personas.interfaces.rest.dto.response.MedicoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MedicoRestMapper {

    private final IPersonaRepository personaRepository;

    public MedicoResponse toResponse(Medico medico) {

        Persona persona = personaRepository.buscarPorId(medico.getPersonaId())
                .orElse(null);

        return MedicoResponse.builder()
                .personaId(medico.getPersonaId())

                .primerNombre(
                        persona != null
                                ? persona.getPrimerNombre()
                                : ""
                )

                .primerApellido(
                        persona != null
                                ? persona.getPrimerApellido()
                                : ""
                )

                .tipoProfesional(medico.getTipoProfesional())
                .estado(medico.getEstado())
                .especialidades(medico.getEspecialidades())
                .build();
    }
}