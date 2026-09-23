package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.valueobjects.*;

public record DatosCreacionCita(

        PacienteId pacienteId,
        MedicoId medicoId,
        UsuarioId creadoPor,

        PacienteSnapshot paciente,
        MedicoSnapshot medico,
        DisponibilidadSnapshot disponibilidad

) {}