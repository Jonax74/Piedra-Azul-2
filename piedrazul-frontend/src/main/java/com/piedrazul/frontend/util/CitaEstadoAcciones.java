package com.piedrazul.frontend.util;

import com.piedrazul.frontend.dto.response.CitaResponse;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class CitaEstadoAcciones {

    public enum Accion {
        CANCELAR,
        REAGENDAR,
        MARCAR_ATENDIDA,
        MARCAR_NO_ASISTIDA
    }

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_DATE_TIME;

    private CitaEstadoAcciones() {
    }

    public static boolean puedeEjecutar(Accion accion, CitaResponse cita) {
        if (cita == null || cita.getEstado() == null) {
            return false;
        }
        String estado = normalizar(cita.getEstado());
        LocalDateTime fecha = parseFecha(cita.getFechaHora());

        boolean fechaPasada = fecha != null && !fecha.isAfter(LocalDateTime.now());

        return switch (accion) {
            case CANCELAR -> esEstado(estado, "Programada", "Reagendada");
            case REAGENDAR -> esEstado(estado, "Programada", "Atendida");
            case MARCAR_ATENDIDA -> esEstado(estado, "Programada", "Reagendada") && fechaPasada;
            case MARCAR_NO_ASISTIDA -> esEstado(estado, "Programada", "Reagendada") && fechaPasada;
        };
    }

    public static String mensajeNoPermitido(Accion accion, CitaResponse cita) {
        String estado = cita != null && cita.getEstado() != null
                ? cita.getEstado()
                : "desconocido";

        return switch (accion) {
            case CANCELAR -> "Solo puede cancelar citas en estado Programada o Reagendada. "
                    + "Estado actual: " + estado + ".";
            case REAGENDAR -> "Solo puede reagendar citas en estado Programada o Atendida. "
                    + "Estado actual: " + estado + ".";
            case MARCAR_ATENDIDA -> mensajeCitaFutura(cita, estado,
                    "marcar como atendida");
            case MARCAR_NO_ASISTIDA -> mensajeCitaFutura(cita, estado,
                    "marcar como no asistida");
        };
    }

    public static void configurarColumnaEstado(TableColumn<CitaResponse, String> columna) {
        columna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "cita-estado-programada",
                        "cita-estado-reagendada",
                        "cita-estado-atendida",
                        "cita-estado-cancelada",
                        "cita-estado-no-asistida",
                        "cita-estado-default"
                );
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item);
                getStyleClass().add(estiloEstado(item));
            }
        });
    }

    public static String estiloEstado(String estado) {
        if (estado == null) {
            return "cita-estado-default";
        }
        return switch (normalizar(estado)) {
            case "Programada" -> "cita-estado-programada";
            case "Reagendada" -> "cita-estado-reagendada";
            case "Atendida" -> "cita-estado-atendida";
            case "Cancelada" -> "cita-estado-cancelada";
            case "No Asistida" -> "cita-estado-no-asistida";
            default -> "cita-estado-default";
        };
    }

    public static String hintAcciones(CitaResponse cita) {
        if (cita == null) {
            return "Seleccione una cita para ver las acciones disponibles.";
        }
        String estado = cita.getEstado() != null ? cita.getEstado() : "Sin estado";
        if ("Cancelada".equalsIgnoreCase(estado) || "No Asistida".equalsIgnoreCase(estado)) {
            return "La cita está en estado final (" + estado + ") y no admite más cambios.";
        }
        if ("Atendida".equalsIgnoreCase(estado)) {
            return "Cita atendida. Puede programar una nueva cita de seguimiento con Reagendar.";
        }
        return "Estado actual: " + estado + ". Elija una acción permitida.";
    }

    private static String mensajeCitaFutura(CitaResponse cita, String estado, String accion) {
        LocalDateTime fecha = parseFecha(cita != null ? cita.getFechaHora() : null);
        if (fecha != null && fecha.isAfter(LocalDateTime.now())) {
            return "Solo puede " + accion + " cuando la fecha y hora de la cita ya hayan pasado.";
        }
        return "Solo puede " + accion + " citas en estado Programada o Reagendada. "
                + "Estado actual: " + estado + ".";
    }

    private static boolean esEstado(String estado, String... valores) {
        if (estado == null) {
            return false;
        }
        for (String valor : valores) {
            if (valor.equalsIgnoreCase(estado)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizar(String estado) {
        String valor = estado.trim();
        if ("Confirmada".equalsIgnoreCase(valor)) {
            return "Programada";
        }
        return valor;
    }

    public static LocalDateTime parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(valor, ISO_LOCAL);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(valor, ISO_OFFSET);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
