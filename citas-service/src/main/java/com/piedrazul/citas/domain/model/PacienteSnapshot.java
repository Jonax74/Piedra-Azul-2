package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.valueobjects.PacienteId;
import java.time.LocalDateTime;

public class PacienteSnapshot {
    private final PacienteId id;
    private final String nombreCompleto;
    private final String email;
    private final String telefono;
    private final boolean activo;
    private final LocalDateTime actualizadoEn;

    public PacienteSnapshot(PacienteId id, String nombreCompleto,
                            String email, String telefono, boolean activo) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.telefono = telefono;
        this.activo = activo;
        this.actualizadoEn = LocalDateTime.now();
    }

    public boolean existe() {
        return activo;
    }

    // Getters
    public PacienteId getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}