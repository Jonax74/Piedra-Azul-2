package com.piedrazul.citas.domain.model;

import java.time.LocalDateTime;

public class AuditMetadata {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    // Constructor para nuevas entidades
    private AuditMetadata(LocalDateTime createdAt, String createdBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = createdAt;
    }

    // Constructor para reconstruir desde BD
    private AuditMetadata(LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }

    public static AuditMetadata crear() {
        return new AuditMetadata(LocalDateTime.now(), "SYSTEM");
    }

    public static AuditMetadata crearConUsuario(String usuario) {
        return new AuditMetadata(LocalDateTime.now(), usuario);
    }

    // Método para reconstruir desde base de datos
    public static AuditMetadata reconstruir(LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy) {
        return new AuditMetadata(createdAt, updatedAt, createdBy);
    }

    public void actualizar() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
}