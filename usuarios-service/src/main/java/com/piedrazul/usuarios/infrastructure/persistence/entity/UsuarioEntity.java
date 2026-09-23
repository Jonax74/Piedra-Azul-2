package com.piedrazul.usuarios.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Tabla 'usuario' tras la migracion a Keycloak.
 *
 * Esquema esperado (Hibernate ddl-auto=update lo creara si la tabla no existe;
 * si vienes del modelo viejo, conviene DROP DATABASE + recreate porque
 * Hibernate NO borra columnas obsoletas como password/estado/intentos_fallidos
 * ni las tablas rol/usuario_rol asociadas):
 *
 *   usuario(
 *     usu_id              UUID PRIMARY KEY,           -- = atributo usuario_id en Keycloak
 *     keycloak_user_id    UUID NOT NULL UNIQUE,       -- = sub del JWT
 *     username            VARCHAR(50) NOT NULL UNIQUE,
 *     per_id              BIGINT NOT NULL UNIQUE,     -- FK logica a personas-service
 *     fecha_creacion      TIMESTAMP NOT NULL,
 *     fecha_actualizacion TIMESTAMP NOT NULL
 *   )
 *
 * Nota: el id NO es auto-generado por la BD. Lo generamos en el servicio
 * (RegistrarUsuarioService) antes de crear el usuario en Keycloak, para que el
 * mismo UUID quede como claim 'usuario_id' del JWT.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "usu_id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "keycloak_user_id", nullable = false, unique = true, updatable = false)
    private UUID keycloakUserId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "per_id", nullable = false, unique = true)
    private Long personaId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = now;
        }
        this.fechaActualizacion = now;
    }

    @PreUpdate
    void onUpdate() {
        this.fechaActualizacion = Instant.now();
    }
}
