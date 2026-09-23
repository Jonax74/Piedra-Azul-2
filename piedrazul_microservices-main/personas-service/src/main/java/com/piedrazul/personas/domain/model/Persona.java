package com.piedrazul.personas.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public class Persona {

    private Long id;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private Genero genero;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String dni;
    private String correo;

    public Persona() {
    }

    public Persona(
            Long id,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            Genero genero,
            LocalDate fechaNacimiento,
            String telefono,
            String dni,
            String correo
    ) {
        validarPrimerNombre(primerNombre);
        validarPrimerApellido(primerApellido);
        validarGenero(genero);
        validarFechaNacimiento(fechaNacimiento);
        validarTelefono(telefono);
        validarDni(dni);

        this.id = id;
        this.primerNombre = primerNombre.trim();
        this.segundoNombre = limpiarOpcional(segundoNombre);
        this.primerApellido = primerApellido.trim();
        this.segundoApellido = limpiarOpcional(segundoApellido);
        this.genero = genero;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono.trim();
        this.dni = dni.trim();
        this.correo = limpiarOpcional(correo);
    }

    public static Persona crear(
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            Genero genero,
            LocalDate fechaNacimiento,
            String telefono,
            String dni,
            String correo
    ) {
        return new Persona(
                null,
                primerNombre,
                segundoNombre,
                primerApellido,
                segundoApellido,
                genero,
                fechaNacimiento,
                telefono,
                dni,
                correo
        );
    }

    public void actualizarDatosAdministrables(
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            Genero genero,
            LocalDate fechaNacimiento,
            String telefono
    ) {
        validarPrimerNombre(primerNombre);
        validarPrimerApellido(primerApellido);
        validarGenero(genero);
        validarFechaNacimiento(fechaNacimiento);
        validarTelefono(telefono);

        this.primerNombre = primerNombre.trim();
        this.segundoNombre = limpiarOpcional(segundoNombre);
        this.primerApellido = primerApellido.trim();
        this.segundoApellido = limpiarOpcional(segundoApellido);
        this.genero = genero;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono.trim();
    }

    public void actualizarDatosBasicos(
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            Genero genero,
            LocalDate fechaNacimiento,
            String telefono,
            String correo
    ) {
        validarPrimerNombre(primerNombre);
        validarPrimerApellido(primerApellido);
        validarGenero(genero);
        validarFechaNacimiento(fechaNacimiento);
        validarTelefono(telefono);

        this.primerNombre = primerNombre.trim();
        this.segundoNombre = limpiarOpcional(segundoNombre);
        this.primerApellido = primerApellido.trim();
        this.segundoApellido = limpiarOpcional(segundoApellido);
        this.genero = genero;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono.trim();
        this.correo = limpiarOpcional(correo);
    }

    private void validarPrimerNombre(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El primer nombre es obligatorio");
        }
    }

    private void validarPrimerApellido(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El primer apellido es obligatorio");
        }
    }

    private void validarGenero(Genero valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El género es obligatorio");
        }
    }

    private void validarFechaNacimiento(LocalDate valor) {
        if (valor != null && valor.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
    }

    private void validarTelefono(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono es obligatorio");
        }
    }

    private void validarDni(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI es obligatorio");
        }
    }

    private String limpiarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public Genero getGenero() {
        return genero;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDni() {
        return dni;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        sb.append(primerNombre);

        if (segundoNombre != null) {
            sb.append(" ").append(segundoNombre);
        }

        sb.append(" ").append(primerApellido);

        if (segundoApellido != null) {
            sb.append(" ").append(segundoApellido);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Persona persona)) return false;
        return Objects.equals(id, persona.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}