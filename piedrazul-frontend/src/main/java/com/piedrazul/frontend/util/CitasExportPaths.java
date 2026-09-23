package com.piedrazul.frontend.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CitasExportPaths {

    private static final DateTimeFormatter NOMBRE_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private CitasExportPaths() {
    }

    public static Path carpetaDescargas() throws IOException {
        Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloads);
        return downloads;
    }

    public static Path rutaExcel(String prefijoArchivo) throws IOException {
        return carpetaDescargas().resolve(nombreArchivo(prefijoArchivo, ".xlsx"));
    }

    public static Path rutaCsv(String prefijoArchivo) throws IOException {
        return carpetaDescargas().resolve(nombreArchivo(prefijoArchivo, ".csv"));
    }

    public static String nombreArchivo(String prefijoArchivo, String extension) {
        return prefijoArchivo + "_" + LocalDateTime.now().format(NOMBRE_ARCHIVO) + extension;
    }
}
