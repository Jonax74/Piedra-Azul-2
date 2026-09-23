package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.valueobjects.PacienteId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de PacienteSnapshot")
class PacienteSnapshotTest {

    @Test
    @DisplayName("Debería crear un snapshot de paciente correctamente")
    void testCrear() {
        PacienteId pacienteId = PacienteId.of(1L);
        PacienteSnapshot snapshot = new PacienteSnapshot(
                pacienteId,
                "Juan Pérez",
                "juan@email.com",
                "3001234567",
                true
        );

        assertNotNull(snapshot);
        assertEquals(pacienteId, snapshot.getId());
        assertEquals("Juan Pérez", snapshot.getNombreCompleto());
        assertEquals("juan@email.com", snapshot.getEmail());
        assertEquals("3001234567", snapshot.getTelefono());
        assertTrue(snapshot.isActivo());
        assertNotNull(snapshot.getActualizadoEn());
    }

    @Test
    @DisplayName("existe() debería devolver true si el paciente está activo")
    void testExiste() {
        PacienteId pacienteId = PacienteId.of(1L);
        PacienteSnapshot activo = new PacienteSnapshot(pacienteId, "Juan", "j@j.com", "123", true);
        PacienteSnapshot inactivo = new PacienteSnapshot(pacienteId, "Pedro", "p@p.com", "456", false);

        assertTrue(activo.existe());
        assertFalse(inactivo.existe());
    }
}