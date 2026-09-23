package com.piedrazul.frontend.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.nio.file.Path;
import java.util.Optional;

public final class CitasExportMessages {

    public static final String EXITO =
            "Exportación exitosa, revise sus descargas";
    public static final String SIN_DATOS =
            "No hay citas registradas para las fechas establecidas";
    public static final String ERROR =
            "Ocurrió un error en la exportación";

    public static final String TIPO_EXCEL = "Excel (.xlsx)";
    public static final String TIPO_CSV = "CSV (.csv)";

    private CitasExportMessages() {
    }

    public static boolean confirmarExportacion(String tipoArchivo, Path destino) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar exportación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
                "¿Desea descargar el archivo?\n\n"
                        + "Nombre: " + destino.getFileName() + "\n"
                        + "Tipo: " + tipoArchivo + "\n"
                        + "Ubicación: " + destino.getParent()
        );

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }
}
