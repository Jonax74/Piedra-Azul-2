package com.piedrazul.citas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de AuditMetadata")
class AuditMetadataTest {

    @Test
    @DisplayName("Debería crear metadata con fecha actual y createdBy SYSTEM")
    void testCrear() {
        AuditMetadata audit = AuditMetadata.crear();

        assertNotNull(audit);
        assertNotNull(audit.getCreatedAt());
        assertNotNull(audit.getUpdatedAt());
        assertEquals("SYSTEM", audit.getCreatedBy());
        assertEquals(audit.getCreatedAt(), audit.getUpdatedAt());
    }

    @Test
    @DisplayName("Debería crear metadata con usuario específico")
    void testCrearConUsuario() {
        AuditMetadata audit = AuditMetadata.crearConUsuario("juanperez");

        assertNotNull(audit);
        assertEquals("juanperez", audit.getCreatedBy());
    }

    @Test
    @DisplayName("Debería reconstruir metadata desde valores existentes")
    void testReconstruir() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 2, 10, 0, 0);

        AuditMetadata audit = AuditMetadata.reconstruir(createdAt, updatedAt, "admin");

        assertEquals(createdAt, audit.getCreatedAt());
        assertEquals(updatedAt, audit.getUpdatedAt());
        assertEquals("admin", audit.getCreatedBy());
    }

    @Test
    @DisplayName("Debería actualizar la fecha de modificación")
    void testActualizar() {
        AuditMetadata audit = AuditMetadata.crear();
        LocalDateTime updatedAtOriginal = audit.getUpdatedAt();

        // Esperar un milisegundo para asegurar diferencia
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        audit.actualizar();

        assertNotEquals(updatedAtOriginal, audit.getUpdatedAt());
        assertNotNull(audit.getUpdatedAt());
    }
}