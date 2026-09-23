package com.piedrazul.frontend.util;

import com.piedrazul.frontend.dto.response.CitaResponse;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class CitasCsvExporter {

    private static final DateTimeFormatter FECHA_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CitasCsvExporter() {
    }

    public static Path exportar(List<CitaResponse> citas, boolean incluirMedico, String prefijoArchivo)
            throws IOException {
        return exportar(citas, incluirMedico, CitasExportPaths.rutaCsv(prefijoArchivo));
    }

    public static Path exportar(List<CitaResponse> citas, boolean incluirMedico, Path destino)
            throws IOException {

        try (Writer writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');

            List<String> encabezados = new ArrayList<>();
            encabezados.add("ID");
            encabezados.add("Fecha");
            encabezados.add("Paciente");
            if (incluirMedico) {
                encabezados.add("Médico");
            }
            encabezados.add("Especialidad");
            encabezados.add("Estado");
            encabezados.add("Motivo");
            writer.write(String.join(",", encabezados.stream().map(CitasCsvExporter::escapar).toList()));
            writer.write(System.lineSeparator());

            for (CitaResponse cita : citas) {
                List<String> columnas = new ArrayList<>();
                columnas.add(escapar(valor(cita.getId())));
                columnas.add(escapar(formatearFecha(cita)));
                columnas.add(escapar(valor(
                        cita.getPacienteNombre() != null
                                ? cita.getPacienteNombre()
                                : String.valueOf(cita.getPacienteId())
                )));
                if (incluirMedico) {
                    columnas.add(escapar(valor(
                            cita.getMedicoNombre() != null
                                    ? cita.getMedicoNombre()
                                    : String.valueOf(cita.getMedicoId())
                    )));
                }
                columnas.add(escapar(EspecialidadLabels.etiqueta(cita.getEspecialidad())));
                columnas.add(escapar(valor(cita.getEstado())));
                columnas.add(escapar(CitaMotivoUtil.resolverMotivo(cita)));
                writer.write(String.join(",", columnas));
                writer.write(System.lineSeparator());
            }
        }

        return destino;
    }

    private static String escapar(String value) {
        if (value == null) {
            return "\"\"";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String formatearFecha(CitaResponse cita) {
        if (cita.getFechaHora() == null || cita.getFechaHora().isBlank()) {
            return "-";
        }
        try {
            LocalDateTime fecha = LocalDateTime.parse(cita.getFechaHora(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return fecha.format(FECHA_DISPLAY);
        } catch (DateTimeParseException e) {
            try {
                LocalDateTime fecha = LocalDateTime.parse(cita.getFechaHora(), DateTimeFormatter.ISO_DATE_TIME);
                return fecha.format(FECHA_DISPLAY);
            } catch (DateTimeParseException ex) {
                return cita.getFechaHora();
            }
        }
    }

    private static String valor(String text) {
        return text != null ? text : "-";
    }
}
