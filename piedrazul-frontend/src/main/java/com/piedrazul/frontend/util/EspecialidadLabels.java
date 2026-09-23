package com.piedrazul.frontend.util;

public final class EspecialidadLabels {

    private EspecialidadLabels() {
    }

    public static String etiqueta(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "Medicina General";
        }
        return switch (codigo.trim().toUpperCase()) {
            case "TERAPEUTA_NEURAL" -> "Terapeuta Neural";
            case "QUIROPRACTICO" -> "Quiropráctico";
            case "FISIOTERAPEUTA" -> "Fisioterapia";
            case "GENERAL" -> "Medicina General";
            default -> codigo;
        };
    }
}
