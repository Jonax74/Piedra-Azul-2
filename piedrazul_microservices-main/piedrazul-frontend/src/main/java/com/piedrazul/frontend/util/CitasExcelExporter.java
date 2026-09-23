package com.piedrazul.frontend.util;

import com.piedrazul.frontend.dto.response.CitaResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class CitasExcelExporter {

    private static final DateTimeFormatter FECHA_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CitasExcelExporter() {
    }

    public static Path exportar(List<CitaResponse> citas, boolean incluirMedico, String prefijoArchivo)
            throws IOException {
        return exportar(citas, incluirMedico, CitasExportPaths.rutaExcel(prefijoArchivo));
    }

    public static Path exportar(List<CitaResponse> citas, boolean incluirMedico, Path destino)
            throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Informe de citas");
            CellStyle headerStyle = crearEstiloEncabezado(workbook);

            int colIndex = 0;
            Row header = sheet.createRow(0);
            crearCelda(header, colIndex++, "ID", headerStyle);
            crearCelda(header, colIndex++, "Fecha", headerStyle);
            crearCelda(header, colIndex++, "Paciente", headerStyle);
            if (incluirMedico) {
                crearCelda(header, colIndex++, "Médico", headerStyle);
            }
            crearCelda(header, colIndex++, "Especialidad", headerStyle);
            crearCelda(header, colIndex++, "Estado", headerStyle);
            crearCelda(header, colIndex, "Motivo", headerStyle);

            int rowIndex = 1;
            for (CitaResponse cita : citas) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(valor(cita.getId()));
                row.createCell(col++).setCellValue(formatearFecha(cita));
                row.createCell(col++).setCellValue(valor(
                        cita.getPacienteNombre() != null
                                ? cita.getPacienteNombre()
                                : String.valueOf(cita.getPacienteId())
                ));
                if (incluirMedico) {
                    row.createCell(col++).setCellValue(valor(
                            cita.getMedicoNombre() != null
                                    ? cita.getMedicoNombre()
                                    : String.valueOf(cita.getMedicoId())
                    ));
                }
                row.createCell(col++).setCellValue(EspecialidadLabels.etiqueta(cita.getEspecialidad()));
                row.createCell(col++).setCellValue(valor(cita.getEstado()));
                row.createCell(col).setCellValue(CitaMotivoUtil.resolverMotivo(cita));
            }

            for (int i = 0; i <= (incluirMedico ? 6 : 5); i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream out = Files.newOutputStream(destino)) {
                workbook.write(out);
            }
        }

        return destino;
    }

    private static CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void crearCelda(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
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
