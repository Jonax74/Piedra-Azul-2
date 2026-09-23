package com.piedrazul.usuarios.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearUsuarioRequest {

    @NotNull(message = "personaId es obligatorio")
    private Long personaId;

    @NotBlank(message = "username es obligatorio")
    @Size(min = 3, max = 50, message = "username debe tener entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "password es obligatorio")
    @Size(min = 6, message = "password debe tener al menos 6 caracteres")
    private String password;

    private String email;

    public void setEmail(String email) {
        this.email = (email == null || email.isBlank()) ? null : email.trim();
    }

    private String firstName;

    private String lastName;

    @NotEmpty(message = "Debe asignarse al menos un rol")
    private List<String> roles;
}
